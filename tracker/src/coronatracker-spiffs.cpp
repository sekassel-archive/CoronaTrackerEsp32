#include "coronatracker-spiffs.h"

bool initSPIFFS(bool createEncountersFile, bool createTEKFile)
{
    if (!SPIFFS.begin(true))
    {
        Serial.println("Initializing failed");
        return false;
    }

    //Remove comment to reset file
    //SPIFFS.remove(ENCOUNTERS_PATH);

    if (createEncountersFile)
    {
        bool ret = createFile(ENCOUNTERS_PATH);
        if (ret == false)
        {
            return false;
        }
    }

    if (createTEKFile && !SPIFFS.exists(TEMPORARY_EXPOSURE_KEY_PATH))
    {
        bool ret = createFile(TEMPORARY_EXPOSURE_KEY_PATH);
        if (ret == false)
        {
            return false;
        }

        sqlite3 *tek_db;
        if (sqlite3_open(TEK_DB_PATH, &tek_db))
        {
            return false;
        }

        char *errMsg;
        if (sqlite3_exec(tek_db, "CREATE TABLE tek (tek BLOB, enin INTEGER);", NULL, NULL, &errMsg) != SQLITE_OK)
        {
            Serial.printf("Failed to create table in datbase: %s\n", errMsg);
            return false;
        }
        sqlite3_free(errMsg);
        sqlite3_close(tek_db);
    }
    return true;
}

bool createFile(const char *path)
{
    if (!SPIFFS.exists(path))
    {
        Serial.printf("Creating File %s\n", path);
        File file = SPIFFS.open(path);

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
        if (c != str[index])
        {
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

bool addTemporaryExposureKeyToDatabase(signed char *tek, size_t tek_length, int enin)
{
    sqlite3 *tek_db;
    if (sqlite3_open(TEK_DB_PATH, &tek_db))
    {
        return false;
    }

    sqlite3_stmt *res;
    const char *tail;

    std::stringstream sql;
    sql << "INSERT INTO tek VALUES (?,";
    sql << enin;
    sql << ");";

    const char *sql_command = sql.str().c_str();

    if (sqlite3_prepare_v2(tek_db, sql_command, strlen(sql_command), &res, &tail) != SQLITE_OK)
    {
        Serial.printf("ERROR preparing sql: %s\n", sqlite3_errmsg(tek_db));
        sqlite3_close(tek_db);
        return false;
    }
    if (sqlite3_bind_blob(res, 1, tek, tek_length, SQLITE_STATIC) != SQLITE_OK)
    {
        Serial.printf("ERROR binding blob: %s\n", sqlite3_errmsg(tek_db));
        sqlite3_close(tek_db);
        return false;
    }

    if (sqlite3_step(res) != SQLITE_DONE)
    {
        Serial.printf("ERROR inserting data: %s\n", sqlite3_errmsg(tek_db));
        sqlite3_close(tek_db);
        return false;
    }

    if (sqlite3_finalize(res) != SQLITE_OK)
    {
        Serial.printf("ERROR finalizing data: %s\n", sqlite3_errmsg(tek_db));
        sqlite3_close(tek_db);
        return false;
    }

    sqlite3_close(tek_db);

    return true;
}