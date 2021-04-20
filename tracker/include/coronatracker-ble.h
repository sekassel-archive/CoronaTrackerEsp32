#include <BLEDevice.h>
#include <Arduino.h>

#include "coronatracker-crypto.h"
#include "coronatracker-spiffs.h"

void initializeBluetoothForScan();
bool initializeBluetoothForAdvertisment();
void deinitBLE(bool releaseMemory);
std::vector<std::string> scanForCovidDevices(uint32_t duration);
bool generateNewTemporaryExposureKey(int ENIntervalNumber);