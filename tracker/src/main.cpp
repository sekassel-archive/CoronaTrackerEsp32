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
bool startConfig = false;

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

void blinkLED()
{
    int state = digitalRead(BUILTIN_LED);
    digitalWrite(BUILTIN_LED, !state);
}

void wifiRequestTest()
{
    Serial.println("Starting A HTTP Request");
    doScan = false;
    BLEDevice::getAdvertising()->stop();
    BLEDevice::getScan()->stop();
    delay(1000);

    wifiManager.setConfigPortalTimeout(10); //Workaround so that we dont really start the Portal
    wifiManager.setConnectTimeout(45);      //45 Seconds

    Serial.println(ssid);
    Serial.println(password);

    //Serial.println(WiFi.begin(ssid.c_str(), password.c_str()));
    Serial.println(wifiManager.getWiFiSSID().c_str());
    Serial.println(wifiManager.getWiFiPass().c_str());

    Serial.print("Is Wifi Connected: ");
    Serial.println(WiFi.isConnected());
    Serial.print("Wifi AutoReConnect (0 == OFF): ");
    Serial.println(WiFi.getAutoReconnect());
    Serial.print("Wifi Mode (0 == OFF): ");
    Serial.println(WiFi.getMode());
    Serial.print("Wifi-SSID: ");
    Serial.println(WiFi.SSID());
    Serial.print("Wifi-Status: ");
    Serial.println(WiFi.status());
    Serial.print("Wifi-Status: ");
    Serial.println(WiFi.begin());

    /*
    bool con = true;
    int i = 0;

    while (WiFi.status() != WL_CONNECTED)
    {
        if (i == 30)
        {
            con = false;
            break;
        }
        Serial.println(WiFi.status());
        delay(500);
        Serial.print(".");
        i++;
    }
    */
    if (wifiManager.autoConnect())
    {
        Serial.print("Is Wifi Connected: ");
        Serial.println(WiFi.isConnected());
        Serial.print("Wifi AutoReConnect (0 == OFF): ");
        Serial.println(WiFi.getAutoReconnect());
        Serial.print("Wifi Mode (0 == OFF): ");
        Serial.println(WiFi.getMode());
        Serial.print("Wifi-SSID: ");
        Serial.println(WiFi.SSID());
        Serial.print("Wifi-Status: ");
        Serial.println(WiFi.status());
        Serial.print("Wifi-Status: ");
        Serial.println(WiFi.begin());

        Serial.println("Sending HTTP...");

        //Simply http Request for demonsttation
        HTTPClient http;

        http.begin("192.168.178.59:4567/hello"); //Specify the URL
        int httpCode = http.GET();               //Make the request

        if (httpCode > 0)
        { //Check for the returning code

            String payload = http.getString();
            Serial.println(httpCode);
            Serial.println(payload);
        }
        else
        {
            Serial.print("Error on HTTP request: ");
            Serial.println(httpCode);
        }
        http.end(); //Free the resources

        delay(1000);
        Serial.println("Trying to disconnect");
        wifiManager.disconnect();
        Serial.println("Hopefully disconnected");

        Serial.print("Is Wifi Connected: ");
        Serial.println(WiFi.isConnected());
        Serial.print("Wifi AutoReConnect (0 == OFF): ");
        Serial.println(WiFi.getAutoReconnect());
        Serial.print("Wifi Mode (0 == OFF): ");
        Serial.println(WiFi.getMode());
        Serial.print("Wifi-SSID: ");
        Serial.println(WiFi.SSID());
        Serial.print("Wifi-Status: ");
        Serial.println(WiFi.status());
        Serial.print("Wifi-Status: ");
        Serial.println(WiFi.begin());
    }
    else
    {
        Serial.println("Could not Connect to Wifi - Retrying later");
    }
    Serial.println("was here1");
    delay(1000);
    doScan = true;
    Serial.println("was here");
    BLEDevice::startAdvertising();
}

void configureWifi()
{
    Serial.println("Starting WifiManger-Config");
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

void checkButtonPress()
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
            Serial.println("Button was pressed for 4 seconds");
            Serial.println("");
            buttonTicker.detach();
            buttonTicker.attach_ms(500, blinkLED);
            startConfig = true;
        }
    }

    lastButtonState = buttonState;
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
    //wifiManager.resetSettings();

    //Setting up Serial
    Serial.begin(115200);
    Serial.println("Serial initialized");

    //Setting up pinModes
    Serial.println("Setting up pinModes");
    pinMode(LED_BUILTIN, OUTPUT);
    pinMode(KEY_BUILTIN, INPUT); // Button input

    //Setting up WifiManger
    Serial.println("Setting up WifiManager");
    //Setiings for first connect on startup
    wifiManager.setConfigPortalTimeout(10); //Workaround so that we dont really start the Portal
    wifiManager.setConnectTimeout(30);      //30 Seconds
    wifiManager.setCleanConnect(true);

    //If we already have a network saved we connect to it, otherwise we wait for configuration
    if (!wifiManager.autoConnect("COVID-AP", "covid-19"))
    {
        delay(1000);
        Serial.println("Wifi not yet set up. Awaiting Putton Press for Configuration");
        digitalWrite(LED_BUILTIN, HIGH);

        //Setup Config Button
        buttonTicker.attach_ms(500, checkButtonPress);
    }
    else
    {
        delay(1000);
        ssid = wifiManager.getWiFiSSID();
        password = wifiManager.getWiFiPass();
        Serial.printf("Wifi already setup.\nSSID: %s, Password: %s\n", ssid.c_str(), password.c_str());
        digitalWrite(LED_BUILTIN, LOW);

        //Getting Time
        configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
        printLocalTime();

        //Shutting down Wifi
        wifiManager.disconnect();
        //WiFi.disconnect(true, false);
        //WiFi.mode(WIFI_OFF);

        //WiFi.disconnect(true, false);
        delay(1000);

        WiFi.setAutoReconnect(false);

        Serial.print("Is Wifi Connected: ");
        Serial.println(WiFi.isConnected());
        Serial.print("Wifi AutoReConnect (0 == OFF): ");
        Serial.println(WiFi.getAutoReconnect());
        Serial.print("Wifi Mode (0 == OFF): ");
        Serial.println(WiFi.getMode());
        Serial.print("Wifi-SSID: ");
        Serial.println(WiFi.SSID());
        Serial.print("Wifi-Status: ");
        Serial.println(WiFi.status());
        Serial.print("Wifi-Status: ");
        Serial.println(WiFi.begin());

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
        wifiTicker.attach(60, wifiRequestTest);
    }
}

void loop()
{
    if (doScan)
    {
        Serial.println("Starting Scan...");
        Serial.println((BLEDevice::getScan()->start(1, false)).getCount());
    }
    else if (startConfig)
    {
        configureWifi();
    }
    delay(10000); //Scan Every 10 Seconds
}