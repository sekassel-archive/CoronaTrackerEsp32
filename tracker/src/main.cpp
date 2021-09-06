#include <Arduino.h>

#pragma region CUSTOM_DEFINITIONS
#define BUTTON_PIN 0
#define ACTION_NOTHING 0
#define ACTION_ADVERTISE 1
#define ACTION_SCAN 2
#define ACTION_WIFI_CONFIG 3
#define ACTION_INFECTION_REQUEST 4
#define ACTION_SLEEP 5         // just for display
#define SLEEP_INTERVAL 3000000 // microseconds
#define BOOTS_UNTIL_DISPLAY_TURNS_OFF 4
#define ADVERTISE_TIME 500                      // milliseconds
#define INFECTION_REQUEST_INTERVALL int(2 * 60) // auf 18 min setzen!
#pragma endregion CUSTOM_DEFINITIONS

#include "coronatracker-utils.h"

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

#pragma region GLOBAL_VARIABLES
// saved during deep sleep mode
RTC_DATA_ATTR int nextAction = 0;
RTC_DATA_ATTR bool wifiInitialized = false;
RTC_DATA_ATTR int bootCount = 0;
RTC_DATA_ATTR int scannedDevices = -1;
RTC_DATA_ATTR bool firstBoot = true;
RTC_DATA_ATTR bool requestOnStartUp = false; // for disabling startup request
RTC_DATA_ATTR exposure_status exposureStatus = EXPOSURE_NO_UPDATE;
RTC_DATA_ATTR bool isDisplayActive = false;

RTC_DATA_ATTR time_t scanTime;
RTC_DATA_ATTR time_t updateTime;

int buttonState = 0;
int lastButtonState = 0;
int startPressed = 0;

#pragma endregion GLOBAL_VARIABLES

void setNextAction(int action);
void goIntoDeepSleep(bool requestInfections);

// TODO: change location for display stuff
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
        Serial.println("Warning: Can't get local time.");
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
            firstStartInitializeSteps();

            firstBoot = false;

            time_t now = time(NULL);
            scanTime = now + (60); // 60 seconds
            updateTime = now + INFECTION_REQUEST_INTERVALL;

            goIntoDeepSleep(requestOnStartUp);
        }
        startInitializeSteps();
    }

    // button still pressed
    if (digitalRead(0) == 0 && nextAction != ACTION_WIFI_CONFIG)
    {
        processVerificationForUserInput(&exposureStatus);
    }

    switch (nextAction)
    {
    case ACTION_SCAN:
    {
        Serial.println("Start: ACTION_SCAN");
        actionScanForBluetoothDevices(&scannedDevices);
        break;
    }
    case ACTION_ADVERTISE:
    {
        Serial.println("Start: ACTION_ADVERTISE");
        advertiseBluetoothDevice(&scannedDevices);
        break;
    }
    case ACTION_WIFI_CONFIG:
    {
        Serial.println("Start: ACTION_WIFI_CONFIG");
        setupWifiConnection(&wifiInitialized);
        break;
    }
    case ACTION_INFECTION_REQUEST:
    {
        Serial.println("Start: ACTION_INFECTION_REQUEST");
        sendCollectedDataToServer();
        sendExposureInformationIfExists();
        exposureStatus = getInfectionStatusFromServer();

        if (exposureStatus == EXPOSURE_DETECT)
        {
            isDisplayActive = true;
            Serial.println("ExposureState: EXPOSURE_DETECT");
        } else {
            Serial.println("ExposureState: EXPOSURE_NO_DETECT");
        }
        break;
    }
    } // switch case end

    if (isDisplayActive)
    {
        defaultDisplay(timeinfo, ACTION_SLEEP, exposureStatus, scannedDevices);
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
        updateTime += INFECTION_REQUEST_INTERVALL;
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

bool initializeDeviceSpecificDisplay(tm timeinfo)
{
    if (!isDisplayActive)
    {
        Serial.println("Initialize display");
        initDisplay();
    }
    else
    {
        defaultDisplay(timeinfo, nextAction, exposureStatus, scannedDevices);
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
            defaultDisplay(timeinfo, nextAction, exposureStatus, scannedDevices);
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
        defaultDisplay(timeinfo, nextAction, exposureStatus, scannedDevices);
        isDisplayActive = true;
    }
#endif
}