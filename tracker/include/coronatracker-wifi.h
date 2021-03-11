#include <Arduino.h>
#include <WiFiManager.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

#include "coronatracker-crypto.h"
#include "coronatracker-spiffs.h"

#define SERVER_URL "https://tracing.uniks.de/api"
#define RSIN_URL "/infections/rsin"

const size_t JSON_CAPACITY = JSON_ARRAY_SIZE(16);

typedef enum
{
    EXPOSURE_NO_DETECT = 0,
    EXPOSURE_DETECT = 1,
    EXPOSURE_UPDATE_FAILED = 2,
    EXPOSURE_NO_UPDATE = 3, //No update happened yet
} exposure_status;

bool disconnectWifi();
bool connectToStoredWifi();
bool configureWifi();
std::map<uint32_t, uint16_t> getRSINAsMap(bool connectToWifi);
exposure_status checkForInfections();