#include <string>
#include <map>
#include <SPIFFS.h>
#include <sqlite3.h>
#include <sstream>

#define ENCOUNTERS_PATH "/encounters.txt"

#define TEMPORARY_EXPOSURE_KEY_DATABASE_PATH "/tek.db"
#define TEK_DATABASE_SQLITE_PATH "/spiffs/tek.db"

#define MAIN_DATABASE_PATH "/main.db"
#define MAIN_DATABSE_SQLITE_PATH "/spiffs/main.db"

bool fileContainsString(std::string str, const char *path);
bool createFile(const char* path);
bool initSPIFFS(bool createEncountersFile, bool createDataBases);
bool writeIDtoFile(std::string id, const char *path);
bool insertTemporaryExposureKeyIntoDatabase(signed char *tek, size_t tek_length, int enin);
bool insertBluetoothDataIntoDataBase(time_t time, signed char *data, int data_size);