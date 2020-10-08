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
    display.setFont(ArialMT_Plain_10);
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
}

void showStartWifiMessageOnDisplay()
{
    display.drawString(0, 20, "Something Something");
    display.display();
    delay(3000);
    Serial.println("Clear display!");
    display.clear();
}

void showLocalTimeOnDisplay(struct tm timeinfo)
{
}

void showIsInfectedOnDisplay(bool metInfected)
{
    if (!metInfected)
    {
    }
    else
    {
    }
}

void buttonPressedInMain()
{
    Serial.println("Boot button was pressed.");
    display.drawString(0, 20, "Something Something");
    display.display();
    delay(3000);
    Serial.println("Clear display!");
    display.clear();
    display.display();
    delay(5000);
}

void clearDisplay()
{
    //throws core panic exception
    display.clear();
    display.display();
    delay(5000);
}