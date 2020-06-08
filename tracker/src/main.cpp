#include <Arduino.h>
#include <Ticker.h>

//BLE Libraries
#include <BLEDevice.h>

//WiFi Libraries
#include <WifiManager.h>
#include <HTTPClient.h>

//BLE Variables
static BLEUUID serviceUUID((uint16_t)0xFD68);                    //UUID taken from App
static BLEUUID charUUID("ae733f1d-b5b6-4e95-b688-ae2acb5133e2"); //Randomly generated

char device_id[30] = "Hallo Welt COVID"; //ID to be braodcasted
bool doScan = false;

//Wifi Variables
const static int BUTTON_PRESS_LENGTH = 4000; //4 Seconds

String ssid;
String password;

int buttonState = 0;
int lastButtonState = 0;
int startPressed = 0;

WiFiManager wifiManager;
Ticker wifiTicker;
Ticker buttonTicker;
bool waitForConfig = false;

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

void printWifiStatus()
{
    Serial.println("WIFI STATUS:\n------------------------------------------");
    Serial.print(" - WiFi Status: ");
    Serial.println(" - [WL_IDLE_STATUS == 0, WL_NO_SSID_AVAIL == 1, WL_SCAN_COMPLETED == 2, WL_CONNECTED == 3, WL_CONNECT_FAILED == 4, WL_CONNECTION_LOST == 5, WL_DISCONNECTED == 6]");
    Serial.print("- Status: ");
    Serial.println(WiFi.status());
    Serial.print(" - Status (Second try): ");
    Serial.println(WiFi.status());
    Serial.print("- Mode: ");
    Serial.println(WiFi.getMode());
    Serial.print("- Is connected?: ");
    Serial.println(WiFi.isConnected());
    Serial.print("- WiFi - SSID: ");
    Serial.println(WiFi.SSID());
    Serial.print("- WiFi IP: ");
    Serial.println(WiFi.localIP());
    Serial.println("- Wifi Print: ");
    Serial.println("------------");
    WiFi.printDiag(Serial);
    Serial.println("------------");
    Serial.println("------------------------------------------");
}

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
    int state = digitalRead(BUILTIN_LED);
    digitalWrite(BUILTIN_LED, !state);
}

void startSimpleHTTPRequest()
{
    printWifiStatus();
    wifiTicker.detach();
    Serial.println("Disabeling BLE");
    doScan = false;
    BLEDevice::getAdvertising()->stop();
    BLEDevice::getScan()->stop();
    delay(10000);

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
            String payload = http.getString();
            Serial.println(payload);
        }
        else
        {
            Serial.println("Error on HTTP request");
        }

        http.end();
        delay(1000);
        disconnectWifi();
    }
    Serial.println("Starting BLE");
    doScan = true;
    BLEDevice::startAdvertising();
    wifiTicker.attach(60, startSimpleHTTPRequest);
}

void configureWifi()
{
    Serial.println("Starting WifiManger-Config");
    buttonTicker.attach_ms(500, blinkLED);
    wifiManager.setConfigPortalTimeout(300); //5 Minutes
    wifiManager.setConnectTimeout(30);       //30 Seconds
    bool res = wifiManager.startConfigPortal();

    buttonTicker.detach();
    if (res)
    {
        Serial.println("We connected to Wifi...");
        digitalWrite(LED_BUILTIN, LOW);
    }
    else
    {
        Serial.println("Could not connect to Wifi");
        digitalWrite(LED_BUILTIN, HIGH);
        wifiManager.resetSettings();
    }

    //Delay so feedback can be seen on LED
    delay(5000);
    ESP.restart();
}

void printLocalTime()
{
    struct tm timeinfo;
    if (!getLocalTime(&timeinfo))
    {
        Serial.println("Failed to obtain time");
        return;
    }
    Serial.println(&timeinfo, "%A, %B %d %Y %H:%M:%S");
}

void setup()
{
    //Deletes stored Wifi Credentials if uncommented
    //wifiManager.resetSettings();

    //Setting up Serial
    Serial.begin(115200);
    Serial.println("Serial initialized");

    //Setting up pinModes
    Serial.println("Setting up pinModes");
    pinMode(LED_BUILTIN, OUTPUT);
    pinMode(KEY_BUILTIN, INPUT); // Button input

    //Connection Failed
    if (!connectToStoredWifi())
    {
        Serial.println("Awaiting Putton Press for Configuration");
        digitalWrite(LED_BUILTIN, HIGH);
        waitForConfig = true;
    }
    else
    {
        digitalWrite(LED_BUILTIN, LOW);

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
        wifiTicker.attach(60, startSimpleHTTPRequest);
    }
}

void loop()
{
    if (doScan)
    {
        Serial.print("Starting Scan... Devices Found: ");
        Serial.println((BLEDevice::getScan()->start(1, false)).getCount());
        delay(10000); //Scan Every 10 Seconds
    }
    else if (waitForConfig)
    {
        buttonState = digitalRead(KEY_BUILTIN); // read the button input

        //Button was pressed
        if (buttonState == LOW)
        {
            //First press
            if (buttonState != lastButtonState)
            {
                startPressed = millis();
            }
            else if ((millis() - startPressed) >= BUTTON_PRESS_LENGTH)
            {
                configureWifi();
            }
        }

        lastButtonState = buttonState;
        delay(500);
    }
}