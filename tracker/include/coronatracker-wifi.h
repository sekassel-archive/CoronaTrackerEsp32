#include <Arduino.h>
#include <WifiManager.h>
#include <HTTPClient.h>
#include <ArduinoJSON.h>

#define SERVER_URL "https://tracing.uniks.de"

bool disconnectWifi();
bool connectToStoredWifi();
std::pair<bool, std::vector<long>> requestInfections();
bool configureWifi();