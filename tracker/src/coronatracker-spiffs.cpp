#include "coronatracker-spiffs.h"
#include "coronatracker-crypto.h"

const char *dataf = "Callback function called";
static int callback(void *data, int argc, char **argv, char **azColName)
{
    int i;
    Serial.println("------");
    for (i = 0; i < argc; i++)
    {
        if (strcmp(azColName[i], "time") == 0 || strcmp(azColName[i], "enin") == 0 || strcmp(azColName[i], "entry") == 0)
        {
            Serial.printf("%s = %s\n", azColName[i], argv[i] ? argv[i] : "NULL");
        }
        else
        {
            Serial.printf("%s = %02hhX %02hhX %02hhX %02hhX %02hhX %02hhX %02hhX %02hhX %02hhX %02hhX %02hhX %02hhX %02hhX %02hhX %02hhX %02hhX\n", azColName[i],
                          argv[i][0], argv[i][1], argv[i][2], argv[i][3], argv[i][4],
                          argv[i][5], argv[i][6], argv[i][7], argv[i][8], argv[i][9],
                          argv[i][10], argv[i][11], argv[i][12], argv[i][13], argv[i][14],
                          argv[i][15]);
        }
    }
    Serial.println("------");
    return 0;
}

int printSQLResult(sqlite3 *db, const char *sql)
{
    char *zErrMsg = 0;
    int rc = sqlite3_exec(db, sql, callback, (void *)dataf, &zErrMsg);
    if (rc != SQLITE_OK)
    {
        Serial.printf("SQL error: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
    }
    return rc;
}

bool printDatabases()
{
    sqlite3 *main_db;
    if (sqlite3_open(MAIN_DATABASE_SQLITE_PATH, &main_db) != SQLITE_OK)
    {
        Serial.printf("ERROR opening database: %s\n", sqlite3_errmsg(main_db));
        return false;
    }
    Serial.println("_________Temporary Rolling Proximity Identifiers___________");
    printSQLResult(main_db, "SELECT * FROM temp");
    Serial.println("___________________________________________________________");
    Serial.println("____________Saved Rolling Proximity Identifiers____________");
    printSQLResult(main_db, "SELECT * FROM main");
    Serial.println("___________________________________________________________");
    sqlite3_close(main_db);

    sqlite3 *tek_db;

    if (sqlite3_open(TEK_DATABASE_SQLITE_PATH, &tek_db) != SQLITE_OK)
    {
        Serial.println("Error on opening database");
        return false;
    }

    Serial.println("_________________Temporary Exposure Keys___________________");
    printSQLResult(tek_db, "SELECT * FROM tek");
    Serial.println("___________________________________________________________");
    sqlite3_close(tek_db);

    return true;
}

bool initSPIFFS()
{
    if (!SPIFFS.begin(true))
    {
        Serial.println("Initializing failed");
        return false;
    }
    // remove comment to reset SPIFFS
    //SPIFFS.format();
    //return false;
    return true;
}

bool initSpiffsCreateDataBases()
{
    initSPIFFS();
    createFile(TEMPORARY_EXPOSURE_KEY_DATABASE_PATH);

    sqlite3 *db;
    if (sqlite3_open(TEK_DATABASE_SQLITE_PATH, &db))
    {
        return false;
    }

    char *errMsg;
    if (sqlite3_exec(db, "CREATE TABLE IF NOT EXISTS tek (tek BLOB, enin INTEGER);", NULL, NULL, &errMsg) != SQLITE_OK)
    {
        Serial.printf("Failed to create table tek in datbase: %s\n", errMsg);
        sqlite3_close(db);
        return false;
    }
    sqlite3_free(errMsg);
    sqlite3_close(db);

    createFile(MAIN_DATABASE_PATH);

    if (sqlite3_open(MAIN_DATABASE_SQLITE_PATH, &db))
    {
        return false;
    }

    // UNIQUE Keyword does not work currently (https://github.com/siara-cc/esp32_arduino_sqlite3_lib/issues/18)
    if (sqlite3_exec(db, "CREATE TABLE IF NOT EXISTS main (time INTEGER, bl_data BLOB);", NULL, NULL, &errMsg) != SQLITE_OK)
    {
        Serial.printf("Failed to create table main in datbase: %s\n", errMsg);
        sqlite3_close(db);
        return false;
    }

    if (sqlite3_exec(db, "CREATE TABLE IF NOT EXISTS temp (time INTEGER, bl_data BLOB);", NULL, NULL, &errMsg) != SQLITE_OK)
    {
        Serial.printf("Failed to create table temp in datbase: %s\n", errMsg);
        sqlite3_close(db);
        return false;
    }

    sqlite3_free(errMsg);
    sqlite3_close(db);
    return true;
}

bool createFile(const char *path)
{
    Serial.printf("Creating File %s.. ", path);
    if (!SPIFFS.exists(path))
    {
        File file = SPIFFS.open(path);

        if (!file)
        {
            Serial.println("Failed");
            return false;
        }
        file.close();
        Serial.println("Created.");
        return true;
    }
    Serial.println("Already exists.");
    return true;
}

bool readUuid(char *uuidstr)
{
    // if no uuid file exists it will be generated and it will be readable
    if (!SPIFFS.begin(true) && createFile(UUID_FILE_PATH))
    {
        Serial.println("SPIFFS initialize for UUID failed!");
        return false;
    }

    File file = SPIFFS.open(UUID_FILE_PATH, FILE_READ);
    if (!file)
    {
        Serial.println("There was an error opening the UUID file for reading!");
        return false;
    }

    // read content from uuid file
    // string should only be empty if there where never an uuid assigned to this chip
    // uuid should be assigned only once to the same physical chip
    std::stringstream uuid_ss;
    for (int i = 0; file.available(); i++)
    {
        uuid_ss << file.read();
    }
    file.close();

    // uuid have to be 36 character long, server only accept 36 character long uuids
    if (uuid_ss.str().size() == 36)
    {
        strcpy(uuidstr, uuid_ss.str().c_str());
        return true;
    }
    else
    {
        Serial.println("UUID string size != 36 character! Failed to load UUID.");
        return false;
    }
}

bool writeUuid(char *uuidstr)
{
    // if no uuid file exists it will be generated and it will be readable
    if (!SPIFFS.begin(true) && createFile(UUID_FILE_PATH))
    {
        Serial.println("SPIFFS initialize for UUID failed!");
        return false;
    }

    // save new uuid to file
    File file = SPIFFS.open(UUID_FILE_PATH, FILE_WRITE);
    if (!file)
    {
        Serial.println("There was an error opening the UUID file to write new UUID into file!");
        return false;
    }
    file.write((byte *)&uuidstr, sizeof(uuidstr));
    file.close();
    return true;
}

bool insertRPI(time_t time, signed char *data, int data_size, bool intoMain, sqlite3 *main_db)
{
    sqlite3_stmt *res;
    const char *tail;

    std::stringstream sql_ss;
    sql_ss << "INSERT INTO ";
    if (intoMain)
    {
        sql_ss << "main";
    }
    else
    {
        sql_ss << "temp";
    }
    sql_ss << " VALUES (";
    sql_ss << time;
    sql_ss << ",?);";

    const char *sql = sql_ss.str().c_str();
    Serial.print("Inserting Rolling Proximity Identifier into Database: ");
    Serial.println(sql);

    if (sqlite3_prepare_v2(main_db, sql, strlen(sql), &res, &tail) != SQLITE_OK)
    {
        Serial.printf("ERROR preparing sql: %s\n", sqlite3_errmsg(main_db));
        return false;
    }

    if (sqlite3_bind_blob(res, 1, data, data_size, SQLITE_STATIC) != SQLITE_OK)
    {
        Serial.printf("ERROR binding blob: %s\n", sqlite3_errmsg(main_db));
        return false;
    }

    if (sqlite3_step(res) != SQLITE_DONE)
    {
        Serial.printf("ERROR inserting data: %s\n", sqlite3_errmsg(main_db));
        return false;
    }

    if (sqlite3_finalize(res) != SQLITE_OK)
    {
        Serial.printf("ERROR finalizing data: %s\n", sqlite3_errmsg(main_db));
        return false;
    }

    return true;
}

bool insertTemporaryExposureKeyIntoDatabase(signed char *tek, size_t tek_length, int enin)
{
    Serial.println("Inserting Temporary Exposure Key into Database");
    sqlite3 *tek_db;
    if (sqlite3_open(TEK_DATABASE_SQLITE_PATH, &tek_db) != SQLITE_OK)
    {
        Serial.println("Could not open database");
        return false;
    }

    sqlite3_stmt *res;
    const char *tail;

    const char *sql_command = "INSERT INTO tek VALUES (?,?);";

    if (sqlite3_prepare_v2(tek_db, sql_command, strlen(sql_command), &res, &tail) != SQLITE_OK)
    {
        Serial.printf("ERROR preparing sql(%s) : %s\n", sql_command, sqlite3_errmsg(tek_db));
        sqlite3_close(tek_db);
        return false;
    }

    if (sqlite3_bind_blob(res, 1, tek, tek_length, SQLITE_STATIC) != SQLITE_OK)
    {
        Serial.printf("ERROR binding blob(%s): %s\n", tek, sqlite3_errmsg(tek_db));
        sqlite3_close(tek_db);
        return false;
    }

    if (sqlite3_bind_int(res, 2, enin) != SQLITE_OK)
    {
        Serial.printf("ERROR binding int(%d): %s\n", enin, sqlite3_errmsg(tek_db));
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

    std::stringstream delete_sql;
    delete_sql << "DELETE FROM tek WHERE enin <";
    delete_sql << enin - (144 * 14); //Two Weeks
    delete_sql << ";";

    char *zErrMsg;
    if (sqlite3_exec(tek_db, delete_sql.str().c_str(), NULL, NULL, &zErrMsg) != SQLITE_OK)
    {
        Serial.printf("SQL error on delete: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
        return false;
    }

    sqlite3_free(zErrMsg);
    sqlite3_close(tek_db);

    return true;
}

bool getCurrentTek(signed char *tek, int *enin)
{
    sqlite3 *tek_db;
    sqlite3_stmt *res;

    if (sqlite3_open(TEK_DATABASE_SQLITE_PATH, &tek_db) != SQLITE_OK)
    {
        Serial.println("Error on opening database");
        return false;
    }

    const char *sql = "SELECT tek, enin FROM tek WHERE enin=(SELECT MAX(enin) FROM tek)";

    if (sqlite3_prepare_v2(tek_db, sql, strlen(sql), &res, nullptr) != SQLITE_OK)
    {
        Serial.printf("ERROR preparing sql: %s\n", sqlite3_errmsg(tek_db));
        return false;
    }

    if (sqlite3_step(res) != SQLITE_ROW)
    {
        Serial.printf("ERROR stepping: %s\n", sqlite3_errmsg(tek_db));
        return false;
    }
    const signed char *data = (const signed char *)sqlite3_column_blob(res, 0);

    for (int i = 0; i < sqlite3_column_bytes(res, 0); i++)
    {
        tek[i] = data[i];
    }

    int x = sqlite3_column_int(res, 1);
    *enin = x;

    sqlite3_finalize(res);
    sqlite3_close(tek_db);

    return true;
}

bool insertTemporaryRollingProximityIdentifiers(time_t time, std::vector<std::__cxx11::string> rpis)
{
    sqlite3 *db;
    if (sqlite3_open(MAIN_DATABASE_SQLITE_PATH, &db) != SQLITE_OK)
    {
        Serial.printf("ERROR opening database: %s\n", sqlite3_errmsg(db));
        sqlite3_close(db);
        return false;
    };

    for (auto rpi : rpis)
    {
        const char *serviceData = rpi.c_str();
        int size = rpi.length();
        signed char data[size];
        for (int i = 0; i < size; i++)
        {
            data[i] = serviceData[i];
        }

        if (!insertRPI(time, data, size, false, db))
        {
            sqlite3_close(db);
            return false;
        }
    }

    sqlite3_close(db);
    return true;
}

static int mainInsertCallback(void *data, int argc, char **argv, char **azColName)
{
    time_t current_time;
    time(&current_time);

    for (int i = 0; i < argc; i++)
    {
        insertRPI(current_time, (signed char *)argv[i], 16, true, (sqlite3 *)data);

        //Work around for only unique values (at least 6 entries get deleted - Only 10 will ever be seen if scanned every minute)
        sqlite3_stmt *res;
        const char *tail;

        const char *sql = "DELETE FROM temp WHERE bl_data = ?";

        if (sqlite3_prepare_v2((sqlite3 *)data, sql, strlen(sql), &res, &tail) != SQLITE_OK)
        {
            Serial.printf("ERROR preparing sql: %s\n", sqlite3_errmsg((sqlite3 *)data));
        }
        else
        {
            if (sqlite3_bind_blob(res, 1, (signed char *)argv[i], 16, SQLITE_STATIC) != SQLITE_OK)
            {
                Serial.printf("ERROR binding blob: %s\n", sqlite3_errmsg((sqlite3 *)data));
            }
            else
            {
                if (sqlite3_step(res) != SQLITE_DONE)
                {
                    Serial.printf("ERROR inserting data: %s\n", sqlite3_errmsg((sqlite3 *)data));
                }
                else
                {
                    Serial.println("Delete entries from temp");
                }
            }
        }
        sqlite3_finalize(res);
    }

    return 0;
}

bool cleanUpTempDatabase()
{
    Serial.println("Cleaning up databases");
    sqlite3 *main_db;
    char *zErrMsg;

    if (sqlite3_open(MAIN_DATABASE_SQLITE_PATH, &main_db) != SQLITE_OK)
    {
        Serial.printf("ERROR opening database: %s\n", sqlite3_errmsg(main_db));
        return false;
    }

    //Move exposures to main if necessary
    if (sqlite3_exec(main_db, "SELECT DISTINCT bl_data FROM temp GROUP BY bl_data HAVING COUNT(bl_data) > 5", mainInsertCallback, (void *)main_db, &zErrMsg) != SQLITE_OK)
    {
        Serial.printf("ERROR on retrieving exposed rpis: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
        return false;
    }

    //Delete old entries
    std::stringstream delete_sql_temp;
    delete_sql_temp << "DELETE FROM temp WHERE time <";
    delete_sql_temp << time(NULL) - (15 * 60); //15 Minutes
    delete_sql_temp << ";";

    if (sqlite3_exec(main_db, delete_sql_temp.str().c_str(), NULL, NULL, &zErrMsg) != SQLITE_OK)
    {
        Serial.printf("SQL error on delete: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
        return false;
    }

    //Delete old entries from main
    std::stringstream delete_sql_main;
    delete_sql_main << "DELETE FROM main WHERE time <";
    delete_sql_main << time(NULL) - (60 * 60 * 24 * 7 * 2); //2 Weeks
    delete_sql_main << ";";

    if (sqlite3_exec(main_db, delete_sql_main.str().c_str(), NULL, NULL, &zErrMsg) != SQLITE_OK)
    {
        Serial.printf("SQL error on delete: %s\n", zErrMsg);
        sqlite3_free(zErrMsg);
        return false;
    }

    sqlite3_close(main_db);
    return true;
}

//Return how many of the keys are in database from the given keys
int checkForKeysInDatabase(sqlite3 *db, signed char keys[][16], int key_amount, int key_length)
{
    sqlite3_stmt *res;
    const char *sql;

    if (key_amount == 144)
    {
        sql = "SELECT COUNT(bl_data) FROM main WHERE bl_data IN ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    }
    else
    {
        std::stringstream sql_ss;
        sql_ss << "SELECT COUNT(bl_data) FROM main WHERE bl_data IN (";
        for (int i = 0; i < key_amount; i++)
        {
            if (i == key_amount - 1)
            {
                sql_ss << " ?";
                break;
            }
            sql_ss << " ?,";
        }
        sql_ss << ");";
        sql = sql_ss.str().c_str();
    }

    if (sqlite3_prepare_v2(db, sql, strlen(sql), &res, nullptr) != SQLITE_OK)
    {
        Serial.printf("ERROR preparing sql: %s\n", sqlite3_errmsg(db));
        sqlite3_finalize(res);
        return -1;
    }

    for (int i = 1; i <= key_amount; i++)
    {
        if (sqlite3_bind_blob(res, i, (signed char *)keys[i], key_length, SQLITE_STATIC) != SQLITE_OK)
        {
            Serial.printf("ERROR binding blob: %s\n", sqlite3_errmsg(db));
            sqlite3_finalize(res);
            return -1;
        }
    }

    if (sqlite3_step(res) != SQLITE_ROW)
    {
        Serial.printf("ERROR stepping: %s\n", sqlite3_errmsg(db));
        sqlite3_finalize(res);
        return -1;
    }
    int occ = sqlite3_column_int(res, 0);

    sqlite3_finalize(res);
    return occ;
}

ContactInformation *checkForCollectedContactInformationsInDatabase()
{
    // get actual enin to compare if we have not matching enins in our clected contact informations
    int currentENIN = calculateENIntervalNumber(time(NULL));
    sqlite3 *main_db;
    const char *sql;
    sqlite3_stmt *res;

    if (sqlite3_open(MAIN_DATABASE_SQLITE_PATH, &main_db) != SQLITE_OK)
    {
        Serial.printf("ERROR opening main database: %s\n", sqlite3_errmsg(main_db));
        return NULL;
    }

    //Serial.println("____________Saved Rolling Proximity Identifiers____________");
    //printSQLResult(main_db, "SELECT * FROM main");
    //time = 1610031333
    //bl_data = B4 89 B1 13 4A 74 32 8C 1C 3E 9B 32 DE 2F C7 3D

    sql = "SELECT * FROM main WHERE time CONTAINS \"?%\";";

    // TODO

    return NULL;
}
