#include "coronatracker-ble.h"

static BLEUUID serviceUUID((uint16_t)0xFD6F); //UUID taken from App
signed char tek[TEK_LENGTH];                  //Temporary Exposure Key

BLEServer *pServer;
BLEService *pService;
BLEAdvertising *pAdvertising;
BLEScan *pBLEScan;

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

            if (advertisedDevice.getServiceData().length() < 16)
            {
                Serial.println("Advertised Data is too short");
                return;
            }

            const char *serviceData = advertisedDevice.getServiceData().c_str();
            signed char data[16];
            for (int i = 0; i < 16; i++)
            {
                data[i] = serviceData[i];
            }

            if (!insertRollingProximityIdentifier(time(NULL), data, 16, false))
            {
                Serial.println("Failed to insert RPI!");
            }
        }
    }
};
MyAdvertisedDeviceCallbacks myCallbacks;

bool generateNewTEK()
{
    Serial.println("Generate TEK called!");
    generateTemporaryExposureKey(tek);

    time_t current_time;
    time(&current_time);
    int enin = calculateENIntervalNumber(current_time);

    return insertTemporaryExposureKeyIntoDatabase(tek, TEK_LENGTH, enin);
}

bool initBLE(bool initScan, bool initAdvertisment)
{
    //Needs to be done before bluetooth else out of memory error
    if (initAdvertisment)
    {
        int enin;
        if (!getCurrentTek(tek, &enin))
        {
            Serial.println("Error retrieving current tek");
            return false;
        }

        time_t current_time;
        time(&current_time);
        int currentENIN = calculateENIntervalNumber(current_time);

        //If entry is older than a day
        if (currentENIN - EKROLLING_PERIOD >= enin)
        {
            Serial.printf("Generating new TEK at: %d\n", currentENIN);
            generateNewTEK();
        }
    }

    //Setting up Server
    Serial.println("Setting Up Server");
    BLEDevice::init("CovidTracker");
    pServer = BLEDevice::createServer();
    pService = pServer->createService(serviceUUID);
    pService->start();

    //Service Data
    if (initAdvertisment)
    {
        esp_power_level_t power = esp_ble_tx_power_get(ESP_BLE_PWR_TYPE_ADV);
        //Starts at 0 with -12dbm, each step adds 3 dbm
        signed char tpl = -12 + power * 3;

        signed char version = 1; //00000001, Stands for 01:00

        time_t current_time;
        time(&current_time);
        int enin = calculateENIntervalNumber(current_time);

        signed char payload[20];
        int err = getAdvertisingPayload((const unsigned char *)tek, enin, version, tpl, (unsigned char *)payload);

        if (err != 0)
        {
            Serial.printf("Error on adertising payload: %d\n", err);
            return false;
        }

        BLEAdvertisementData oAdvertisementData = BLEAdvertisementData();
        oAdvertisementData.setServiceData(serviceUUID, std::string(reinterpret_cast<char *>(payload)));

        Serial.println("Setting up Advertisment");
        pAdvertising = BLEDevice::getAdvertising();
        pAdvertising->addServiceUUID(serviceUUID);
        pAdvertising->setAdvertisementData(oAdvertisementData);
        pAdvertising->setMinPreferred(0x06);
        pAdvertising->setMinPreferred(0x12);
        pAdvertising->start();
    }
    //Setting up Scan
    if (initScan)
    {

        Serial.println("Setting up Scan");
        pBLEScan = BLEDevice::getScan();
        pBLEScan->setAdvertisedDeviceCallbacks(&myCallbacks);
        pBLEScan->setActiveScan(true); //active scan uses more power, but get results faster
        pBLEScan->setInterval(100);
        pBLEScan->setWindow(99); // less or equal setInterval value
    }

    return true;
}

void deinitBLE()
{
    if (pAdvertising != nullptr)
    {
        pAdvertising->stop();
    }
    if (pBLEScan != nullptr)
    {
        pBLEScan->stop();
    }

    BLEDevice::deinit(false);
    delete pServer;
    delete pService;
}

void scanForCovidDevices(uint32_t duration)
{
    BLEDevice::getScan()->start(duration, false);
}