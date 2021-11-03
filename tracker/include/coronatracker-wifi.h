#pragma once
#include "coronatracker-custom-typdefs.h"
#include <sstream>
#include <WiFiManager.h>
#include <HTTPClient.h>

#define SERVER_URL "https://tracing.uniks.de/api"
//#define SERVER_URL "http://192.168.2.2:4567/api"
#define GET_TEST_URL "/ping"
#define GET_NEW_UUID "/uuid"
#define GET_DAYLIGHT_OFFSET "/daylightoffset"
#define GET_TODAYS_RSIN "/todaysRsin"
#define POST_VERIFICATION "/verify"
#define POST_VERIFICATION_DATA "/verify/update"
#define POST_DATA_INPUT_RPIS "/data/input"
#define POST_DATA_INPUT_TEK "/data/input/tek/share"
#define POST_INFECTION_STATUS "/infection/status"

bool disconnectWifi(void);
bool connectToStoredWifi(void);
bool configureWifi(void);
int getDaylightOffset(void);
bool getNewUuid(std::string *uuidstr);
int getTodaysRsin(void);
exposure_status getInfectionStatus(std::string *uuidstr);
bool sendContactInformation(std::string *uuidstr, int enin, std::vector<std::string> *rpiData);
bool sendTekInformation(std::string *uuidstr, int enin, std::string *tekData);
bool sendPinForVerification(std::string *uuidstr, std::string *pin, std::string *timestamp);
std::string checkServerSuccessfullDataInput(std::string *uuidstr, std::string *pin, std::string *timestamp);