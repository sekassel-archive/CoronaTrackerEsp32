#include <Arduino.h>
#include <Ticker.h>

#pragma region DISPLAY_INCLUDES

#ifdef LILYGO_WATCH_2020_V1
#include "LilyGoWatch.h"
#include "coronatracker-display-ttgowatch.h"
#endif

#ifdef ESP32DEVOTA_COMMON
#include "coronatracker-display-ssd1306wire.h"
#endif

#ifdef LILYGO_WRISTBAND
#include "coronatracker-display-wristband.h"
#endif
#pragma endregion DISPLAY_INCLUDES

#include "coronatracker-utils.h"

#define BUTTON_PIN 0
#ifndef LED_PIN
#define LED_PIN 16
#endif

#define ACTION_NOTHING 0
#define ACTION_ADVERTISE 1
#define ACTION_SCAN 2
#define ACTION_WIFI_CONFIG 3
#define ACTION_INFECTION_REQUEST 4
#define ACTION_SLEEP 5 // just for display

#define SLEEP_INTERVAL 3000000 // microseconds

#define BOOTS_UNTIL_DISPLAY_TURNS_OFF 4
#define SCAN_TIME 10       // seconds
#define ADVERTISE_TIME 500 // milliseconds

// saved during deep sleep mode
RTC_DATA_ATTR int nextAction = 0;
RTC_DATA_ATTR bool wifiInitialized = false;
RTC_DATA_ATTR int bootCount = 0;
RTC_DATA_ATTR int scanedDevices = -1;
RTC_DATA_ATTR bool firstBoot = true;
RTC_DATA_ATTR bool requestOnStartUp = false; // for disabling startup request
RTC_DATA_ATTR exposure_status exposureStatus = EXPOSURE_NO_UPDATE;
RTC_DATA_ATTR bool isDisplayActive = false;

RTC_DATA_ATTR time_t scanTime;
RTC_DATA_ATTR time_t updateTime;

RTC_DATA_ATTR char uuidString[36];

// wifi variables
const static int BUTTON_PRESS_DURATION_MILLISECONDS = 4000;

int buttonState = 0;
int lastButtonState = 0;
int startPressed = 0;

Ticker buttonTicker;

// time variables
const char *ntpServer = "pool.ntp.org";
const long gmtOffset_sec = 3600;
const int daylightOffset_sec = 3600;

void blinkLED();
bool initializeTime();
size_t restartAfterErrorWithDelay(String errorMessage);
void setNextAction(int action);
void goIntoDeepSleep(bool requestInfections);
bool initializeTek();
bool initializeDeviceSpecificDisplay(tm timeinfo);

void setup()
{
    // setting up serial
    Serial.begin(115200);

    float start = micros();
    float end;

    // setting up pinModes
    pinMode(LED_PIN, OUTPUT);
    pinMode(BUTTON_PIN, PULLUP);
    digitalWrite(LED_PIN, HIGH); // LED off

    struct tm timeinfo;
    if (!getLocalTime(&timeinfo))
    {
        Serial.println("Warning: can't get local time");
    }

    initializeDeviceSpecificDisplay(timeinfo);

    if (!wifiInitialized)
    {
        digitalWrite(LED_PIN, LOW);
        setNextAction(ACTION_WIFI_CONFIG);
    }
    else
    {
        if (firstBoot)
        {
            firstBoot = false;

            !initSPIFFS(true) ? restartAfterErrorWithDelay("SPIFFS initialize failed!") : Serial.println("Initialized SPIFFS.");
            !connectToStoredWifi() ? restartAfterErrorWithDelay("Couldn't connect to Wifi!") : Serial.println("Connected to WiFi.");
            !initializeTime() ? restartAfterErrorWithDelay("Time initialize failed!") : Serial.println("Initialized Time.");
            !initializeTek() ? restartAfterErrorWithDelay("Failed to generate Temporary Exposure Key!") : Serial.println("Initialized Tek.");
            !initializeUuid(uuidString) ? restartAfterErrorWithDelay("Failed to get UUID!") : Serial.println("Initialized UUID.");
            !disconnectWifi() ? Serial.println("Disconnect Failed!") : Serial.println("Disconnecting Wifi.");

            time_t now = time(NULL);
            scanTime = now + (60);        // 60 seconds
            updateTime = now + (18 * 60); // 18 minutes

            goIntoDeepSleep(requestOnStartUp);
        }

        !initSPIFFS(false) ? restartAfterErrorWithDelay("SPIFFS initialize failed!") : Serial.println("Initialized SPIFFS.");
    }

    switch (nextAction)
    {
    case ACTION_SCAN:
    {
        Serial.println("Start: ACTION_SCAN");
        initializeBluetoothForScan();
        std::vector<std::__cxx11::string> rpis = scanForCovidDevices((uint32_t)SCAN_TIME);
        deinitBLE(true); // free memory for database interaction
        scanedDevices = rpis.size();
        insertTemporaryRollingProximityIdentifiers(time(NULL), rpis);
        cleanUpTempDatabase();
        break;
    }
    case ACTION_ADVERTISE:
    {
        Serial.println("Start: ACTION_ADVERTISE");
        if (initializeBluetoothForAdvertisment() == false)
        {
            restartAfterErrorWithDelay("Initialize Bluetooth for Advertisment failed!");
        }

        digitalWrite(LED_PIN, LOW);
        delay(ADVERTISE_TIME);
        digitalWrite(LED_PIN, HIGH);
        break;
    }
    case ACTION_WIFI_CONFIG:
    {
        Serial.println("Start: ACTION_WIFI_CONFIG");
        buttonTicker.attach_ms(500, blinkLED);

        configureWifiMessageOnDisplay();
        bool res = configureWifi();

        buttonTicker.detach();
        if (res)
        {
            Serial.println("Successfully connected to Wifi!");
            wifiInitialized = true;
            digitalWrite(LED_PIN, HIGH);
            wifiConfiguredOnDisplay(true);
        }
        else
        {
            Serial.println("Couldn't connect to Wifi!");
            digitalWrite(LED_PIN, LOW);
            wifiConfiguredOnDisplay(false);
            // delay so feedback can be seen on LED
            delay(5000);
            ESP.restart();
        }
        delay(5000);
        disconnectWifi();
        break;
    }
    case ACTION_INFECTION_REQUEST:
    {
        Serial.println("Start: ACTION_INFECTION_REQUEST");
        defaultDisplay(timeinfo, nextAction, exposureStatus, scanedDevices); // while infection request the display is always on
        exposureStatus = checkForInfections();
        afterInfectionRequestOnDisplay(exposureStatus);
        delay(10000);
        break;
    }
    } // switch case end

    if (isDisplayActive)
    {
        defaultDisplay(timeinfo, ACTION_SLEEP, exposureStatus, scanedDevices);
    }

    end = micros();
    float result = end - start;
    result /= 1000; // convert to milliseconds
    Serial.printf("Time(milliseconds): %g\n", result);

    goIntoDeepSleep(false);
}

void loop()
{
    // never called
}

void blinkLED()
{
    int state = digitalRead(LED_PIN);
    digitalWrite(LED_PIN, !state);
}

bool initializeTime()
{
    struct tm timeinfo;
    int start = millis();
    const int WAITTIME = 180000; // 3 Minutes

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
    delay(5000);
    return true;
}

size_t restartAfterErrorWithDelay(String errorMessage)
{
    digitalWrite(LED_PIN, LOW);
    Serial.print("Restart after Error: ");
    Serial.println(errorMessage);
    delay(10000);
    ESP.restart();
    return 0;
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

void goIntoDeepSleep(bool requestInfections)
{
    time_t nextBootTime = time(NULL) + (SLEEP_INTERVAL / (1000 * 1000)) - 1; //Next Boot with Offset

    if (requestInfections) //Request Infection on First boot after initialize
    {
        setNextAction(ACTION_INFECTION_REQUEST);
    }
    else if (nextBootTime >= updateTime)
    {
        updateTime += (60 * 60);
        setNextAction(ACTION_INFECTION_REQUEST);
    }
    else if (nextBootTime >= scanTime)
    {
        scanTime += (60);
        setNextAction(ACTION_SCAN);
    }
    else
    {
        setNextAction(ACTION_ADVERTISE);
    }

    esp_sleep_enable_ext0_wakeup(GPIO_NUM_0, LOW);
    esp_sleep_enable_timer_wakeup(SLEEP_INTERVAL);
    esp_deep_sleep_start();
}

bool initializeTek()
{
    Serial.print("Generating TEK - ");
    signed char tek[16];
    int enin;
    if (getCurrentTek(tek, &enin))
    {
        Serial.print("TEK already exists: ");
        for (int i = 0; i < (sizeof(tek) / sizeof(tek[0])); i++)
        {
            Serial.print(tek[i]);
        }
        Serial.println(" ");
    }
    else
    {
        if (!generateNewTemporaryExposureKey(calculateENIntervalNumber(time(NULL))))
        {
            return false;
        }
        Serial.print("Generated TEK:");
        for (int i = 0; i < (sizeof(tek) / sizeof(tek[0])); i++)
        {
            Serial.print(tek[i]);
        }
        Serial.println(" ");
    }
    return true;
}

bool initializeDeviceSpecificDisplay(tm timeinfo)
{
    if (!isDisplayActive)
    {
        Serial.println("Initialize display");
        initDisplay();
    }
    else
    {
        defaultDisplay(timeinfo, nextAction, exposureStatus, scanedDevices);
    }

    esp_sleep_wakeup_cause_t wakeup_reason;
    wakeup_reason = esp_sleep_get_wakeup_cause();

#ifdef ESP32DEVOTA_COMMON
    buttonState = digitalRead(BUTTON_PIN);
    if ((wakeup_reason == ESP_SLEEP_WAKEUP_EXT0 || buttonState == LOW) && wifiInitialized) // LOW means clicked
    {
        Serial.println("Wakeup caused by external signal using RTC_IO");
        if (!isDisplayActive)
        {
            defaultDisplay(timeinfo, nextAction, exposureStatus, scanedDevices);
            isDisplayActive = true;
        }
        else
        {
            initDisplay();
            isDisplayActive = false;
        }
    }
#endif

#if defined(LILYGO_WATCH_2020_V1) || defined(LILYGO_WRISTBAND)
    if (wifiInitialized)
    {
        Serial.println("Update display every time for lilygo devices");
        defaultDisplay(timeinfo, nextAction, exposureStatus, scanedDevices);
        isDisplayActive = true;
    }
#endif
}