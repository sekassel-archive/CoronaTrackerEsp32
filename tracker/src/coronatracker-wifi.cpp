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

void checkForInfections() {
    Serial.println("Connecting to server and checking for infections");
    if (!connectToStoredWifi())
    {
        Serial.println("Could not Connect to Wifi");
        return;
    }
    else
    {
        Serial.println("Requesting infections numbers");
        HTTPClient http;

        http.begin("http://192.168.178.59:4567/infections/rsin");
        int code = http.GET();

        if (!(code == HTTP_CODE_OK))
        {
            Serial.println("Failed to connect to server");
            return;
        }
        else
        {
            DynamicJsonDocument doc(1024);
            DeserializationError err = deserializeJson(doc, http.getString());

            if (err)
            {
                Serial.print(F("deserializeJson() failed with code "));
                Serial.println(err.c_str());
                return;
            }
            else
            {
                WebsocketsClient client;

                bool con = client.connect("ws://192.168.178.59:4567/cwa");

                if (!con)
                {
                    Serial.println("Could not connect to websocket");
                    return;
                }
                else
                {
                    client.onMessage([&](WebsocketsMessage message) {
                        Serial.print("Got Message: ");
                        Serial.println(message.data());
                        //TODO Handle Message

                        });

                    JsonObject json = doc.as<JsonObject>();
                    for (JsonPair pair : json)
                    {
                        Serial.printf("Starting to poll %s", pair.key().c_str());

                        int values = pair.value().as<int>();

                        for (int i = 0; i < values; i++)
                        {
                            String stringToSend = pair.key().c_str();
                            stringToSend.concat(":");
                            stringToSend.concat(i);

                            client.send(stringToSend);

                            while (!client.poll())
                            {
                                delay(1);
                            }
                        }
                    }
                    client.close();
                }
            }
        }
        http.end();
    }
    disconnectWifi();
}


//Deprecated
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