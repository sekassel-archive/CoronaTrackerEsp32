#include <Arduino.h>

#include "coronatracker-utils.h"
#include "coronatracker-crypto.h"
#include "coronatracker-wifi.h"
#include "coronatracker-spiffs.h"
#include "coronatracker-ble.h"

#pragma region DISPLAY_INCLUDES
#ifdef LILYGO_WATCH_2020_V1
#include "LilyGoWatch.h"
#include "coronatracker-display-ttgowatch.h"
#endif

#ifdef ESP32DEVOTA_COMMON
#include "coronatracker-display-ssd1306wire.h"
#endif

#ifdef LILYGO_WRISTBAND
#include "coronatracker-display-wristband.h"
#endif
#pragma endregion DISPLAY_INCLUDES

void firstStartInitializeSteps()
{
    !initSpiffsCreateDataBases() ? restartAfterErrorWithDelay("SPIFFS initialize failed!") : Serial.println("Initialized SPIFFS.");
    !connectToStoredWifi() ? restartAfterErrorWithDelay("Couldn't connect to Wifi!") : Serial.println("Connected to WiFi.");
    !initializeTime() ? restartAfterErrorWithDelay("Time initialize failed!") : Serial.println("Initialized Time.");
    !initializeTek() ? restartAfterErrorWithDelay("Failed to generate Temporary Exposure Key!") : Serial.println("Initialized Tek.");
    !initializeUuid() ? restartAfterErrorWithDelay("Failed to get UUID!") : Serial.println("Initialized UUID.");
    !disconnectWifi() ? Serial.println("Disconnect Failed!") : Serial.println("Disconnecting Wifi.");
}

void startInitializeSteps(void)
{
    !initSPIFFS() ? restartAfterErrorWithDelay("SPIFFS initialize failed!") : Serial.println("Initialized SPIFFS.");
}

void processVerificationForUserInput(void)
{
    int count = 4;
    // button should at least be pressed a amount of time
    do
    {
        displayVerificationCountdown(count--);
        sleep(1);
        if (digitalRead(0) != 0)
        {
            return;
        }
    } while (count != 0);

    // wait for release button
    displayReleaseButton();
    while (digitalRead(0) == 0)sleep(1);

    // generate PIN and get UUID to display
    srand((unsigned)time(NULL));
    int randomPinNumber = rand() % 10000; // random numbers from 0000 to 9999

    std::stringstream ss;
    for (int i = 4 - String(randomPinNumber).length(); i != 0; i--)
    {
        ss << "0";
    }
    ss << randomPinNumber;

    std::string pin = ss.str();
    std::string uuid = readUuid();

    displayUuidAndTekForVerification(uuid, pin);

    while (digitalRead(0) != 0)sleep(1);

    // wait for release button
    displayReleaseButton();
    while (digitalRead(0) == 0)sleep(1);

    // send pin to server for verification
    std::string *timestamp = sendPinForVerification(&uuid, &pin);
    if(timestamp == NULL)
    {
        displayVerificationFailed();
        sleep(5);
    }
    else
    {
        displayVerificationSuccess();
        sleep(5);
        // TODO:
        // check frequently if there is a valid entry for login with infected information
        // -> if infected, send needed TEK data
        // -> no infection, remove / ignore / reset infection_state to normal
    }
}

void actionScanForBluetoothDevices(int *scannedDevices)
{
    initializeBluetoothForScan();
    std::vector<std::__cxx11::string> rpis = scanForCovidDevices((uint32_t)SCAN_TIME);
    deinitBLE(true); // free memory for database interaction
    *scannedDevices = rpis.size();
    insertTemporaryRollingProximityIdentifiers(time(NULL), rpis);
    cleanUpTempDatabase();
}

void advertiseBluetoothDevice(int *scannedDevices)
{
    if (initializeBluetoothForAdvertisment() == false)
    {
        restartAfterErrorWithDelay("Initialize Bluetooth for Advertisment failed!");
    }

    digitalWrite(LED_PIN, LOW);
    delay(ADVERTISE_TIME);
    digitalWrite(LED_PIN, HIGH);
}

void setupWifiConnection(bool *wifiInitialized)
{
    configureWifiMessageOnDisplay();
    bool res = configureWifi();

    if (res)
    {
        Serial.println("Successfully connected to Wifi!");
        *wifiInitialized = true;
        digitalWrite(LED_PIN, HIGH);
        wifiConfiguredOnDisplay(true);
    }
    else
    {
        Serial.println("Couldn't connect to Wifi!");
        digitalWrite(LED_PIN, LOW);
        wifiConfiguredOnDisplay(false);
        // delay so feedback can be seen on LED
        delay(5000);
        ESP.restart();
    }
    delay(5000);
    disconnectWifi();
}

void sendCollectedDataToServer()
{
    // there may be problems with flash size, so maybe do this in smaller steps in future IF needed
    std::map<int, std::vector<std::string>> collectedContactInformation;

    if (!checkForCollectedContactInformationsInDatabase(&collectedContactInformation) ||
        collectedContactInformation.size() == 0)
    {
        Serial.println("checkForCollectedContactInformationsInDatabase has nothing to send to server!");
        return;
    }

    std::string uuid = readUuid();
    if (strcmp(uuid.c_str(), "NULL") == 0)
    {
        Serial.printf("Failed to read UUID from file!\n");
        return;
    }

    if (!connectToStoredWifi())
    {
        Serial.println("Couldn't connect to Wifi!");
        return;
    }

    std::map<int, std::vector<std::string>>::iterator it = collectedContactInformation.begin();
    while (it != collectedContactInformation.end())
    {
        int eninTmp = (int)it->first;
        if (!sendContactInformation(&uuid, eninTmp, &it->second))
        {
            // couldn't send data to server, so we need to keep it in db and try again later
            Serial.printf("Error in sendContactInformation for enin: %i\n", eninTmp);
            collectedContactInformation.erase(eninTmp);
        }
        it++;
    }

    disconnectWifi();

    // remove remaining (sended) collectedContactInformation from db, so it won't be send twice
    deleteCollectedContactInformationsSendedToServerFromDb(&collectedContactInformation);
}

exposure_status getInfectionStatusFromServer()
{
    // maybe move to main
    std::string uuid = readUuid();
    if (strcmp(uuid.c_str(), "NULL") == 0)
    {
        Serial.printf("Failed to read UUID from file!\n");
        return EXPOSURE_UPDATE_FAILED;
    }
    return getInfectionStatus(&uuid);
}

bool sendTekInfoToServerAfterInfection(void)
{
    int enin;
    std::string uuidstr;
    std::string tekData;

    // TODO: gather the data to be sent to server without duplicates

    return sendTekInformation(&uuidstr, enin, &tekData);
}

bool initializeTek(void)
{
    Serial.print("Generating TEK - ");
    signed char tek[16];
    int enin;
    if (getCurrentTek(tek, &enin))
    {
        Serial.print("TEK already exists: ");
        for (int i = 0; i < (sizeof(tek) / sizeof(tek[0])); i++)
        {
            Serial.print(tek[i]);
        }
        Serial.println(" ");
    }
    else
    {
        if (!generateNewTemporaryExposureKey(calculateENIntervalNumber(time(NULL))))
        {
            return false;
        }
        Serial.print("Generated TEK:");
        for (int i = 0; i < (sizeof(tek) / sizeof(tek[0])); i++)
        {
            Serial.print(tek[i]);
        }
        Serial.println(" ");
    }
    return true;
}

bool initializeTime(void)
{
    struct tm timeinfo;
    int start = millis();
    const int WAITTIME = 180000; // 3 Minutes

    do
    {
        if ((millis() - start) >= WAITTIME)
        {
            return false;
        }
        configTime(3600, 3600, NTP_SERVER);
        delay(500);

    } while (!getLocalTime(&timeinfo));
    Serial.print("Local Time: ");
    Serial.println(&timeinfo, "%A, %B %d %Y %H:%M:%S");
    delay(5000);
    return true;
}

bool initializeUuid()
{
    // 1) try to read uuid from spiff
    // 2) if not readable / valid uuid, try to get a new one from server (this should only happen once)
    // 3) try to write new uuid to spiff
    if (strcmp(readUuid().c_str(), "NULL") != 0)
    {
        return true;
    }
    else
    {
        std::string newUuid;
        if (getNewUuid(&newUuid))
        {
            return writeUuid(&newUuid);
        }
        else
        {
            return false;
        }
    }
}

size_t restartAfterErrorWithDelay(String errorMessage)
{
    digitalWrite(LED_PIN, LOW);
    Serial.print("Restart after Error: ");
    Serial.println(errorMessage);
    delay(10000);
    ESP.restart();
    return 0;
}