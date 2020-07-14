#include "coronatracker-wifi.h"

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

std::pair<bool, std::vector<long>> requestInfections()
{
    Serial.println("Requesting infections from server.");

    std::vector<long> infectionVector;

    if (!connectToStoredWifi())
    {
        Serial.println("Could not Connect to Wifi - Retrying later");
        return std::make_pair(false, infectionVector);;
    }
    else
    {
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
                    if (pair.value().is<JsonArray>())
                    {
                        JsonArray array = pair.value().as<JsonArray>();
                        for (JsonVariant elem : array)
                        {
                            if (elem.is<long>())
                            {
                                infectionVector.push_back(elem);
                            }
                        }
                    }
                }
            }
            else
            {
                Serial.printf("deserializeJson() failed with code %s\n", err.c_str());
                return std::make_pair(false, infectionVector);;
            }
        }
        else
        {
            Serial.println("Error on HTTP request");
            return std::make_pair(false, infectionVector);;
        }
        http.end();
        disconnectWifi();

        return std::make_pair(true, infectionVector);
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