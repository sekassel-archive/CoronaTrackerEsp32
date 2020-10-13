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