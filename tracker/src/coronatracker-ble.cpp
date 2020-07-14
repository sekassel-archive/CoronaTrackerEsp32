#include "coronatracker-ble.h"

static BLEUUID serviceUUID((uint16_t)0xFD68); //UUID taken from App
char device_id[30] = "Hallo Welt COVID";      //ID to be braodcasted
std::multimap<std::string, time_t> recentEncounterMap;

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
            Serial.print(advertisedDevice.toString().c_str());
            Serial.print(" --> ID: ");
            Serial.println(advertisedDevice.getServiceData().c_str());

            recentEncounterMap.insert(std::make_pair(advertisedDevice.getServiceData(), time(NULL)));
        }
    }
};
MyAdvertisedDeviceCallbacks myCallbacks;

void initBLE()
{
    //Setting up Server
    Serial.println("Setting Up Server");
    BLEDevice::init("CovidTracker");
    pServer = BLEDevice::createServer();
    pService = pServer->createService(serviceUUID);
    pService->start();

    //Service Data
    BLEAdvertisementData oAdvertisementData = BLEAdvertisementData();
    oAdvertisementData.setServiceData(serviceUUID, device_id);

    Serial.println("Setting up Advertisment");
    pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(serviceUUID);
    pAdvertising->setAdvertisementData(oAdvertisementData);
    pAdvertising->setMinPreferred(0x06);
    pAdvertising->setMinPreferred(0x12);
    pAdvertising->start();

    //Setting up Scan
    Serial.println("Setting up Scan");
    pBLEScan = BLEDevice::getScan();
    pBLEScan->setAdvertisedDeviceCallbacks(&myCallbacks);
    pBLEScan->setActiveScan(true); //active scan uses more power, but get results faster
    pBLEScan->setInterval(100);
    pBLEScan->setWindow(99); // less or equal setInterval value
}

void deinitBLE()
{
    pAdvertising->stop();
    pBLEScan->stop();
    BLEDevice::deinit(false);
    delete pServer;
    delete pService;
}

void scanForCovidDevices(uint32_t duration)
{
    BLEDevice::getScan()->start(duration, false);
}

std::multimap<std::string, time_t> *getRecentEncounters()
{
    return &recentEncounterMap;
}