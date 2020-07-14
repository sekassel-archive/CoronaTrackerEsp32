#include "coronatracker-wifi.h"
#include "coronatracker-file.h"

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

//TODO Give back Error if occuring
bool requestInfections()
{
    Serial.println("Requesting infections from server.");
    if (!connectToStoredWifi())
    {
        Serial.println("Could not Connect to Wifi - Retrying later");
        return false;
    }
    else
    {
        bool metInfected = false;
        HTTPClient http;

        http.begin(String(SERVER_URL) + "/infections");
        int httpCode = http.GET();

        Serial.print("ReturnCode: ");
        Serial.println(httpCode);
        if (httpCode > 0)
        {
            Serial.println("---------- Message ----------");
            String response = http.getString();
            Serial.println(response);
            Serial.println("-----------------------------");

            //TODO: Maybe calculate approximate size beforehand
            DynamicJsonDocument doc(2048);
            DeserializationError err = deserializeJson(doc, response);

            if (!err)
            {
                JsonObject responseJSON = doc.as<JsonObject>();
                for (JsonPair pair : responseJSON)
                {
                    Serial.print(pair.key().c_str());
                    Serial.print(" : ");
                    if (pair.value().is<JsonArray>())
                    {
                        JsonArray array = pair.value().as<JsonArray>();
                        for (JsonVariant elem : array)
                        {
                            if (elem.is<long>())
                            {
                                long l = elem;
                                Serial.printf("%ld ", l);

                                if (fileContainsString(String(l).c_str()))
                                {
                                    Serial.print("(I) ");
                                    metInfected = true;
                                }
                            }
                        }
                    }
                    Serial.println();
                }

                if (metInfected)
                {
                    Serial.println("User has met someone infected!");
                }
                else
                {
                    Serial.println("You are not infected.");
                }
            }
            else
            {
                Serial.printf("deserializeJson() failed with code %s\n", err.c_str());
            }
        }
        else
        {
            Serial.println("Error on HTTP request");
        }

        http.end();
        delay(1000);
        disconnectWifi();

        return metInfected;
    }
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