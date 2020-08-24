#include <string>
#include <map>
#include <SPIFFS.h>
#include <sqlite3.h>
#include <sstream>

#define TEMPORARY_EXPOSURE_KEY_DATABASE_PATH "/tek.db"
#define TEK_DATABASE_SQLITE_PATH "/spiffs/tek.db"

#define MAIN_DATABASE_PATH "/main.db"
#define MAIN_DATABASE_SQLITE_PATH "/spiffs/main.db"

bool createFile(const char *path);
bool initSPIFFS(bool createDataBases);
bool insertTemporaryExposureKeyIntoDatabase(signed char *tek, size_t tek_length, int enin);
bool getCurrentTek(signed char *tek, int *enin);
bool cleanUpTempDatabase();
bool insertRollingProximityIdentifier(time_t time, signed char *data, int data_size, bool savePermanently);
int checkForKeysInDatabase(sqlite3 *db, signed char keys[][16], int key_amount, int key_length);
bool printDatabases();