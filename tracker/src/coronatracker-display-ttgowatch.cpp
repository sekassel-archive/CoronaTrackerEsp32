#ifdef LILYGO_WATCH_2020_V1

#include "coronatracker-display-ttgowatch.h"
#include "LilyGoWatch.h"

bool initialised_LILYGO_WATCH_2020_V1 = false;

void initDisplay()
{
    TTGOClass *ttgo = TTGOClass::getWatch();
    if (!initialised_LILYGO_WATCH_2020_V1) {
        initialised_LILYGO_WATCH_2020_V1 = true;
        Serial.println("TTGO Watch init.");
        ttgo->begin();
        ttgo->openBL();
    }
    ttgo->tft->fillScreen(TFT_BLACK);
    ttgo->tft->setTextColor(TFT_WHITE, TFT_BLACK);  // Adding a background colour erases previous text automatically
}

void configureWifiMessageOnDisplay()
{
    TTGOClass *ttgo = TTGOClass::getWatch();
    Serial.println("showStartWifiMessageOnDisplay");
    ttgo->tft->drawString("Connect to", 0,  0, 4);
    ttgo->tft->drawString("Wifi-Portal", 0,  25, 4);
    ttgo->tft->drawString("Name: Coronatracker",    0, 80, 4);
    ttgo->tft->drawString("Sign-in to network",  0, 120, 4);
    ttgo->tft->drawString("or connect to",  0, 145, 4);
    ttgo->tft->drawString("192.168.4.1", 0, 190, 4);
}

void wifiConfiguredOnDisplay(bool configured)
{
    TTGOClass *ttgo = TTGOClass::getWatch();
    initDisplay();
    ttgo->tft->drawString("   Wifi-Config:",    0, 80, 4);
    if (configured)
    {
        ttgo->tft->drawString("    success!",    0, 125, 4);
    }
    else
    {
        ttgo->tft->drawString("    failed!",    0, 125, 4);
    }
}

void defaultDisplay(struct tm timeinfo, int action, exposure_status exposureStatus, int numberOfScanedDevices)
{
    TTGOClass *ttgo = TTGOClass::getWatch();
    initDisplay();
    Serial.println("Default display");
    int HOUR = (timeinfo.tm_hour + 2) % 24; //tm_hour has a time offset of 2 hours
    String wDay = weekDayToString(timeinfo.tm_wday);
    String mDay = (timeinfo.tm_mday < 10 ? "0" : "") + (String)timeinfo.tm_mday;
    String month = ((timeinfo.tm_mon + 1) < 10 ? "0" : "") + (String)(timeinfo.tm_mon + 1); //tm_mon starts at 0
    String year = (String)(timeinfo.tm_year + 1900);                                        //tm_years are the years since 1900
    String hour = (HOUR < 10 ? "0" : "") + (String)HOUR;
    String minute = (timeinfo.tm_min < 10 ? "0" : "") + (String)timeinfo.tm_min;
    //Mon, 24.07.2023 17:34
    ttgo->tft->drawString(wDay + ", " + mDay + "." + month + "." + year, 0, 0, 2);
    ttgo->tft->drawString(" " + hour + ":" + minute, 128, 16, 2);
    if (action == ACTION_NOTHING)
    {
        ttgo->tft->drawString("Initialize", 0, 16, 2);
    }
    else if (action == ACTION_ADVERTISE)
    {
        ttgo->tft->drawString("Advertise", 0, 16, 2);
    }
    else if (action == ACTION_SCAN)
    {
        ttgo->tft->drawString("Scan", 0, 16, 2);
    }
    else if (action == ACTION_INFECTION_REQUEST)
    {
        ttgo->tft->drawString("cwa update", 0, 16, 2);
    }
    else if (action == ACTION_SLEEP)
    {
        ttgo->tft->drawString("Sleep", 0, 16, 2);
    }
    ttgo->tft->drawString( "Seen devices: " + ((numberOfScanedDevices == -1) ? "-" : (String)numberOfScanedDevices), 0, 32, 2);
    String status = "";
    if (exposureStatus == EXPOSURE_NO_DETECT)
    {
        status = "No exposures";
    }
    else if (exposureStatus == EXPOSURE_DETECT)
    {
        status = "!Exposures found!";
    }
    else if (exposureStatus == EXPOSURE_UPDATE_FAILED)
    {
        status = "Update failed";
    }
    else
    {
        status = "No update yet";
    }

    ttgo->tft->drawString(status, 0, 47, 2);
}

String weekDayToString(int weekDay)
{
    switch (weekDay)
    {
    case 0:
        return "Sun";
        break;
    case 1:
        return "Mon";
        break;
    case 2:
        return "Tue";
        break;
    case 3:
        return "Wed";
        break;
    case 4:
        return "Thu";
        break;
    case 5:
        return "Fri";
        break;
    case 6:
        return "Sat";
        break;
    default:
        return "Day";
        break;
    }
}

void afterInfectionRequestOnDisplay(exposure_status exposureStatus)
{
    TTGOClass *ttgo = TTGOClass::getWatch();
    initDisplay();
    if (exposureStatus == EXPOSURE_NO_DETECT)
    {
        ttgo->tft->drawString("No exposure", 64, 0, 2);
        ttgo->tft->drawString("detected", 64, 16, 2);
    }
    else if (exposureStatus == EXPOSURE_DETECT)
    {
        ttgo->tft->drawString("Exposure", 64, 0, 2);
        ttgo->tft->drawString("detected!", 64, 16, 2);
    }
    else if (exposureStatus == EXPOSURE_UPDATE_FAILED)
    {
        ttgo->tft->drawString("Update failed", 64, 0, 2);
    }
    else
    {
        ttgo->tft->drawString("No update yet", 64, 0, 2);
    }
}

#endif