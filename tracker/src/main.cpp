#include <Arduino.h>
#include <Ticker.h>

//BLE Libraries
#include <BLEDevice.h>

//WiFi Libraries
#include <WifiManager.h>
#include <HTTPClient.h>

#define LED_PIN 4
#define TP_PWR_PIN 25
#define TP_PIN_PIN 33

//BLE Variables
static BLEUUID serviceUUID((uint16_t)0xFD68);                    //UUID taken from App
static BLEUUID charUUID("ae733f1d-b5b6-4e95-b688-ae2acb5133e2"); //Randomly generated

char device_id[30] = "Hallo Welt COVID"; //ID to be braodcasted
bool doScan = false;
const static int SCAN_DELAY_MILLISECONDS = 10000; //10 Seconds

//Wifi Variables
const static int BUTTON_PRESS_DURATION_MILLISECONDS = 4000; //4 Seconds
const static int REQUEST_DELAY_SECONDS = 60;           //60 Seconds
//const static int REQUEST_DELAY_SECONDS = 3600; // 1hour -> Final Time

const String SERVER_URL = "https://tracing.uniks.de";

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

        http.begin(SERVER_URL + "/hello");
        int httpCode = http.GET();

        Serial.print("ReturnCode: ");
        Serial.println(httpCode);
        if (httpCode > 0)
        {
            Serial.println("---------- Message ----------");
            String payload = http.getString();
            Serial.println(payload);
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
}

void setHTTPFlag()
{
    doScan = false;
    sendHTTPRequest = true;
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

    //Connection Failed
    if (!connectToStoredWifi())
    {
        Serial.println("Awaiting Putton Press for Wifi-Configuration");
        digitalWrite(LED_PIN, HIGH);
        waitForConfig = true;
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

void loop()
{
    if (doScan)
    {
        Serial.println("Starting Scan...");
        int result = (BLEDevice::getScan()->start(1, false)).getCount();
        Serial.printf("Devices Found: %i\n", result);
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