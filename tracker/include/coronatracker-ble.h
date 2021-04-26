#pragma once
#include "coronatracker-crypto.h"
#include "coronatracker-spiffs.h"
#include <Arduino.h>
#include <BLEDevice.h>

void initializeBluetoothForScan();
bool initializeBluetoothForAdvertisment();
void deinitBLE(bool releaseMemory);
std::vector<std::string> scanForCovidDevices(uint32_t duration);
bool generateNewTemporaryExposureKey(int ENIntervalNumber);