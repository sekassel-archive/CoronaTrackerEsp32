#include <BLEDevice.h>
#include <Arduino.h>

#include "coronatracker-crypto.h"
#include "coronatracker-spiffs.h"

bool initBLE(bool initScan, bool initAdvertisment);
void deinitBLE(bool releaseMemory);
std::vector<std::string> scanForCovidDevices(uint32_t duration);
bool generateNewTemporaryExposureKey(int ENIntervalNumber);