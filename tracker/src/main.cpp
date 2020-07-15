#include <Arduino.h>
#include <Ticker.h>

#include "coronatracker-display.h"
#include "coronatracker-ble.h"
#include "coronatracker-wifi.h"
#include "coronatracker-spiffs.h"

#define LED_PIN 4
#define TP_PWR_PIN 25
#define TP_PIN_PIN 33

bool doScan = false;
const static int SCAN_DELAY_MILLISECONDS = 10000; //10 Seconds

const int ENCOUNTERS_NEEDED = 10;

//Wifi Variables
const static int BUTTON_PRESS_DURATION_MILLISECONDS = 4000; //4 Seconds
const static int REQUEST_DELAY_SECONDS = 60;                //60 Seconds
//const static int REQUEST_DELAY_SECONDS = 3600; // 1hour -> Final Time

int buttonState = 0;
int lastButtonState = 0;
int startPressed = 0;

Ticker wifiTicker;
Ticker buttonTicker;
bool waitForConfig = false;
bool sendHTTPRequest = false;

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
    showLocalTimeOnDisplay(timeinfo);
    return true;
}

void setHTTPFlag()
{
    doScan = false;
    sendHTTPRequest = true;
}

void restartAfterErrorWithDelay(String errorMessage, uint32_t delayMS = 10000)
{
    digitalWrite(LED_PIN, HIGH);
    Serial.println(errorMessage);
    delay(delayMS);
    ESP.restart();
}

void setup()
{
    //Deletes stored Wifi Credentials if uncommented
    //WiFiManager manager;
    //manager.resetSettings();

    //Setting up Serial
    Serial.begin(115200);
    Serial.println("Serial initialized");

    // initialize display
    tftInit(REQUEST_DELAY_SECONDS);

    //Setting up pinModes
    Serial.println("Setting up pinModes");
    pinMode(LED_PIN, OUTPUT);
    pinMode(TP_PIN_PIN, INPUT); // Button input
    pinMode(TP_PWR_PIN, PULLUP);
    digitalWrite(TP_PWR_PIN, HIGH);

    //Connection Failed
    if (!connectToStoredWifi())
    {
        Serial.println("Awaiting Button Press for Wifi-Configuration");
        digitalWrite(LED_PIN, HIGH);
        waitForConfig = true;
        showStartWifiMessageOnDisplay();
    }
    else
    {
        digitalWrite(LED_PIN, LOW);

        //Getting Time
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
        if (!initSPIFFS())
        {
            restartAfterErrorWithDelay("SPIFFS initialize failed");
        }

        Serial.println("Initializing BLE");
        if (!initBLE())
        {
            restartAfterErrorWithDelay("BLE initialize failed");
        }

        //Start a request upon startup
        sendHTTPRequest = true;
    }
}

void loop()
{
    if (doScan)
    {
        Serial.println("Starting Scan...");
        scanForCovidDevices((uint32_t)1);

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
        delay(10000); //Scan Every 10 Seconds
    }
    else if (waitForConfig)
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
                    digitalWrite(LED_PIN, LOW);
                }
                else
                {
                    Serial.println("Could not connect to Wifi");
                    digitalWrite(LED_PIN, HIGH);
                    //Delay so feedback can be seen on LED
                    delay(5000);
                }
                ESP.restart();
            }
        }

        lastButtonState = buttonState;
        delay(500);
    }
    else if (sendHTTPRequest)
    {
        wifiTicker.detach();
        deinitBLE();
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
            delay(10000);
        }
        showRequestDelayOnDisplay();
        sendHTTPRequest = false;
        doScan = true;
        wifiTicker.attach(REQUEST_DELAY_SECONDS, setHTTPFlag);
        initBLE();
        delay(500);
    }
}