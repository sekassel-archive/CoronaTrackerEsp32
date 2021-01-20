#ifdef LILYGO_WRISTBAND

#include "coronatracker-display-wristband.h"

TFT_eSPI tft = TFT_eSPI();

// TODO:    change X & Y coordinates for display purposes

void initDisplay()
{
  tft.init();
  tft.setRotation(1);
  tft.setSwapBytes(true);
  tft.fillScreen(TFT_BLACK);
  ledcSetup(0, 5000, 8);
  ledcAttachPin(TFT_BL, 0);
  ledcWrite(0, 185);
}

void configureWifiMessageOnDisplay()
{
    tft.fillScreen(TFT_BLACK);
    tft.setTextDatum(L_BASELINE);
    tft.setTextColor(TFT_WHITE);
    Serial.println("showStartWifiMessageOnDisplay");
    tft.drawString("Connect to Wifi-Portal", 2, 5, 1);
    tft.drawString("Name: Coronatracker", 2, 15, 1);
    tft.drawString("Sign-in to network or", 2, 25, 1);
    tft.drawString("connect to 192.168.4.1", 2, 35, 1);
}

void wifiConfiguredOnDisplay(bool configured)
{
    tft.fillScreen(TFT_BLACK);
    tft.setTextDatum(L_BASELINE);
    tft.setTextColor(TFT_WHITE);
    
    if (configured)
    {
        tft.drawString("Wifi-Config: success!", 2, 20, 2);
    }
    else
    {
        tft.drawString("Wifi-Config: failed!", 2, 20, 2);
    }
}

void defaultDisplay(struct tm timeinfo, int action, exposure_status exposureStatus, int numberOfScanedDevices)
{
    initDisplay();
    tft.fillScreen(TFT_BLACK);
    tft.setTextDatum(L_BASELINE);
    tft.setTextColor(TFT_WHITE);

    Serial.println("Default display");
    int HOUR = (timeinfo.tm_hour + 2) % 24; //tm_hour has a time offset of 2 hours
    String wDay = weekDayToString(timeinfo.tm_wday);
    String mDay = (timeinfo.tm_mday < 10 ? "0" : "") + (String)timeinfo.tm_mday;
    String month = ((timeinfo.tm_mon + 1) < 10 ? "0" : "") + (String)(timeinfo.tm_mon + 1); //tm_mon starts at 0
    String year = (String)(timeinfo.tm_year + 1900);                                        //tm_years are the years since 1900
    String hour = (HOUR < 10 ? "0" : "") + (String)HOUR;
    String minute = (timeinfo.tm_min < 10 ? "0" : "") + (String)timeinfo.tm_min;
    //Mon, 24.07.2023 17:34
    tft.drawString(wDay + ", " + mDay + "." + month + "." + year, 2, 10, 2);
    tft.drawString(" " + hour + ":" + minute, 2, 25, 2);
    if (action == ACTION_NOTHING)
    {
        tft.drawString("Initialize", 2, 30, 1);
    }
    else if (action == ACTION_ADVERTISE)
    {
        tft.drawString("Advertise", 2, 30, 1);
    }
    else if (action == ACTION_SCAN)
    {
        tft.drawString("Scan", 2, 30, 1);
    }
    else if (action == ACTION_INFECTION_REQUEST)
    {
        tft.drawString("cwa update", 2, 30, 1);
    }
    else if (action == ACTION_SLEEP)
    {
        tft.drawString("Sleep", 2, 30, 1);
    }
    tft.drawString("Seen devices: " + ((numberOfScanedDevices == -1) ? "-" : (String)numberOfScanedDevices), 2, 40, 1);
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

    tft.drawString(status, 2, 50, 1);
    delay(1500);
}

void afterInfectionRequestOnDisplay(exposure_status exposureStatus)
{
    initDisplay();
    tft.fillScreen(TFT_BLACK);
    tft.setTextDatum(L_BASELINE);
    tft.setTextColor(TFT_WHITE);

    String ret = "";
    if (exposureStatus == EXPOSURE_NO_DETECT)
    {
        tft.drawString("No exposure detected", 2, 10, 2);
    }
    else if (exposureStatus == EXPOSURE_DETECT)
    {
        tft.drawString("Exposure detected!", 2, 10, 2);
    }
    else if (exposureStatus == EXPOSURE_UPDATE_FAILED)
    {
        tft.drawString("Update failed", 2, 10, 2);
    }
    else
    {
        tft.drawString("No update yet", 2, 10, 2);
    }
    delay(1500);
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

#endif