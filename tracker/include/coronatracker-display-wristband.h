#ifdef LILYGO_WRISTBAND

#include <Arduino.h>
#include "coronatracker-wifi.h"
#include <TFT_eSPI.h>
#include <SPI.h>
#include <Wire.h>

#define TP_PIN_PIN 33
#define I2C_SDA_PIN 21
#define I2C_SCL_PIN 22
#define IMU_INT_PIN GPIO_NUM_38
#define RTC_INT_PIN 34
#define BATT_ADC_PIN 35
#define VBUS_PIN 36
#define TP_PWR_PIN 25
#define LED_PIN 4
#define CHARGE_PIN GPIO_NUM_32
#define MPU_ADDR 0x69
#define TFT_BL 27  // Dispaly backlight control pin

// ST7735 specific commands used in init
#define ST7735_SWRESET 0x01 //
#define ST7735_SLPIN   0x10 //
#define ST7735_DISPOFF 0x28 //
#define SEG7_BACKGROUND 0x0821 //

#define ACTION_NOTHING 0
#define ACTION_ADVERTISE 1
#define ACTION_SCAN 2
#define ACTION_WIFI_CONFIG 3
#define ACTION_INFECTION_REQUEST 4
#define ACTION_SLEEP 5

void initDisplay();
void configureWifiMessageOnDisplay();
void wifiConfiguredOnDisplay(bool configured);
void defaultDisplay(struct tm timeinfo, int action, exposure_status exposureStatus, int numberOfScanedDevices);
String weekDayToString(int weekDay);
void afterInfectionRequestOnDisplay(exposure_status exposureStatus);

#endif