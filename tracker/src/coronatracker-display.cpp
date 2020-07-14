#include "coronatracker-display.h"

TFT_eSPI tft = TFT_eSPI();
int REQUEST_DELAY_SECONDS;

void tftInit(int requestDelaySeconds)
{
    REQUEST_DELAY_SECONDS = requestDelaySeconds;

    tft.init();
    tft.setRotation(1);
    tft.fillScreen(TFT_BLACK);
    tft.setTextColor(TFT_WHITE);

    tft.setTextSize(1); //With size equals to 1, you can print 10 lines and about 27 characters per line
    tft.setCursor(0, 0);
}

void showRequestDelayOnDisplay()
{
    struct tm timeinfo;
    Serial.printf("Time get: %d\n", getLocalTime(&timeinfo));
    tft.fillScreen(TFT_BLACK);
    tft.setTextSize(2);
    tft.setCursor(0, 0);
    tft.print("Next Wifi-\nConnection:\n");
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

    tft.print(&timeinfo, "%H:%M:%S\n%A");
}

void configureWifiMessageOnDisplay()
{
    tft.fillScreen(TFT_BLACK);
    tft.setTextSize(2);
    tft.setCursor(0, 0);
    tft.print("Connect to\n\"Tracker\"\non your phone");
}

void showStartWifiMessageOnDisplay()
{
    tft.fillScreen(TFT_BLACK);
    tft.setTextSize(2);
    tft.print("Press Button\nfor 4 Seconds\nto start \nWifi-\nConfiguration");
}

void showLocalTimeOnDisplay(struct tm timeinfo)
{
    tft.fillScreen(TFT_BLACK);
    tft.setTextSize(2);
    tft.print(&timeinfo, "%A\n%B %d %Y\n%H:%M:%S"); //could look better when centered
    delay(10000);
}

void showIsInfectedOnDisplay(bool metInfected)
{
    if (!metInfected)
    {
        tft.fillScreen(TFT_BLACK);
        tft.setTextSize(2);
        tft.setCursor(0, 1);
        tft.print("\nEverything\nis fine.");
        delay(10000);
    }
    else
    {
        tft.fillScreen(TFT_BLACK);
        tft.setTextSize(2);
        tft.setCursor(0, 1);
        tft.print("You've met\nsomeone who\nis infected.\nPlease go\nquarantine.");
    }
}