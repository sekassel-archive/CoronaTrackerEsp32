#include <string>
#include <SPIFFS.h>

#define ENCOUNTERS_PATH "/encounters.txt"

bool fileContainsString(std::string str, const char *path = ENCOUNTERS_PATH);