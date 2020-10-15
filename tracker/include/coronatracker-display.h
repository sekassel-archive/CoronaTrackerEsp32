#include <Arduino.h>

void initDisplay();
void showIsInfectedOnDisplay(bool metInfected);
void showLocalTimeOnDisplay(struct tm timeinfo);
void configureWifiMessageOnDisplay();
void showRequestDelayOnDisplay(int bootsLeftUntilNextRequest, int bootsUntilScan);
void wifiConfiguredSuccessfullyOnDisplay();
void configureWifiFailedOnDisplay();
void showNumberOfScanedDevicesOnDisplay(int scanedDevices);
void defaultDisplay(struct tm timeinfo, String action, String status, int numberOfScanedDevices);
String weekDayToString(int weekDay);