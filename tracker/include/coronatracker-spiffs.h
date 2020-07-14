#include <string>
#include <map>
#include <SPIFFS.h>

#define ENCOUNTERS_PATH "/encounters.txt"

bool fileContainsString(std::string str, const char *path = ENCOUNTERS_PATH);
bool initSPIFFS();
bool writeIDtoFile(std::string id, const char *path = ENCOUNTERS_PATH);