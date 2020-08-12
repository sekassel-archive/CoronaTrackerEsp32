#include <string>
#include <map>
#include <SPIFFS.h>
#include <sqlite3.h>
#include <sstream>

#define ENCOUNTERS_PATH "/encounters.txt"
#define TEMPORARY_EXPOSURE_KEY_PATH "/tek.db"
#define TEK_DB_PATH "/spiffs/tek.db"

bool fileContainsString(std::string str, const char *path);
bool createFile(const char* path);
bool initSPIFFS(bool createEncountersFile, bool createTEKFile);
bool writeIDtoFile(std::string id, const char *path);
bool addTemporaryExposureKeyToDatabase(signed char *tek, size_t tek_length, int enin);