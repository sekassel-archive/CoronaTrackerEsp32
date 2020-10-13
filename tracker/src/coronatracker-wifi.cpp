#include "coronatracker-wifi.h"

using namespace websockets;

bool disconnectWifi()
{
    Serial.println("Deactivating Wifi");
    return WiFi.disconnect(true, false);
}

bool connectToStoredWifi()
{
    Serial.println("Trying to establish to Wifi-Connection.");
    WiFi.mode(WIFI_STA);
    WiFi.begin();
    return WiFi.waitForConnectResult() == WL_CONNECTED;
}

String lastMessage;
void onMessageCallback(WebsocketsMessage message)
{
    lastMessage = message.data();
}

std::map<uint32_t, uint16_t> getRSINAsMap(bool connectToWifi)
{
    std::map<uint32_t, uint16_t> rsinMap;
    if (connectToWifi && !connectToStoredWifi())
    {
        Serial.println("Could not Connect to Wifi");
        return rsinMap;
    }
    else
    {
        HTTPClient http;

        http.begin(String(SERVER_URL) + String(RSIN_URL));
        int code = http.GET();

        if (!(code == HTTP_CODE_OK))
        {
            Serial.println("Failed to connect to server");
            return rsinMap;
        }
        else
        {
            DynamicJsonDocument doc(960); //This allows 40 entries
            DeserializationError err = deserializeJson(doc, http.getString());
            http.end();

            if (err)
            {
                Serial.print(F("deserializeJson() failed with code "));
                Serial.println(err.c_str());
                return rsinMap;
            }
            else
            {
                JsonObject json = doc.as<JsonObject>();
                for (JsonPair pair : json)
                {
                    rsinMap.insert(std::make_pair(atoi(pair.key().c_str()), pair.value().as<unsigned short>()));
                }
            }
        }
    }
    return rsinMap;
}

bool checkForInfections()
{
    Serial.println("Connecting to server and checking for infections");
    if (!connectToStoredWifi())
    {
        Serial.println("Could not Connect to Wifi");
        return false;
    }
    else
    {
        Serial.println("Requesting infections numbers");
        auto rsinMap = getRSINAsMap(false);

        std::map<uint32_t, uint16_t> currentProgress = getCurrentProgress();
        if (currentProgress.empty())
        {
            Serial.println("Failed to read current progress");
            return false;
        }

        if (rsinMap.empty())
        {
            Serial.println("Failed to get rsins");
            return false;
        }
        else
        {
            WebsocketsClient client;
            client.onMessage(onMessageCallback);

            bool con = client.connect(WS_URL);

            if (!con)
            {
                Serial.println("Could not connect to websocket");
                return false;
            }
            else
            {
                sqlite3 *db;
                if (sqlite3_open(MAIN_DATABASE_SQLITE_PATH, &db) != SQLITE_OK)
                {
                    Serial.printf("ERROR opening database: %s\n", sqlite3_errmsg(db));
                    return false;
                }

                for (auto it = rsinMap.begin(); it != rsinMap.end(); it++)
                {
                    uint32_t rsin = it->first;
                    uint16_t values = it->second;
                    int i = 0;

                    Serial.printf("Starting to check rsin: %d\n", rsin);

                    //We only need to check keys, if we haven't checked them before
                    if (currentProgress.find(rsin) != currentProgress.end())
                    {
                        i = currentProgress.find(rsin)->second;
                    }

                    for (i; i < values; i++)
                    {
                        Serial.printf("Entry %d/%d\n", i, values);

                        String stringToSend = String(rsin);
                        stringToSend.concat(":");
                        stringToSend.concat(i);

                        client.send(stringToSend);

                        while (!client.poll())
                        {
                            delay(1);
                        }

                        if (lastMessage.equals("Not found") || lastMessage.equals("Wrong Input!"))
                        {
                            continue;
                        }

                        StaticJsonDocument<JSON_CAPACITY> doc;
                        deserializeJson(doc, lastMessage);

                        JsonArray byteArray = doc.as<JsonArray>();
                        signed char keyData[16];

                        int j = 0;
                        for (JsonVariant elem : byteArray)
                        {
                            keyData[j] = elem.as<int>();
                            j++;
                        }

                        signed char rpis[EKROLLING_PERIOD][16];
                        for (int j = 0; j < EKROLLING_PERIOD; j++)
                        {
                            calculateRollingProximityIdentifier((const unsigned char *)keyData, (rsin + j), (unsigned char *)rpis[j]);
                        }
                        int occ = checkForKeysInDatabase(db, rpis, EKROLLING_PERIOD, 16);

                        if (occ == -1)
                        {
                            Serial.println("There was an Error checking keys");

                            i--; //Temporary Workaround for Out Of Memory Error, will simply retry the current entry
                        }
                        else if (occ > 0)
                        {
                            client.close();
                            disconnectWifi();
                            sqlite3_close(db);
                            Serial.println("User was exposed to someone infected!");
                            return true;
                        }
                    }
                }
                sqlite3_close(db);
            }
            client.close();
        }
        insertCWAProgress(rsinMap);
    }
    disconnectWifi();
    return EXPOSURE_NO_DETECT;
}

bool configureWifi()
{
    WiFiManager wifiManager;
    wifiManager.setConfigPortalTimeout(300); //5 Minutes
    wifiManager.setConnectTimeout(30);       //30 Seconds
    bool result = wifiManager.startConfigPortal("Coronatracker");

    if (!result)
    {
        wifiManager.resetSettings();
    }
    return result;
}