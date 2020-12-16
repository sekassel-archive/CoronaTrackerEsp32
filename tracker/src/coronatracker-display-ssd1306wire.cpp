#ifdef ESP32DEVOTA_COMMON

#include "coronatracker-display-font.h"
#include "coronatracker-display-ssd1306wire.h"
#include "SSD1306Wire.h"


#define DISPLAY_ADRESS 0x3c
#define DISPLAY_SDA 5
#define DISPLAY_SCL 4

SSD1306Wire display(DISPLAY_ADRESS, DISPLAY_SDA, DISPLAY_SCL);

void initDisplay()
{
    if (!display.init())
    {
        Serial.println("Display init error.");
    }
    display.setFont(ArialMT_Plain_16); //10, 16 or 24 are possible choices
    display.setTextAlignment(TEXT_ALIGN_LEFT);
}

void configureWifiMessageOnDisplay()
{
    Serial.println("showStartWifiMessageOnDisplay");
    display.setFont(Nimbus_Sans_L_Regular_Condensed_15);
    display.drawString(0, 0, "Connect to Wifi-Portal"); //probably 17 letters with size 16
    display.drawString(0, 15, "Name: Coronatracker");
    display.drawString(0, 30, "Sign-in to network or");
    display.drawString(0, 45, "connect to 192.168.4.1");
    display.display();
    display.setFont(ArialMT_Plain_16);
}

void wifiConfiguredOnDisplay(bool configured)
{
    initDisplay();
    display.setTextAlignment(TEXT_ALIGN_CENTER);
    display.drawString(64, 0, "Wifi-Config:");
    if (configured)
    {
        display.drawString(64, 16, "success!");
    }
    else
    {
        display.drawString(64, 16, "failed!");
    }
    display.display();
}

void defaultDisplay(struct tm timeinfo, int action, exposure_status exposureStatus, int numberOfScanedDevices)
{
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
    display.drawString(0, 0, wDay + ", " + mDay + "." + month + "." + year);
    display.setTextAlignment(TEXT_ALIGN_RIGHT);
    display.drawString(128, 16, " " + hour + ":" + minute);
    display.setTextAlignment(TEXT_ALIGN_LEFT);
    if (action == ACTION_NOTHING)
    {
        display.drawString(0, 16, "Initialize");
    }
    else if (action == ACTION_ADVERTISE)
    {
        display.drawString(0, 16, "Advertise");
    }
    else if (action == ACTION_SCAN)
    {
        display.drawString(0, 16, "Scan");
    }
    else if (action == ACTION_INFECTION_REQUEST)
    {
        display.drawString(0, 16, "cwa update");
    }
    else if (action == ACTION_SLEEP)
    {
        display.drawString(0, 16, "Sleep");
    }
    display.drawString(0, 32, "Seen devices: " + ((numberOfScanedDevices == -1) ? "-" : (String)numberOfScanedDevices));
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

    display.drawString(0, 47, status);
    display.display();
}

void afterInfectionRequestOnDisplay(exposure_status exposureStatus)
{
    initDisplay();
    String ret = "";
    display.setTextAlignment(TEXT_ALIGN_CENTER);
    if (exposureStatus == EXPOSURE_NO_DETECT)
    {
        display.drawString(64, 0, "No exposure");
        display.drawString(64, 16, "detected");
    }
    else if (exposureStatus == EXPOSURE_DETECT)
    {
        display.drawString(64, 0, "Exposure");
        display.drawString(64, 16, "detected!");
    }
    else if (exposureStatus == EXPOSURE_UPDATE_FAILED)
    {
        display.drawString(64, 0, "Update failed");
    }
    else
    {
        display.drawString(64, 0, "No update yet");
    }

    display.display();
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