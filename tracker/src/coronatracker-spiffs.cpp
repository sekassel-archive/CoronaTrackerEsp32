#include "coronatracker-spiffs.h"

bool initSPIFFS(bool createEncountersFile)
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
        Serial.println("Creating File");
        File file = SPIFFS.open(ENCOUNTERS_PATH);

        if (!file)
        {
            Serial.println("There was an error creating the file");
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

    File file = SPIFFS.open(path, FILE_READ);
    if (!file)
    {
        return false;
    }

    if (len == 0)
    {
        return false;
    }

    while (file.available())
    {
        char c = file.read();
        if (c != str[index])
        {
            index = 0;
        }

        if (c == str[index])
        {
            if (++index >= len)
            {
                return true;
            }
        }
    }
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