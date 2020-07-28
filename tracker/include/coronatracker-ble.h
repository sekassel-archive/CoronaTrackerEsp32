#include <BLEDevice.h>
#include <Arduino.h>

void initBLE();
void deinitBLE();
void scanForCovidDevices(uint32_t duration);
std::multimap<std::string, time_t> *getRecentEncounters();