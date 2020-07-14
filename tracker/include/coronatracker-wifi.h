#include <Arduino.h>
#include <WifiManager.h>
#include <HTTPClient.h>
#include <ArduinoJSON.h>

#define SERVER_URL "https://tracing.uniks.de"

bool disconnectWifi();
bool connectToStoredWifi();
bool requestInfections();
bool configureWifi();