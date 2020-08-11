#include <BLEDevice.h>
#include <Arduino.h>

#include "coronatracker-crypto.h"

bool initBLE(bool initScan, bool initAdvertisment);
void deinitBLE();
void scanForCovidDevices(uint32_t duration);
std::multimap<std::string, time_t> *getRecentEncounters();