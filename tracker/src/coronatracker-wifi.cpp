#include "coronatracker-wifi.h"

bool disconnectWifi()
{
    Serial.println("Deactivating Wifi");
    return WiFi.disconnect(true, false);
}

bool connectToStoredWifi()
{
    // Try to establish to Wifi-Connection
    WiFi.mode(WIFI_STA);
    WiFi.begin();
    return WiFi.waitForConnectResult() == WL_CONNECTED;
}

bool configureWifi()
{
    WiFiManager wifiManager;
    wifiManager.setConfigPortalTimeout(300); //5 Minutes
    wifiManager.setConnectTimeout(30);       //30 Seconds

    int buttonState = digitalRead(0); // Button Pin checked if Pressed == 0
    bool result;

    // connects to wifi automatically but if boot pressed while startup
    // it will open hotspot for configuration
    if (buttonState != 0)
    {
        result = wifiManager.autoConnect("Coronatracker", NULL);
    }
    else
    {
        result = wifiManager.startConfigPortal("Coronatracker", NULL);
    }

    if (!result)
    {
        wifiManager.resetSettings();
    }
    return result;
}

bool getNewUuid(std::string *uuidstr)
{
    if (!WiFi.isConnected() && !connectToStoredWifi())
    {
        Serial.println("Could not Connect to Wifi");
        return false;
    }
    else
    {
        HTTPClient http;

        http.begin(String(SERVER_URL) + String(GET_NEW_UUID));
        int code = http.GET();

        if (!(code == HTTP_CODE_OK))
        {
            Serial.println("Failed to connect to server!");
            http.end();
            return false;
        }
        else
        {
            String uuid = http.getString();
            http.end();

            if (uuid.length() == 36)
            {
                (*uuidstr) = (std::string)uuid.c_str();
            }
            else
            {
                Serial.println("UUID from Server != 36 character! Invalid UUID length.");
                Serial.print("Recieved string: ");
                Serial.println(uuid);
                return false;
            }
        }
    }
    return true;
}

exposure_status getInfectionStatus(std::string *uuidstr)
{
    if (!connectToStoredWifi())
    {
        Serial.println("Get infection status failed: couldn't connect to Wifi!");
        return EXPOSURE_UPDATE_FAILED;
    }

    HTTPClient http;

    // Your Domain name with URL path or IP address with path
    http.begin(String(SERVER_URL) + String(POST_INFECTION_STATUS));

    // Specify content-type header
    http.addHeader("Content-Type", "application/json");

    // Data to send with HTTP POST
    std::stringstream stringStream;
    stringStream << "{\"uuid\":\"" << uuidstr->c_str() << "\"}";

    // Send HTTP POST request
    int httpResponseCode = http.POST(stringStream.str().c_str());
    String body = http.getString();
    disconnectWifi();

    if (httpResponseCode != HTTP_CODE_OK)
    {
        Serial.print("HTTP Response Code not 200! Body: ");
        Serial.println(body);
        return EXPOSURE_UPDATE_FAILED;
    }
    else
    {
        if (body.equals("Infected"))
        {
            Serial.print("HTTP Response Code 200: User is: ");
            Serial.println(body);
            return EXPOSURE_DETECT;
        }
        // otherwise it is "Unknown" which means the user is not infected
        // "Unknown" does not mean that an error happened or the user wasn't found
        // it should prevent data discovery which uuid is present in server db
    }

    return EXPOSURE_NO_DETECT;
}

bool sendContactInformation(std::string *uuidstr, int enin, std::vector<std::string> *rpiData)
{
    Serial.print("sendContactInformation(uuidstr: ");
    Serial.print(uuidstr->c_str());
    Serial.printf(", enin: %i, rpiData: ", enin);
    std::vector<std::string>::iterator it2 = rpiData->begin();
    while (it2 != rpiData->end())
    {
        Serial.printf(it2->c_str());
        it2++;
        if (it2 != rpiData->end())
            Serial.printf(",");
    }

    if (!connectToStoredWifi())
    {
        Serial.println("Get infection status failed: Couldn't connect to Wifi!");
        return false;
    }

    HTTPClient http;

    // Your Domain name with URL path or IP address with path
    http.begin(String(SERVER_URL) + String(POST_DATA_INPUT_RPIS));

    // Specify content-type header: application/json
    http.addHeader("Content-Type", "application/json");

    // Data to send with HTTP POST
    std::stringstream ss;
    std::vector<std::string>::iterator rpiListIter = rpiData->begin();

    ss << "{\"uuid\":\"" << uuidstr->c_str() << "\",\"status\":\"0\",\"enin\":\"" << enin << "\",\"rpiList\":\"[";
    while (rpiListIter != rpiData->end())
    {
        ss << rpiListIter->c_str();
        rpiListIter++;
        if (rpiListIter != rpiData->end())
        {
            ss << ",";
        }
    }
    ss << "]\"}";

    // TODO: rpiData to JSON Array into String
    Serial.printf("http Post Payload: %s \n", ss.str().c_str());

    // Send HTTP POST request
    int httpResponseCode = http.POST(ss.str().c_str());
    String body = http.getString();
    disconnectWifi();

    if (httpResponseCode != HTTP_CODE_OK)
    {
        Serial.printf("HTTP Response Code: %i\nBody: ", httpResponseCode);
        Serial.println(body);
        return false;
    }
    else
    {
        if (body.equals("Success!"))
        {
            Serial.print("HTTP Response Code 200: ");
            Serial.println(body);
        }
        else
        {
            Serial.printf("HTTP Response Code 200, but Body not Success! Body instead: %s\n", body);
        }
        // body should be "Success!" if not changed in server
        // TODO: remove body check later for better performance
        return true;
    }
}