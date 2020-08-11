#include <Arduino.h>
#include <WifiManager.h>
#include <HTTPClient.h>
#include <ArduinoJSON.h>
#include <ArduinoWebsockets.h>

#include "coronatracker-crypto.h"

#define SERVER_URL "https://tracing.uniks.de"
#define RSIN_URL "/infections/rsin"
#define WS_URL "ws://tracing.uniks.de/cwa"

#define ROLLING_PERIOD 144

const size_t JSON_CAPACITY = JSON_ARRAY_SIZE(16);

bool disconnectWifi();
bool connectToStoredWifi();
std::pair<bool, std::vector<long>> requestInfections();
bool configureWifi();
void checkForInfections();