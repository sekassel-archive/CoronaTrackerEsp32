#include <BLEDevice.h>
#include <Arduino.h>

#include "coronatracker-crypto.h"
#include "coronatracker-spiffs.h"

bool initBLE(bool initScan, bool initAdvertisment);
void deinitBLE();
void scanForCovidDevices(uint32_t duration);
bool generateNewTEK();