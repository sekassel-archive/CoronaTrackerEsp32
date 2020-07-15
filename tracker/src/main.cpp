#include <Arduino.h>
#include <Ticker.h>

#include "coronatracker-display.h"
#include "coronatracker-ble.h"
#include "coronatracker-wifi.h"
#include "coronatracker-spiffs.h"

#define LED_PIN 4
#define TP_PWR_PIN 25
#define TP_PIN_PIN 33

#define ACTION_NOTHING 0
#define ACTION_ADVERTISE 1
#define ACTION_SCAN 2
#define ACTION_WIFI_CONFIG 3
#define ACTION_INFECTION_REQUEST 4

#define SLEEP_INTERVAL 100000 //In microseconds --> 100 milliseconds

#define BOOTS_UNTIL_SCAN 500
#define BOOTS_UNTIL_INFECTION_REQUEST 30000

//Saved during deep sleep mode
RTC_DATA_ATTR int nextAction = 0;
RTC_DATA_ATTR int bootCount = 0;
RTC_DATA_ATTR bool wifiInitialized = false;
RTC_DATA_ATTR bool firstBoot = true;

const int ENCOUNTERS_NEEDED = 10;

//Wifi Variables
const static int BUTTON_PRESS_DURATION_MILLISECONDS = 4000; //4 Seconds

int buttonState = 0;
int lastButtonState = 0;
int startPressed = 0;

Ticker buttonTicker;

//Time Variables
const char *ntpServer = "pool.ntp.org";
const long gmtOffset_sec = 3600;
const int daylightOffset_sec = 3600;

void blinkLED()
{
    int state = digitalRead(LED_PIN);
    digitalWrite(LED_PIN, !state);
}

bool initializeTime()
{
    struct tm timeinfo;
    int start = millis();
    const int WAITTIME = 180000; //3 Minutes

    do
    {
        if ((millis() - start) >= WAITTIME)
        {
            return false;
        }
        configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
        delay(500);

    } while (!getLocalTime(&timeinfo));
    Serial.print("Local Time: ");
    Serial.println(&timeinfo, "%A, %B %d %Y %H:%M:%S");
    //showLocalTimeOnDisplay(timeinfo);
    return true;
}

void restartAfterErrorWithDelay(String errorMessage, uint32_t delayMS = 10000)
{
    digitalWrite(LED_PIN, HIGH);
    Serial.println(errorMessage);
    delay(delayMS);
    ESP.restart();
}

void setNextAction(int action)
{
    switch (action)
    {
    case ACTION_SCAN:
        Serial.println("Next: Scanning");
        nextAction = ACTION_SCAN;
        break;
    case ACTION_WIFI_CONFIG:
        Serial.println("Next: Wifi-Config");
        nextAction = ACTION_WIFI_CONFIG;
        break;
    case ACTION_INFECTION_REQUEST:
        Serial.println("Next: Infection-Request");
        nextAction = ACTION_INFECTION_REQUEST;
        break;
    case ACTION_ADVERTISE:
        Serial.println("Next: Advertising");
        nextAction = ACTION_ADVERTISE;
        break;
    default:
        nextAction = ACTION_NOTHING;
        break;
    }
}

void goIntoDeepSleep()
{
    bootCount++;

    //Every 500th boot equals roughly every Minute. Every 30000th boot equals (very) roughly 1 hour. This does not factor in constant times to sleep/wake up.
    if (bootCount % BOOTS_UNTIL_INFECTION_REQUEST == 0)
    {
        setNextAction(ACTION_INFECTION_REQUEST);
    }
    else if (bootCount % 5 == 0)
    {
        setNextAction(ACTION_SCAN);
    }
    else
    {
        setNextAction(ACTION_ADVERTISE);
    }

    esp_sleep_enable_timer_wakeup(SLEEP_INTERVAL);
    esp_deep_sleep_start();
}

void setup()
{
    //Deletes stored Wifi Credentials if uncommented
    //WiFiManager manager;
    //manager.resetSettings();

    //Setting up Serial
    Serial.begin(115200);
    Serial.println("Serial initialized");

    //Setting up pinModes
    Serial.println("Setting up pinModes");
    pinMode(LED_PIN, OUTPUT);
    pinMode(TP_PIN_PIN, INPUT); // Button input
    pinMode(TP_PWR_PIN, PULLUP);
    digitalWrite(TP_PWR_PIN, HIGH);

    //Wifi not initialized
    if (!wifiInitialized)
    {
        Serial.println("Awaiting Button Press for Wifi-Configuration");
        tftInit();
        digitalWrite(LED_PIN, HIGH);
        setNextAction(ACTION_WIFI_CONFIG);
        showStartWifiMessageOnDisplay();
    }
    else
    {
        if (firstBoot)
        {
            firstBoot = false;

            //Start a request upon first boot
            setNextAction(ACTION_INFECTION_REQUEST);
        }

        //Getting Time
        if (!connectToStoredWifi())
        {
            Serial.println("Could not connect to Wifi!");
        }
        digitalWrite(LED_PIN, LOW);

        //TODO Test if time needs to be reinitialized or not
        Serial.println("Initializing Time");
        if (!initializeTime())
        {
            restartAfterErrorWithDelay("Time initialize failed");
        }

        Serial.println("Disconnecting Wifi");
        if (!disconnectWifi())
        {
            Serial.println("Disconnect Failed");
        }

        //TODO Test if init needed after deep sleep
        if (nextAction != ACTION_ADVERTISE)
        {
            Serial.println("Initializing SPIFFS");
            if (!initSPIFFS())
            {
                restartAfterErrorWithDelay("SPIFFS initialize failed");
            }
        }

        if (nextAction == ACTION_ADVERTISE || nextAction == ACTION_SCAN)
        {
            Serial.println("Initializing BLE");
            if (!initBLE())
            {
                restartAfterErrorWithDelay("BLE initialize failed");
            }
        }

        if (nextAction == ACTION_INFECTION_REQUEST)
        {
            tftInit();
        }
    }

    if (nextAction == ACTION_SCAN)
    {
        Serial.println("Starting Scan...");
        scanForCovidDevices((uint32_t)1);

        //TODO Maybe move recentEncounters to Main
        auto recentEncounters = getRecentEncounters();
        for (auto it = recentEncounters->begin(), end = recentEncounters->end(); it != end; it = recentEncounters->upper_bound(it->first))
        {
            if (recentEncounters->count(it->first) >= ENCOUNTERS_NEEDED && !fileContainsString(it->first, ENCOUNTERS_PATH))
            {
                std::string stringToAppend = it->first + ";";
                if (writeIDtoFile(stringToAppend))
                {
                    Serial.printf("Successfully added %s to file.\n", it->first.c_str());
                }
                else
                {
                    Serial.printf("Could not print id to file: %s\n", it->first.c_str());
                }
            }
        }

        //We delete entries that are older than 15 minutes
        time_t fifteenMinutesAgo = time(NULL) - 930; // 15 Minutes and 30 Seconds
        for (auto it = recentEncounters->cbegin(), next_it = it; it != recentEncounters->cend(); it = next_it)
        {
            ++next_it;
            if (it->second < fifteenMinutesAgo)
            {
                recentEncounters->erase(it);
            }
        }

        for (auto it = recentEncounters->cbegin(); it != recentEncounters->cend(); ++it)
        {
            Serial.printf("%s -> %ld\n", it->first.c_str(), it->second);
        }
    }
    else if (nextAction == ACTION_WIFI_CONFIG)
    {
        while (true)
        {
            buttonState = digitalRead(TP_PIN_PIN); // read the button input
            //Button was pressed
            if (buttonState == HIGH)
            {
                //First press
                if (buttonState != lastButtonState)
                {
                    startPressed = millis();
                }
                else if ((millis() - startPressed) >= BUTTON_PRESS_DURATION_MILLISECONDS)
                {
                    Serial.println("Starting WifiManger-Config");
                    buttonTicker.attach_ms(500, blinkLED);

                    configureWifiMessageOnDisplay();
                    bool res = configureWifi();

                    buttonTicker.detach();
                    if (res)
                    {
                        Serial.println("We connected to Wifi...");
                        wifiInitialized = true;
                        digitalWrite(LED_PIN, LOW);
                    }
                    else
                    {
                        Serial.println("Could not connect to Wifi");
                        digitalWrite(LED_PIN, HIGH);
                        //Delay so feedback can be seen on LED
                        delay(5000);
                    }
                    disconnectWifi();
                    break;
                    //ESP.restart(); //Loop exit
                }
            }
            lastButtonState = buttonState;
            delay(500);
        }
    }
    else if (nextAction == ACTION_INFECTION_REQUEST)
    {
        auto result = requestInfections();
        if (!result.first)
        {
            Serial.println("Failed to request infections");
        }
        else
        {
            auto infectionVector = result.second;
            bool metInfected = false;

            for (long id : infectionVector)
            {
                if (fileContainsString(String(id).c_str()))
                {
                    Serial.printf("User has me someone infected: %ld\n", id);
                    metInfected = true;
                }
            }
            showIsInfectedOnDisplay(metInfected);
            //TODO Change Behaviour on Infection
            delay(60000);
        }
        showRequestDelayOnDisplay();
    }
    goIntoDeepSleep();
}

void loop()
{
    //Never called
}