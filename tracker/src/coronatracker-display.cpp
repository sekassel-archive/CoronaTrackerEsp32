#include "coronatracker-display.h"
#include "SSD1306Wire.h"

int REQUEST_DELAY_SECONDS = 1;

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

void showRequestDelayOnDisplay()
{
    struct tm timeinfo;
    Serial.printf("Time get: %d\n", getLocalTime(&timeinfo));
    //convert REQUEST_DELAY_SECONDS into hour, minute format
    int currentHoursInSeconds = timeinfo.tm_hour * HOUR;
    int currentMinutesInSeconds = timeinfo.tm_min * MINUTE;
    int request_delay_seconds = REQUEST_DELAY_SECONDS;

    if (request_delay_seconds + currentHoursInSeconds + currentMinutesInSeconds + timeinfo.tm_sec >= DAY)
    {
        timeinfo.tm_wday += 1;
    }

    if ((request_delay_seconds % HOUR) + currentMinutesInSeconds + timeinfo.tm_sec >= HOUR)
    {
        timeinfo.tm_hour = (timeinfo.tm_hour + (request_delay_seconds / HOUR) + 1) % 24;
        request_delay_seconds = REQUEST_DELAY_SECONDS % HOUR;

        if ((request_delay_seconds % MINUTE) + timeinfo.tm_sec >= MINUTE)
        {
            timeinfo.tm_min = (timeinfo.tm_min + (request_delay_seconds / MINUTE) + 1) % 60;
            request_delay_seconds = REQUEST_DELAY_SECONDS % MINUTE;
            timeinfo.tm_sec = (timeinfo.tm_sec + request_delay_seconds) % 60;
        }
        else
        {
            timeinfo.tm_min = (timeinfo.tm_min + (request_delay_seconds / MINUTE)) % 60;
            request_delay_seconds = REQUEST_DELAY_SECONDS % MINUTE;
            timeinfo.tm_sec = (timeinfo.tm_sec + request_delay_seconds) % 60;
        }
    }
    else
    {
        timeinfo.tm_hour = (timeinfo.tm_hour + (request_delay_seconds / HOUR)) % 24;
        request_delay_seconds = REQUEST_DELAY_SECONDS % HOUR;

        if ((request_delay_seconds % MINUTE) + timeinfo.tm_sec >= MINUTE)
        {
            timeinfo.tm_min = (timeinfo.tm_min + (request_delay_seconds / MINUTE) + 1) % 60;
            request_delay_seconds = REQUEST_DELAY_SECONDS % MINUTE;
            timeinfo.tm_sec = (timeinfo.tm_sec + request_delay_seconds) % 60;
        }
        else
        {
            timeinfo.tm_min = (timeinfo.tm_min + (request_delay_seconds / MINUTE)) % 60;
            request_delay_seconds = REQUEST_DELAY_SECONDS % MINUTE;
            timeinfo.tm_sec = (timeinfo.tm_sec + request_delay_seconds) % 60;
        }
    }
}

void configureWifiMessageOnDisplay()
{
    Serial.println("showStartWifiMessageOnDisplay");
    display.clear();
    display.drawString(0, 0, "Configure wifi on"); //probably 18 letters with size 16
    display.drawString(0, 20, "your Phone or ");
    display.drawString(0, 40, "Computer.");
    display.display();
}

void showLocalTimeOnDisplay(struct tm timeinfo)
{
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

void buttonPressedInMainOnDisplay()
{
    Serial.println("Boot button was pressed.");
    display.clear();
    display.drawString(0, 20, "Woke up.");
    display.display();
}

void clearDisplay()
{
    display.clear();
    display.display();
}

void wifiConfiguredSuccessfullyOnDisplay()
{
    display.clear();
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