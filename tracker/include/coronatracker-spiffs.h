#pragma once
#include "coronatracker-custom-typdefs.h"
#include <string>
#include <map>
#include <SPIFFS.h>
#include <sqlite3.h>
#include <sstream>

#define TEMPORARY_EXPOSURE_KEY_DATABASE_PATH "/tek.db"
#define TEK_DATABASE_SQLITE_PATH "/spiffs/tek.db"

#define MAIN_DATABASE_PATH "/main.db"
#define MAIN_DATABASE_SQLITE_PATH "/spiffs/main.db"

#define UUID_FILE_PATH "/uuid.txt"

bool createFile(const char *path);
bool readUuid(char *uuidstr);
bool writeUuid(char *uuidstr);
bool initSPIFFS(void);
bool initSpiffsCreateDataBases(void);
bool insertTemporaryExposureKeyIntoDatabase(signed char *tek, size_t tek_length, int enin);
bool getCurrentTek(signed char *tek, int *enin);
bool cleanUpTempDatabase();
bool insertTemporaryRollingProximityIdentifiers(time_t time, std::vector<std::__cxx11::string> rpis);
int checkForKeysInDatabase(sqlite3 *db, signed char keys[][16], int key_amount, int key_length);
bool printDatabases();
bool checkForCollectedContactInformationsInDatabase(std::map<int, std::vector<char*>> *contactInformationMap);
void deleteCollectedContactInformationsSendedToServerFromDb(std::map<int, std::vector<char*>> *contactInformationMap);