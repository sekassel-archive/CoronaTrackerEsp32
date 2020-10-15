#include "coronatracker-display.h"
#include "SSD1306Wire.h"

SSD1306Wire display(0x3c, 5, 4);

void initDisplay()
{
    if (!display.init())
    {
        Serial.println("Display init error.");
    }
    display.setFont(ArialMT_Plain_10); //10, 16 or 24 are possible choices
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
    display.drawString(0, 0, "Configure wifi on"); //probably 17 letters with size 16
    display.drawString(0, 20, "your Phone or ");
    display.drawString(0, 40, "Computer.");
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

void wifiConfiguredSuccessfullyOnDisplay()
{
    display.clear();
    display.setTextAlignment(TEXT_ALIGN_CENTER);
    display.drawString(0, 20, "Wifi configured.");
    display.display();
}

void configureWifiFailedOnDisplay()
{
    display.clear();
    display.drawString(0, 0, "Configure Wifi");
    display.drawString(0, 20, "failed.");
    display.display();
}

void showNumberOfScanedDevicesOnDisplay(int scanedDevices)
{
    display.drawString(0, 16, "Seen devices: " + (String)scanedDevices);
    display.display();
}

void defaultDisplay(struct tm timeinfo, String action, String status, int numberOfScanedDevices)
{
    initDisplay();
    Serial.println("Print time on display");
    int HOUR = (timeinfo.tm_hour + 2) % 24; //tm_hour has a time offset of 2 hours
    String wDay = weekDayToString(timeinfo.tm_wday);
    String mDay = (timeinfo.tm_mday < 10 ? "0" : "") + (String)timeinfo.tm_mday;
    String month = ((timeinfo.tm_mon + 1) < 10 ? "0" : "") + (String)(timeinfo.tm_mon + 1); //tm_mon starts at 0
    String year = (String)(timeinfo.tm_year + 1900);                                        //tm_years are the years since 1900
    String hour = (HOUR < 10 ? "0" : "") + (String)HOUR;
    String minute = (timeinfo.tm_min < 10 ? "0" : "") + (String)timeinfo.tm_min;
    //Mon, 24.07.2023 17:34
    display.drawString(0, 0, wDay + ", " + mDay + "." + month + "." + year + " " + hour + ":" + minute);
    display.drawString(0, 10, action);
    display.drawString(0, 20, status);
    display.drawString(0, 30, "Seen devices: " + (String)numberOfScanedDevices);

    display.display();
    // display.setColor(BLACK);
    // display.fillRect(0, 10, 128, 10);
    // display.display();
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