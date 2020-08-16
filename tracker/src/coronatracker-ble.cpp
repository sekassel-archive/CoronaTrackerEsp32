#include "coronatracker-ble.h"
#include "coronatracker-spiffs.h"

static BLEUUID serviceUUID((uint16_t)0xFD68); //UUID taken from App
char device_id[30] = "Hallo Welt COVID";      //ID to be braodcasted

//TODO Move to main
//TODO Is now resettet after every sleep,either store on flash or in rtc memory
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

            //TODO Write to file instead 
            File file = SPIFFS.open(RECENTENCOUNTERS_PATH, FILE_APPEND);

            // sqlite3 *db;
            // sqlite3_initialize();
            // int rc = sqlite3_open("/spiffs/test.db", &db);


            file.print(advertisedDevice.getServiceData().c_str());
            file.print(",");
            file.print(time(NULL));
            file.print(";");
            file.close();

            // recentEncounterMap.insert(std::make_pair(advertisedDevice.getServiceData(), time(NULL)));
        }
    }
};
MyAdvertisedDeviceCallbacks myCallbacks;

bool initBLE(bool initScan, bool initAdvertisment)
{

    //Setting up Server
    Serial.println("Setting Up Server");
    // BLEDevice::init("CovidTracker");
    // pServer = BLEDevice::createServer();
    // pService = pServer->createService(serviceUUID);
    // pService->start();

    // Serial.print("\nBLEDevice::init(\"CovidTracker\");");
    // float start11 = micros();
    // float end11;

    BLEDevice::init("CovidTracker");

    // end11 = micros();
    // float result11 = end11 - start11;
    // result11 /= 1000; //convert to milliseconds
    // Serial.printf("Time(milliseconds): %g\n", result11);

    // Serial.print("\npServer = BLEDevice::createServer();");
    // float start12 = micros();
    // float end12;

    pServer = BLEDevice::createServer();

    // end12 = micros();
    // float result12 = end12 - start12;
    // result12 /= 1000; //convert to milliseconds
    // Serial.printf("Time(milliseconds): %g\n", result12);


    // Serial.print("\npService = pServer->createService(serviceUUID);");
    // float start13 = micros();
    // float end13;

    pService = pServer->createService(serviceUUID);

    // end13 = micros();
    // float result13 = end13 - start13;
    // result13 /= 1000; //convert to milliseconds
    // Serial.printf("Time(milliseconds): %g\n", result13);


    // Serial.print("\npService->start();");
    // float start14 = micros();
    // float end14;

    pService->start();

    // end14 = micros();
    // float result14 = end14 - start14;
    // result14 /= 1000; //convert to milliseconds
    // Serial.printf("Time(milliseconds): %g\n", result14);

    //Service Data
    if (initAdvertisment)
    {
        // Serial.print("\nBLEAdvertisementData oAdvertisementData = BLEAdvertisementData();");
        // float start1 = micros();
        // float end1;

        BLEAdvertisementData oAdvertisementData = BLEAdvertisementData();

        // end1 = micros();
        // float result1 = end1 - start1;
        // result1 /= 1000; //convert to milliseconds
        // Serial.printf("Time(milliseconds): %g\n", result1);

        // Serial.print("\noAdvertisementData.setServiceData(serviceUUID, device_id);");
        // float start2 = micros();
        // float end2;

        oAdvertisementData.setServiceData(serviceUUID, device_id);

        // end2 = micros();
        // float result2 = end2 - start2;
        // result2 /= 1000; //convert to milliseconds
        // Serial.printf("Time(milliseconds): %g\n", result2);



        Serial.println("Setting up Advertisment");

        // Serial.print("\npAdvertising = BLEDevice::getAdvertising();");
        // float start3 = micros();
        // float end3;

        pAdvertising = BLEDevice::getAdvertising();

        // end3 = micros();
        // float result3 = end3 - start3;
        // result3 /= 1000; //convert to milliseconds
        // Serial.printf("Time(milliseconds): %g\n", result3);

        // Serial.print("\npAdvertising->addServiceUUID(serviceUUID);");
        // float start4 = micros();
        // float end4;

        pAdvertising->addServiceUUID(serviceUUID);

        // end4 = micros();
        // float result4 = end4 - start4;
        // result4 /= 1000; //convert to milliseconds
        // Serial.printf("Time(milliseconds): %g\n", result4);

        // Serial.print("\npAdvertising->setAdvertisementData(oAdvertisementData);");
        // float start5 = micros();
        // float end5;

        pAdvertising->setAdvertisementData(oAdvertisementData);

        // end5 = micros();
        // float result5 = end5 - start5;
        // result5 /= 1000; //convert to milliseconds
        // Serial.printf("Time(milliseconds): %g\n", result5);

        // Serial.print("\npAdvertising->setMinPreferred(0x06);");
        // float start6 = micros();
        // float end6;

        pAdvertising->setMinPreferred(0x06);

        // end6 = micros();
        // float result6 = end6 - start6;
        // result6 /= 1000; //convert to milliseconds
        // Serial.printf("Time(milliseconds): %g\n", result6);

        // Serial.print("\npAdvertising->setMinPreferred(0x12);");
        // float start7 = micros();
        // float end7;

        pAdvertising->setMinPreferred(0x12);

        // end7 = micros();
        // float result7 = end7 - start7;
        // result7 /= 1000; //convert to milliseconds
        // Serial.printf("Time(milliseconds): %g\n", result7);

        // Serial.print("\npAdvertising->start();");
        // float start8 = micros();
        // float end8;

        pAdvertising->start();

        // end8 = micros();
        // float result8 = end8 - start8;
        // result8 /= 1000; //convert to milliseconds
        // Serial.printf("Time(milliseconds): %g\n", result8);

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

std::multimap<std::string, time_t> *getRecentEncounters()
{
    return &recentEncounterMap;
}