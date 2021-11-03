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

int getDaylightOffset(void)
{
    if (!WiFi.isConnected() && !connectToStoredWifi())
    {
        Serial.println("Could not Connect to Wifi");
        return false;
    }
    else
    {
        HTTPClient http;

        http.begin(String(SERVER_URL) + String(GET_DAYLIGHT_OFFSET));
        int code = http.GET();
        if (!(code == HTTP_CODE_OK))
        {
            Serial.println("Failed to connect to server!");
            return -1;
        }
        else
        {
            String r = http.getString();
            http.end();

            if (r.equals("true"))
            {
                return 3600;
            }
            else
            {
                return 0;
            }
        }
    }
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

int getTodaysRsin(void)
{
    if (!WiFi.isConnected() && !connectToStoredWifi())
    {
        Serial.println("Could not Connect to Wifi");
        return 0;
    }
    else
    {
        HTTPClient http;

        http.begin(String(SERVER_URL) + String(GET_TODAYS_RSIN));
        int code = http.GET();

        if (!(code == HTTP_CODE_OK))
        {
            Serial.println("Failed to connect to server!");
            http.end();
            return 0;
        }
        else
        {
            String rsinStr = http.getString();
            http.end();

            if (rsinStr.length() == 7)
            {
                return rsinStr.toInt();
            }
            else
            {
                Serial.println("RSIN from Server != 7 character! Probably invalid RSIN.");
                Serial.print("Recieved string: ");
                Serial.println(rsinStr);
                return 0;
            }
        }
    }
}

exposure_status getInfectionStatus(std::string *uuidstr)
{
    if (!connectToStoredWifi())
    {
        Serial.println("Get infection status failed: couldn't connect to Wifi!");
        return EXPOSURE_UPDATE_FAILED;
    }

    HTTPClient http;

    // your domain name with URL path
    http.begin(String(SERVER_URL) + String(POST_INFECTION_STATUS));

    // specify content-type header
    http.addHeader("Content-Type", "application/json");

    // data to send with HTTP POST
    std::stringstream stringStream;
    stringStream << "{\"uuid\":\"" << uuidstr->c_str() << "\"}";

    // send HTTP POST request
    int httpResponseCode = http.POST(stringStream.str().c_str());
    String body = http.getString();
    disconnectWifi();

    if (httpResponseCode != HTTP_CODE_OK)
    {
        Serial.println("HTTP Response Code not 200!");
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

// wifi need to be activated beforehand
bool sendContactInformation(std::string *uuidstr, int enin, std::vector<std::string> *rpiData)
{
    HTTPClient http;

    http.begin(String(SERVER_URL) + String(POST_DATA_INPUT_RPIS));
    http.addHeader("Content-Type", "application/json");

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

    // send HTTP POST request
    int httpResponseCode = http.POST(ss.str().c_str());
    String body = http.getString();

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
            Serial.printf("Successfully sendet Contact Informations to Server! enin: %i\n", enin);
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

bool sendTekInformation(std::string *uuidstr, int enin, std::string *tekData)
{
    if (!connectToStoredWifi())
    {
        Serial.println("Couldn't connect to Wifi!");
        return false;
    }

    HTTPClient http;

    http.begin(String(SERVER_URL) + String(POST_DATA_INPUT_TEK));
    http.addHeader("Content-Type", "application/json");

    std::stringstream ss;
    ss << "{\"uuid\":\"" << uuidstr->c_str() << "\",\"enin\":\"" << enin << "\",\"tek\":\"" << tekData->c_str() << "\"}";

    // send HTTP POST request
    int httpResponseCode = http.POST(ss.str().c_str());
    String body = http.getString();

    disconnectWifi();

    if (httpResponseCode != HTTP_CODE_OK)
    {
        Serial.printf("HTTP Response Code: %i\n", httpResponseCode);
        Serial.printf("Tried to send enin: %i\twith tek: %s\n", enin, tekData->c_str());
        return false;
    }
    else
    {
        if (body.equals("Success!"))
        {
            Serial.print("HTTP Response Code 200: ");
            Serial.println(body);
            Serial.println("Successfully sendet Tek Information to Server!");
        }
        else
        {
            // body should be "Success!" if not changed in server, this should never happen
            Serial.printf("HTTP Response Code 200, but (Body) not Success! Body instead: %s\n", body);
        }
        return true;
    }
}

bool sendPinForVerification(std::string *uuidstr, std::string *pin, std::string *timestamp)
{
    if (!WiFi.isConnected() && !connectToStoredWifi())
    {
        Serial.println("Could not Connect to Wifi");
        return false;
    }

    HTTPClient http;

    http.begin(String(SERVER_URL) + String(POST_VERIFICATION));
    http.addHeader("Content-Type", "application/json");

    std::stringstream ss;
    ss << "{\"uuid\":\"" << uuidstr->c_str() << "\",\"pin\":\"" << pin->c_str() << "\"}";

    // send HTTP POST request
    int httpResponseCode = http.POST(ss.str().c_str());
    std::string bodyResponse = http.getString().c_str();

    if (httpResponseCode == HTTP_CODE_OK &&
        bodyResponse.compare("Invalid") != 0)
    {
        Serial.printf("HTTP Response Code: %i\nBody: ", httpResponseCode);
        Serial.println(bodyResponse.c_str());
        // bodyResponse should contain a timestamp to get get feedback from server after successfull user validation
        disconnectWifi();
        *timestamp = bodyResponse.c_str();
        return true;
    }
    else
    {
        // something went wrong, so abort
        Serial.print("HTTP Response Code 200: ");
        Serial.println(bodyResponse.c_str());
        Serial.println("Something went wrong on server / user-side. Abort!");
        disconnectWifi();
        return false;
    }
}

std::string checkServerSuccessfullDataInput(std::string *uuidstr, std::string *pin, std::string *timestamp)
{
    if (!WiFi.isConnected() && !connectToStoredWifi())
    {
        Serial.println("Could not Connect to Wifi");
        return "WAIT";
    }

    HTTPClient http;
    http.begin(String(SERVER_URL) + String(POST_VERIFICATION_DATA));
    http.addHeader("Content-Type", "application/json");

    std::stringstream ss;
    ss << "{\"uuid\":\"" << uuidstr->c_str() << "\",\"pin\":\"" << pin->c_str()
       << "\", \"timestamp\":\"" << timestamp->c_str() << "\"}";

    // send HTTP POST request
    int httpResponseCode = http.POST(ss.str().c_str());
    std::string bodyResponse = http.getString().c_str();
    disconnectWifi();

    if (httpResponseCode != HTTP_CODE_OK)
    {
        Serial.printf("HTTP Response Code: %i\n", httpResponseCode);
        return "WAIT";
    }
    return bodyResponse;
}