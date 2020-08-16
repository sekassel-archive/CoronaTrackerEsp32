#include "coronatracker-spiffs.h"

bool initSPIFFS(bool createEncountersFile, bool createRecentEncountersFile)
{
    if (!SPIFFS.begin(true))
    {
        Serial.println("Initializing failed");
        return false;
    }

    //Remove comment to reset file
    //SPIFFS.remove(ENCOUNTERS_PATH);

    if (createEncountersFile && !SPIFFS.exists(ENCOUNTERS_PATH))
    {
        Serial.println("Creating encounter file");
        File file = SPIFFS.open(ENCOUNTERS_PATH);

        if (!file)
        {
            Serial.println("There was an error creating the encounter file");
            return false;
        }
        file.close();
    }

    if(createRecentEncountersFile && !SPIFFS.exists(RECENTENCOUNTERS_PATH)) {
        Serial.println("Creating recentencounter file");
        File file = SPIFFS.open(RECENTENCOUNTERS_PATH);

        if (!file)
        {
            Serial.println("There was an error creating the recentencounter file");
            return false;
        }
        file.close();
    }

    return true;
}

bool fileContainsString(std::string str, const char *path)
{
    int index = 0;
    int len = str.length();

    if (len == 0)
    {
        return false;
    }

    File file = SPIFFS.open(path, FILE_READ);
    if (!file)
    {
        return false;
    }

    while (file.available())
    {
        char c = file.read();
        
        if (c != str[index]){
            index = 0;
        }

        if (c == str[index])
        {
            if (++index >= len)
            {
                file.close();
                return true;
            }
        }
    }
    file.close();
    return false;
}

bool writeIDtoFile(std::string id, const char *path)
{
    File file = SPIFFS.open(path, FILE_APPEND);
    if (!file)
    {
        return false;
    }
    bool success = file.print(id.c_str());
    file.close();
    return success;
}