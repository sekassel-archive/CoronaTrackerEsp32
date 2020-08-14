#include "coronatracker-spiffs.h"

const char *dataf = "Callback function called";
static int callback(void *data, int argc, char **argv, char **azColName)
{
    int i;
    Serial.printf("%s: \n", (const char *)dataf);
    for (i = 0; i < argc; i++)
    {
        Serial.printf("%s = %s\n", azColName[i], argv[i] ? argv[i] : "NULL");
    }
    Serial.printf("\n");
    return 0;
}

char *zErrMsg = 0;
int db_exec(sqlite3 *db, const char *sql)
{
    Serial.println(sql);
    long start = micros();
    int rc = sqlite3_exec(db, sql, callback, (void *)dataf, &zErrMsg);
    if (rc != SQLITE_OK)
    {
        Serial.printf("SQL error: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
    }
    else
    {
        Serial.printf("Operation done successfully\n");
    }
    Serial.print(F("Time taken:"));
    Serial.println(micros() - start);
    return rc;
}

bool initSPIFFS(bool createEncountersFile, bool createDataBases)
{
    if (!SPIFFS.begin(true))
    {
        Serial.println("Initializing failed");
        return false;
    }

    //Remove comment to reset SPIFFS
    //SPIFFS.format();

    if (createEncountersFile)
    {
        bool ret = createFile(ENCOUNTERS_PATH);
        if (ret == false)
        {
            return false;
        }
    }

    if (createDataBases)
    {
        if (!SPIFFS.exists(TEMPORARY_EXPOSURE_KEY_DATABASE_PATH))
        {
            bool ret = createFile(TEMPORARY_EXPOSURE_KEY_DATABASE_PATH);
            if (ret == false)
            {
                return false;
            }

            sqlite3 *tek_db;
            if (sqlite3_open(TEK_DATABASE_SQLITE_PATH, &tek_db))
            {
                return false;
            }

            char *errMsg;
            if (sqlite3_exec(tek_db, "CREATE TABLE IF NOT EXISTS tek (tek BLOB, enin INTEGER);", NULL, NULL, &errMsg) != SQLITE_OK)
            {
                Serial.printf("Failed to create table in datbase: %s\n", errMsg);
                sqlite3_close(tek_db);
                return false;
            }
            sqlite3_free(errMsg);
            sqlite3_close(tek_db);
        }

        if (!SPIFFS.exists(MAIN_DATABASE_PATH))
        {
            bool ret = createFile(MAIN_DATABASE_PATH);
            if (ret == false)
            {
                return false;
            }

            sqlite3 *tek_db;
            if (sqlite3_open(MAIN_DATABSE_SQLITE_PATH, &tek_db))
            {
                return false;
            }

            char *errMsg;
            if (sqlite3_exec(tek_db, "CREATE TABLE IF NOT EXISTS main (time INTEGER, bl_data BLOB);", NULL, NULL, &errMsg) != SQLITE_OK)
            {
                Serial.printf("Failed to create table in datbase: %s\n", errMsg);
                sqlite3_close(tek_db);
                return false;
            }
            sqlite3_free(errMsg);
            sqlite3_close(tek_db);
        }
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

//Inserts blob if one argument is parameter
bool insertBlobIntoDatabase(const char *db_path, const char *sql, int sql_length, signed char *blob, int blob_length)
{
    Serial.println("Inserting blob");
    Serial.println(sql);
    Serial.println(sql_length);

    Serial.println(db_path);

    //Serial.println((char*)blob);

    sqlite3 *db;
    sqlite3_stmt *res;
    const char *tail;

    if (sqlite3_open(db_path, &db))
    {
        Serial.printf("ERROR opening databse: %s\n", sqlite3_errmsg(db));
        sqlite3_close(db);
        return false;
    }

    int rc = sqlite3_prepare_v2(db, sql, sql_length, &res, &tail);
    Serial.println(rc);
    if (rc != SQLITE_OK)
    {
        Serial.printf("ERROR preparing sql: %s\n", sqlite3_errmsg(db));
        sqlite3_close(db);
        return false;
    }

    if (sqlite3_bind_blob(res, 1, blob, blob_length, SQLITE_STATIC) != SQLITE_OK)
    {
        Serial.printf("ERROR binding blob: %s\n", sqlite3_errmsg(db));
        sqlite3_close(db);
        return false;
    }

    if (sqlite3_step(res) != SQLITE_DONE)
    {
        Serial.printf("ERROR inserting data: %s\n", sqlite3_errmsg(db));
        sqlite3_close(db);
        return false;
    }

    if (sqlite3_finalize(res) != SQLITE_OK)
    {
        Serial.printf("ERROR finalizing data: %s\n", sqlite3_errmsg(db));
        sqlite3_close(db);
        return false;
    }


    db_exec(db, "SELECT * FROM tek");
    delay(10000);

    sqlite3_close(db);
    return true;
}

bool insertBluetoothDataIntoDataBase(time_t time, signed char *data, int data_size)
{
    std::stringstream sql_ss;
    sql_ss << "INSERT INTO main VALUES (";
    sql_ss << time;
    sql_ss << ",?);";

    const char *sql = sql_ss.str().c_str();

    return insertBlobIntoDatabase(MAIN_DATABSE_SQLITE_PATH, sql, strlen(sql), data, data_size);
}

bool insertTemporaryExposureKeyIntoDatabase(signed char *tek, size_t tek_length, int enin)
{
    Serial.println(ESP.getFreeHeap());
    std::stringstream sql_ss;
    sql_ss << "INSERT INTO tek VALUES (?,";
    sql_ss << enin;
    sql_ss << ");";

    const char *sql_command = sql_ss.str().c_str();

    Serial.println(sql_command);
    Serial.println(strlen(sql_command));

    for (int i = 0; i < 35; i++)
    {
        Serial.print(sql_command[i]);
        Serial.print(" ");
    }
    Serial.println();
    

    Serial.println(TEK_DATABASE_SQLITE_PATH);

    for (int i = 0; i < strlen(TEK_DATABASE_SQLITE_PATH); i++)
    {
        Serial.print(TEK_DATABASE_SQLITE_PATH[i]);
        Serial.print(" ");
    }
    Serial.println();

    return insertBlobIntoDatabase(TEK_DATABASE_SQLITE_PATH, sql_command, strlen(sql_command), tek, tek_length);
}