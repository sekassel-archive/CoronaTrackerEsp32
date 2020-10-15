#include <Arduino.h>

#define ACTION_NOTHING 0
#define ACTION_ADVERTISE 1
#define ACTION_SCAN 2
#define ACTION_WIFI_CONFIG 3
#define ACTION_INFECTION_REQUEST 4
#define ACTION_SLEEP 5

void initDisplay();
void showIsInfectedOnDisplay(bool metInfected);
void showLocalTimeOnDisplay(struct tm timeinfo);
void configureWifiMessageOnDisplay();
void showRequestDelayOnDisplay(int bootsLeftUntilNextRequest, int bootsUntilScan);
void wifiConfiguredSuccessfullyOnDisplay();
void configureWifiFailedOnDisplay();
void showNumberOfScanedDevicesOnDisplay(int scanedDevices);
void defaultDisplay(struct tm timeinfo, int action, String status, int numberOfScanedDevices);
String weekDayToString(int weekDay);