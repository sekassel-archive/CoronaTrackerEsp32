#include <SPI.h>
#include <TFT_eSPI.h>
#include <Wire.h>

//Time Constants
const static int HOUR = 3600;
const static int MINUTE = 60;
const static int DAY = 86400;

void showIsInfectedOnDisplay(bool metInfected);
void tftInit(int requestDelaySeconds);
void showLocalTimeOnDisplay(struct tm timeinfo);
void showStartWifiMessageOnDisplay();
void configureWifiMessageOnDisplay();
void showRequestDelayOnDisplay();