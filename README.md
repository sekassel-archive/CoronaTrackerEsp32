# CoronaTrackerEsp32

Goal of the project is to create a do it yourself coronatracker.
We use a cheap microcontroller (ESP32) with bluetooth and wifi.
Our code is compatible with the german Corona-Warn-App. Our tracker recognizes smartphones with the Corona-Warn-App and they recognize our tracker. 
The idea is that you can carry the tracker instead of having the app on your smartphone.
The tracker is connected to a chosen wifi so it can request the infected keys from the official Corona-Warn-App server and warn you if you met someone infected.

This project is still work in progress and some features are incomplete/not yet implemented.

## Hardware

At the moment we can not guarantee that the code works on every ESP32 board. The display often needs some different settings and the pins have to be configured.
The device we are planning to optimize for our tracker is the T-WATCH-2020 from LILIGO.

## How you can built your own tracker?

First you need an ESP32 board with a battery.

- download the coronatracker software
- open the project in PlatformIO and upload
- configure wifi like it is shown on the display with help from your pc or phone
- don't forget to charge your device

The tracker connects to the server now and starts advertising like the corona-warn-app does.
