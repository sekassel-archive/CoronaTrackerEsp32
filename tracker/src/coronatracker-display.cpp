#include "coronatracker-display.h"
#include "SSD1306Wire.h"

SSD1306Wire display(0x3c, 5, 4);

void initDisplay()
{
    if (!display.init())
    {
        Serial.println("Display init error.");
    }
    display.setFont(ArialMT_Plain_16); //10, 16 or 24 are possible choices
    display.setTextAlignment(TEXT_ALIGN_LEFT);
}

void showRequestDelayOnDisplay(int bootsLeftUntilNextRequest, int bootsUntilScan)
{
    Serial.println("showRequestDelayOnDisplay");
    display.drawString(0, 32, "Next request: ");
    display.drawString(0, 48, (String)(bootsLeftUntilNextRequest / bootsUntilScan) + " minutes");
    display.display();
}

void configureWifiMessageOnDisplay()
{
    Serial.println("showStartWifiMessageOnDisplay");
    display.drawString(0, 0, "Connect to Wifi"); //probably 17 letters with size 16
    display.drawString(0, 16, "Name:Coronatracker");
    display.drawString(0, 32, "Sign-in to Network");
    display.drawString(0, 48, "or use 192.168.4.1");
    display.display();
}

void showLocalTimeOnDisplay(struct tm timeinfo)
{
    display.clear();
    int HOUR = timeinfo.tm_hour + 2; //the time difference from the server was not saved
    display.drawString(0, 0, (String) "Time: " + (HOUR < 10 ? "0" : "") + (String)HOUR + ":" + (timeinfo.tm_min < 10 ? "0" : "") + (String)timeinfo.tm_min + ":" + (timeinfo.tm_sec < 10 ? "0" : "") + (String)timeinfo.tm_sec);
    display.display();
}

void showIsInfectedOnDisplay(bool metInfected)
{
    display.clear();
    if (!metInfected)
    {
        display.drawString(0, 0, "You were not");
        display.drawString(0, 20, "exposed.");
    }
    else
    {
        display.drawString(0, 0, "You were exposed.");
        display.drawString(0, 20, "Check in with");
        display.drawString(0, 40, "your Doctor.");
        while (true)
        {
            delay(100000);
            yield();
        }
        //TODO show saved exposure keys or something for health authority
    }
    display.display();
}

void wifiConfiguredOnDisplay(bool configured)
{
    initDisplay();
    display.drawString(0, 0, "Wifi-Config:");
    display.setTextAlignment(TEXT_ALIGN_CENTER);
    if (configured)
    {
        display.drawString(0, 16, "success!");
    }
    else
    {
        display.drawString(0, 16, "failed!");
    }
    display.display();
}

void showNumberOfScanedDevicesOnDisplay(int scanedDevices)
{
    display.drawString(0, 16, "Seen devices: " + (String)scanedDevices);
    display.display();
}

void defaultDisplay(struct tm timeinfo, int action, String status, int numberOfScanedDevices)
{
    initDisplay();
    if (action == ACTION_WIFI_CONFIG)
    {
        //TODO
    }
    else
    {
        Serial.println("Print time on display");
        int HOUR = (timeinfo.tm_hour + 2) % 24; //tm_hour has a time offset of 2 hours
        String wDay = weekDayToString(timeinfo.tm_wday);
        String mDay = (timeinfo.tm_mday < 10 ? "0" : "") + (String)timeinfo.tm_mday;
        String month = ((timeinfo.tm_mon + 1) < 10 ? "0" : "") + (String)(timeinfo.tm_mon + 1); //tm_mon starts at 0
        String year = (String)(timeinfo.tm_year + 1900);                                        //tm_years are the years since 1900
        String hour = (HOUR < 10 ? "0" : "") + (String)HOUR;
        String minute = (timeinfo.tm_min < 10 ? "0" : "") + (String)timeinfo.tm_min;
        //Mon, 24.07.2023 17:34
        display.drawString(0, 0, wDay + ", " + mDay + "." + month + "." + year);
        display.drawString(78, 16, " " + hour + ":" + minute);
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
            display.drawString(0, 16, "CWA-Update");
        }
        else if (action == ACTION_SLEEP)
        {
            display.drawString(0, 16, "Sleep");
        }
        display.drawString(0, 32, "Seen devices: " + (String)numberOfScanedDevices);
        display.drawString(0, 48, status);

        display.display();
    }
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

String afterInfectionRequestOnDisplay(exposure_status exposureStatus)
{
    initDisplay();
    String ret = "";
    display.setTextAlignment(TEXT_ALIGN_CENTER);
    if (EXPOSURE_NO_DETECT)
    {
        display.drawString(0, 0, "No Exposure");
        display.drawString(0, 16, "Detected");
        ret = "No Exposures";
    }
    else if (EXPOSURE_DETECT)
    {
        display.drawString(0, 0, "Exposure");
        display.drawString(0, 16, "Detected!");
        ret = "!Exposures found!";
    }
    else if (EXPOSURE_UPDATE_FAILED)
    {
        display.drawString(0, 0, "Update failed");
        ret = "Update failed";
    }
    else
    {
        display.drawString(0, 0, "No update yet");
        ret = "No update yet";
    }

    display.display();
    return ret;
}