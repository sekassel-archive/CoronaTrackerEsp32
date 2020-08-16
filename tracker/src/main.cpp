#include <Arduino.h>
#include <Ticker.h>
#include <SparkFunLSM9DS1.h>
#include <pcf8563.h>
// #include <sqlite3.h>

#include "coronatracker-spiffs.h"
#include "coronatracker-display.h"
#include "coronatracker-ble.h"
#include "coronatracker-wifi.h"

#define LED_PIN 4
#define TP_PWR_PIN 25
#define TP_PIN_PIN 33

#define ACTION_NOTHING 0
#define ACTION_ADVERTISE 1
#define ACTION_SCAN 2
#define ACTION_WIFI_CONFIG 3
#define ACTION_INFECTION_REQUEST 4

#define SLEEP_INTERVAL 3000000 //In microseconds --> 1000 milliseconds

#define BOOTS_UNTIL_SCAN 500
#define BOOTS_UNTIL_INFECTION_REQUEST 30000

#define SCAN_TIME 2

//Saved during deep sleep mode
RTC_DATA_ATTR int nextAction = 0;
RTC_DATA_ATTR int bootCount = 0;
RTC_DATA_ATTR bool wifiInitialized = true;
RTC_DATA_ATTR bool firstBoot = true;

const int ENCOUNTERS_NEEDED = 10;

//Wifi Variables
const static int BUTTON_PRESS_DURATION_MILLISECONDS = 4000; //4 Seconds

int buttonState = 0;
int lastButtonState = 0;
int startPressed = 0;

Ticker buttonTicker;

LSM9DS1 imu;
PCF8563_Class rtc;

//Time Variables
const char *ntpServer = "pool.ntp.org";
const long gmtOffset_sec = 3600;
const int daylightOffset_sec = 3600;

void blinkLED()
{
    // int state = digitalRead(LED_PIN);
    // digitalWrite(LED_PIN, !state);
    int state = digitalRead(BUILTIN_LED);
    digitalWrite(BUILTIN_LED, !state);
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
    // digitalWrite(LED_PIN, HIGH);
    digitalWrite(LED_BUILTIN, HIGH);
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
    else if (bootCount % BOOTS_UNTIL_SCAN == 0)
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

    imu.sleepGyro(true);

    // float start1 = micros();
    // float end1;

    // setCpuFrequencyMhz(80);


    // end1 = micros();
    // float result1 = end1 - start1;
    // result1 /= 1000; //convert to milliseconds
    // Serial.printf("Time(milliseconds): %g\n", result1);

    float start = micros();
    float end;

    // Serial.printf("Is BLE initialized?: %s\n", BLEDevice::getInitialized() ? "true" : "false");

    //Setting up pinModes
    Serial.println("Setting up pinModes");
    // pinMode(LED_PIN, OUTPUT);
    // pinMode(TP_PIN_PIN, INPUT); // Button input
    // pinMode(TP_PWR_PIN, PULLUP);
    // digitalWrite(TP_PWR_PIN, HIGH);

    pinMode(LED_BUILTIN, OUTPUT);
    pinMode(KEY_BUILTIN, INPUT); // Button input

    //Wifi not initialized
    if (!wifiInitialized)
    {
        Serial.println("Awaiting Button Press for Wifi-Configuration");
        tftInit();
        // digitalWrite(LED_PIN, HIGH);
        digitalWrite(LED_BUILTIN, HIGH);
        setNextAction(ACTION_WIFI_CONFIG);
        showStartWifiMessageOnDisplay();
    }
    else
    {
        // digitalWrite(LED_PIN, LOW);
        digitalWrite(LED_BUILTIN, LOW);
        if (firstBoot)
        {
            firstBoot = false;

            //Getting Time
            if (!connectToStoredWifi())
            {
                Serial.println("Could not connect to Wifi!");
            }

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

            Serial.println("Initializing SPIFFS");
            if (!initSPIFFS(true, true))
            {
                restartAfterErrorWithDelay("SPIFFS initialize failed");
            }

            //Start a request upon first boot
            setNextAction(ACTION_INFECTION_REQUEST);
        }

        if (nextAction != ACTION_ADVERTISE)
        {
            Serial.println("Initializing SPIFFS");
            if (!initSPIFFS(false, false))
            {
                restartAfterErrorWithDelay("SPIFFS initialize failed");
            }
        }

        if (nextAction == ACTION_ADVERTISE || nextAction == ACTION_SCAN)
        {
            Serial.println("Initializing BLE");
            if (!initBLE(nextAction == ACTION_SCAN, nextAction == ACTION_ADVERTISE))
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
        scanForCovidDevices((uint32_t)SCAN_TIME);

        //TODO Maybe move recentEncounters to Main

        //TODO Read from File instead

        // File file = SPIFFS.open(RECENTENCOUNTERS_PATH, FILE_READ);

        // int index = 0;
        // int count = 0;
        // std::string id = "";
        // std::string firstId = "";
        // bool idFlag = true;

        // while (file.available())
        // {

        //     char c = file.read();
        //     if (c == ',') {
        //         if (id == firstId) {
        //             count++;
        //         }
        //         else if (firstId == "") {
        //             firstId = id;
        //             count++;
        //         }
        //         idFlag = false;
        //         id = "";
        //     }
        //     else if (idFlag)
        //     {
        //         id += c;
        //     }
        //     else if (c == ';')
        //     {
        //         idFlag = true;
        //     }
        // }
        // firstId = "";
        // count = 0;

        auto recentEncounters = getRecentEncounters();
        for (auto it = recentEncounters->begin(), end = recentEncounters->end(); it != end; it = recentEncounters->upper_bound(it->first))
        {
            if (recentEncounters->count(it->first) >= ENCOUNTERS_NEEDED && !fileContainsString(it->first, ENCOUNTERS_PATH))
            {
                std::string stringToAppend = it->first + ";";
                if (writeIDtoFile(stringToAppend, ENCOUNTERS_PATH))
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

        //TODO Delete Entries from file

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
    else if (nextAction == ACTION_ADVERTISE)
    {
        delay(10000);
    }
    else if (nextAction == ACTION_WIFI_CONFIG)
    {
        while (true)
        {
            // buttonState = digitalRead(TP_PIN_PIN); // read the button input
            buttonState = digitalRead(KEY_BUILTIN); // read the button input
            //Button was pressed
            if (buttonState == /*HIGH*/ LOW)
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
                        // digitalWrite(LED_PIN, LOW);
                        digitalWrite(LED_BUILTIN, LOW);
                    }
                    else
                    {
                        Serial.println("Could not connect to Wifi");
                        // digitalWrite(LED_PIN, HIGH);
                        digitalWrite(LED_BUILTIN, HIGH);
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
            delay(10000);
        }
        showRequestDelayOnDisplay();
    }

    // Serial.printf("Is BLE initialized?: %s\n", BLEDevice::getInitialized() ? "true" : "false");

    // Serial.println("deinitBLE()");

    end = micros();
    float result = end - start;
    result /= 1000; //convert to milliseconds
    Serial.printf("Time(milliseconds): %g\n", result);

    goIntoDeepSleep();
}

void loop()
{
    //Never called
}