#include <BLEDevice.h>
#include <Arduino.h>
#include <sqlite3.h>

bool initBLE(bool initScan, bool initAdvertisment);
void deinitBLE();
void scanForCovidDevices(uint32_t duration);
std::multimap<std::string, time_t> *getRecentEncounters();