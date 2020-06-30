#include <Arduino.h>
#include <Ticker.h>
#include "SPIFFS.h"

//BLE Libraries
#include <BLEDevice.h>

//WiFi Libraries
#include <WifiManager.h>
#include <HTTPClient.h>

//display Libraries
#include <SPI.h>
#include <TFT_eSPI.h>
#include <Wire.h>

#define LED_PIN 4
#define TP_PWR_PIN 25
#define TP_PIN_PIN 33

TFT_eSPI tft = TFT_eSPI();

//BLE Variables
static BLEUUID serviceUUID((uint16_t)0xFD68);                    //UUID taken from App
static BLEUUID charUUID("ae733f1d-b5b6-4e95-b688-ae2acb5133e2"); //Randomly generated

char device_id[30] = "Hallo Welt COVID"; //ID to be braodcasted
bool doScan = false;
const static int SCAN_DELAY_MILLISECONDS = 10000; //10 Seconds

const char *path = "/encounters.txt";
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

std::multimap<std::string, time_t> recentEncounterMap;

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

    tft.setTextSize(1);//With size equals to 1, you can print 10 lines and about 27 characters per line
    tft.setCursor(0, 0);
}

class MyAdvertisedDeviceCallbacks : public BLEAdvertisedDeviceCallbacks
{
    //Called for each advertising BLE server.
    void onResult(BLEAdvertisedDevice advertisedDevice)
    {
        //Checking for other Coviddevices
        if (advertisedDevice.haveServiceData() && advertisedDevice.getServiceDataUUID().equals(serviceUUID))
        {
            Serial.print("Found Covid Device: ");
            Serial.println(advertisedDevice.toString().c_str());
            Serial.print("ID: ");
            Serial.println(advertisedDevice.getServiceData().c_str());

            recentEncounterMap.insert(std::make_pair(advertisedDevice.getServiceData(), time(NULL)));
        }
    }
};

bool disconnectWifi()
{
    Serial.println("Deactivating Wifi");
    return WiFi.disconnect(true, false);
}

bool connectToStoredWifi()
{
    Serial.println("Trying to establish to Wifi-Connection.");
    WiFi.mode(WIFI_STA);
    WiFi.begin();
    return WiFi.waitForConnectResult() == WL_CONNECTED;
}

void blinkLED()
{
    int state = digitalRead(LED_PIN);
    digitalWrite(LED_PIN, !state);
}

void startSimpleHTTPRequest()
{
    Serial.println("Sending Simple HTTP Request.");
    if (!connectToStoredWifi())
    {
        Serial.println("Could not Connect to Wifi - Retrying later");
    }
    else
    {
        HTTPClient http;

        http.begin("http://httpbin.org/user-agent");
        int httpCode = http.GET();

        Serial.print("ReturnCode: ");
        Serial.println(httpCode);
        if (httpCode > 0)
        {
            Serial.println("---------- Message ----------");
            String payload = http.getString();
            Serial.print(payload);
            Serial.println("-----------------------------");
        }
        else
        {
            Serial.println("Error on HTTP request");
        }

        http.end();
        delay(1000);
        disconnectWifi();
    }
}

void configureWifi()
{
    Serial.println("Starting WifiManger-Config");
    buttonTicker.attach_ms(500, blinkLED);

    WiFiManager wifiManager;
    wifiManager.setConfigPortalTimeout(300); //5 Minutes
    wifiManager.setConnectTimeout(30);       //30 Seconds
    bool res = wifiManager.startConfigPortal();

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
        wifiManager.resetSettings();
        //Delay so feedback can be seen on LED
        delay(5000);
    }
    ESP.restart();
}

void showLocalTimeOnDisplay(struct tm timeinfo){
    tft.fillScreen(TFT_BLACK);
    tft.setTextSize(2);
    tft.print(&timeinfo, "%A\n%B %d %Y\n%H:%M:%S");//could look better when centered
}

void printLocalTime()
{
    Serial.print("Local Time: ");
    struct tm timeinfo;
    if (!getLocalTime(&timeinfo))
    {
        Serial.println("Failed to obtain time");
        return;
    }
    Serial.println(&timeinfo, "%A, %B %d %Y %H:%M:%S");
    showLocalTimeOnDisplay(timeinfo);
}

void setHTTPFlag()
{
    doScan = false;
    sendHTTPRequest = true;
}

void showStartWifiMessageOnDisplay(){
    tft.fillScreen(TFT_BLACK);
    tft.setTextSize(2);
    tft.print("Press Button\nfor 4 Seconds\nto start \nWifi-\nConfiguration");
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
        configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
        printLocalTime();

        //Deactivating Wifi
        disconnectWifi();
        delay(1000);

        Serial.println("Initializing SPIFFS");
        if (!SPIFFS.begin(true)) //Error on first flash, after 30 seconds continues?
        {
            Serial.println("Initializing failed");
            digitalWrite(LED_PIN, HIGH);
            delay(10000);
            ESP.restart();
        }

        //Remove comment to reset file
        //SPIFFS.remove(path);

        if (!SPIFFS.exists(path))
        {
            Serial.println("Creating File");
            File file = SPIFFS.open(path);

            if (!file)
            {
                Serial.println("There was an error creating the file");
                digitalWrite(LED_PIN, HIGH);
                delay(10000);
                ESP.restart();
            }
            file.close();
        }

        //Setting up Server
        Serial.println("Setting Up Server");
        BLEDevice::init("CovidTracker");
        BLEServer *pServer = BLEDevice::createServer();
        BLEService *pService = pServer->createService(serviceUUID);
        //Characteristic is not present in app
        BLECharacteristic *pCharacteristic = pService->createCharacteristic(charUUID, BLECharacteristic::PROPERTY_BROADCAST);
        pCharacteristic->setValue(device_id);
        pService->start();

        //Service Data
        BLEAdvertisementData oAdvertisementData = BLEAdvertisementData();
        oAdvertisementData.setServiceData(serviceUUID, device_id);

        Serial.println("Setting up Advertisment");
        BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
        pAdvertising->addServiceUUID(serviceUUID);
        pAdvertising->setAdvertisementData(oAdvertisementData);
        pAdvertising->setMinPreferred(0x06);
        pAdvertising->setMinPreferred(0x12);
        BLEDevice::startAdvertising();

        //Setting up Scan
        Serial.println("Setting up Scan");
        BLEScan *pBLEScan = BLEDevice::getScan();
        pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
        pBLEScan->setActiveScan(true); //active scan uses more power, but get results faster
        pBLEScan->setInterval(100);
        pBLEScan->setWindow(99); // less or equal setInterval value

        doScan = true;
        wifiTicker.attach(60, setHTTPFlag);
    }
}

bool fileContainsString(const char *path, std::string str)
{
    int index = 0;
    int len = str.length();

    File file = SPIFFS.open(path, FILE_READ);
    if (!file)
    {
        return false;
    }

    if (len == 0)
    {
        return false;
    }

    while (file.available())
    {
        char c = file.read();
        if (c != str[index])
        {
            index = 0;
        }

        if (c == str[index])
        {
            if (++index >= len)
            {
                return true;
            }
        }
    }
    return false;
}

void loop()
{
    if (doScan)
    {
        Serial.println("Starting Scan...");
        int result = (BLEDevice::getScan()->start(1, false)).getCount();
        Serial.printf("Devices Found: %i\n", result);

        File file = SPIFFS.open(path, FILE_APPEND);
        if (file)
        {
            for (auto it = recentEncounterMap.begin(), end = recentEncounterMap.end(); it != end; it = recentEncounterMap.upper_bound(it->first))
            {
                if (recentEncounterMap.count(it->first) >= ENCOUNTERS_NEEDED && !fileContainsString(path, it->first))
                {
                    std::string stringToAppend = it->first + ";";
                    if (file.print(stringToAppend.c_str()))
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
            for (auto it = recentEncounterMap.cbegin(), next_it = it; it != recentEncounterMap.cend(); it = next_it)
            {
                ++next_it;
                if (it->second < fifteenMinutesAgo)
                {
                    recentEncounterMap.erase(it);
                }
            }
        }
        else
        {
            Serial.println("Could not open encounters.txt");
        }
        file.close();
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
                configureWifi();
            }
        }

        lastButtonState = buttonState;
        delay(500);
    }
    else if (sendHTTPRequest)
    {
        wifiTicker.detach();
        startSimpleHTTPRequest();
        sendHTTPRequest = false;
        doScan = true;
        wifiTicker.attach(REQUEST_DELAY_SECONDS, setHTTPFlag);
        delay(500);
    }
}