#include <Arduino.h>
#include <Ticker.h>

#include "coronatracker-ble.h"
#include "coronatracker-wifi.h"
#include "coronatracker-spiffs.h"

//display Libraries
#include <SPI.h>
#include <TFT_eSPI.h>
#include <Wire.h>

#define LED_PIN 4
#define TP_PWR_PIN 25
#define TP_PIN_PIN 33

TFT_eSPI tft = TFT_eSPI();

bool doScan = false;
const static int SCAN_DELAY_MILLISECONDS = 10000; //10 Seconds

const int ENCOUNTERS_NEEDED = 10;

//Wifi Variables
const static int BUTTON_PRESS_DURATION_MILLISECONDS = 4000; //4 Seconds
const static int REQUEST_DELAY_SECONDS = 60;                //60 Seconds
//const static int REQUEST_DELAY_SECONDS = 3600; // 1hour -> Final Time

//Time Constants
const static int HOUR = 3600;
const static int MINUTE = 60;
const static int DAY = 86400;

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

/*  Before you can use the display on the esp32devOTA you have to change two lines.
*   Follow Step 3 of the following URL.
*   https://github.com/Xinyuan-LilyGO/LilyGo-T-Wristband/blob/master/examples/T-Wristband-LSM9DS1/README.MD
*   TODO 
*   tracker\.pio\libdeps\esp32devOTA\TFT_eSPI\User_Setup_Select.h 
*   have to be in the gitignore.
*/
void tftInit()
{
    tft.init();
    tft.setRotation(1);
    tft.fillScreen(TFT_BLACK);
    tft.setTextColor(TFT_WHITE);

    tft.setTextSize(1); //With size equals to 1, you can print 10 lines and about 27 characters per line
    tft.setCursor(0, 0);
}

void blinkLED()
{
    int state = digitalRead(LED_PIN);
    digitalWrite(LED_PIN, !state);
}

void showIsInfectedOnDisplay(bool metInfected)
{
    if (!metInfected)
    {
        tft.fillScreen(TFT_BLACK);
        tft.setTextSize(2);
        tft.setCursor(0, 1);
        tft.print("\nEverything\nis fine.");
        delay(10000);
    }
    else
    {
        tft.fillScreen(TFT_BLACK);
        tft.setTextSize(2);
        tft.setCursor(0, 1);
        tft.print("You've met\nsomeone who\nis infected.\nPlease go\nquarantine.");
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
                    break;
                }
            }

            lastButtonState = buttonState;
            delay(500);
        }
    }
}

void showLocalTimeOnDisplay(struct tm timeinfo)
{
    tft.fillScreen(TFT_BLACK);
    tft.setTextSize(2);
    tft.print(&timeinfo, "%A\n%B %d %Y\n%H:%M:%S"); //could look better when centered
    delay(10000);
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

void showStartWifiMessageOnDisplay()
{
    tft.fillScreen(TFT_BLACK);
    tft.setTextSize(2);
    tft.print("Press Button\nfor 4 Seconds\nto start \nWifi-\nConfiguration");
}

void showRequestDelayOnDisplay()
{
    struct tm timeinfo;
    Serial.printf("Time get: %d\n", getLocalTime(&timeinfo));
    tft.fillScreen(TFT_BLACK);
    tft.setTextSize(2);
    tft.setCursor(0, 0);
    tft.print("Next Wifi-\nConnection:\n");
    //convert REQUEST_DELAY_SECONDS into hour, minute format
    int currentHoursInSeconds = timeinfo.tm_hour * HOUR;
    int currentMinutesInSeconds = timeinfo.tm_min * MINUTE;
    int request_delay_seconds = REQUEST_DELAY_SECONDS;

    if (request_delay_seconds + currentHoursInSeconds + currentMinutesInSeconds + timeinfo.tm_sec >= DAY)
    {
        timeinfo.tm_wday += 1;
    }

    if ((request_delay_seconds % HOUR) + currentMinutesInSeconds + timeinfo.tm_sec >= HOUR)
    {
        timeinfo.tm_hour = (timeinfo.tm_hour + (request_delay_seconds / HOUR) + 1) % 24;
        request_delay_seconds = REQUEST_DELAY_SECONDS % HOUR;

        if ((request_delay_seconds % MINUTE) + timeinfo.tm_sec >= MINUTE)
        {
            timeinfo.tm_min = (timeinfo.tm_min + (request_delay_seconds / MINUTE) + 1) % 60;
            request_delay_seconds = REQUEST_DELAY_SECONDS % MINUTE;
            timeinfo.tm_sec = (timeinfo.tm_sec + request_delay_seconds) % 60;
        }
        else
        {
            timeinfo.tm_min = (timeinfo.tm_min + (request_delay_seconds / MINUTE)) % 60;
            request_delay_seconds = REQUEST_DELAY_SECONDS % MINUTE;
            timeinfo.tm_sec = (timeinfo.tm_sec + request_delay_seconds) % 60;
        }
    }
    else
    {
        timeinfo.tm_hour = (timeinfo.tm_hour + (request_delay_seconds / HOUR)) % 24;
        request_delay_seconds = REQUEST_DELAY_SECONDS % HOUR;

        if ((request_delay_seconds % MINUTE) + timeinfo.tm_sec >= MINUTE)
        {
            timeinfo.tm_min = (timeinfo.tm_min + (request_delay_seconds / MINUTE) + 1) % 60;
            request_delay_seconds = REQUEST_DELAY_SECONDS % MINUTE;
            timeinfo.tm_sec = (timeinfo.tm_sec + request_delay_seconds) % 60;
        }
        else
        {
            timeinfo.tm_min = (timeinfo.tm_min + (request_delay_seconds / MINUTE)) % 60;
            request_delay_seconds = REQUEST_DELAY_SECONDS % MINUTE;
            timeinfo.tm_sec = (timeinfo.tm_sec + request_delay_seconds) % 60;
        }
    }

    tft.print(&timeinfo, "%H:%M:%S\n%A");
}

void setup()
{
    //Deletes stored Wifi Credentials if uncommented
    //WiFiManager manager;
    // manager.resetSettings();

    //Setting up Serial
    Serial.begin(115200);
    Serial.println("Serial initialized");

    //initialize display
    tftInit();

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
            Serial.println("Initializing failed");
            digitalWrite(LED_PIN, HIGH);
            delay(10000);
            ESP.restart();
        }

        //Deactivating Wifi
        disconnectWifi();
        delay(1000);

        Serial.println("Initializing SPIFFS");
        if (!initSPIFFS())
        {
            digitalWrite(LED_PIN, HIGH);
            delay(10000);
            ESP.restart();
        }

        Serial.println("Initializing BLE");
        initBLE();

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

                tft.fillScreen(TFT_BLACK);
                tft.setTextSize(2);
                tft.setCursor(0, 0);
                tft.print("Connect to\n\"Tracker\"\non your phone");
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
        }
        showRequestDelayOnDisplay();
        sendHTTPRequest = false;
        doScan = true;
        wifiTicker.attach(REQUEST_DELAY_SECONDS, setHTTPFlag);
        initBLE();
        delay(500);
    }
}