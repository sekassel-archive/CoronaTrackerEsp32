# CoronaTrackerEsp32

An alternative for the [Corona-Warn-App](https://www.coronawarn.app/de/) for the ESP32. 

## Overview

Goal of the project is to create a do it yourself coronatracker.
We use a cheap microcontroller (ESP32) with bluetooth and wifi.
Our code is compatible with the german Corona-Warn-App. Our tracker recognizes smartphones with the Corona-Warn-App and they recognize our tracker. 
The idea is that you can carry the tracker instead of having the app on your smartphone.
The tracker is connected to a chosen wifi so it can request the infected keys from the official Corona-Warn-App server and warn you if you met someone infected.

This project is still work in progress and some features are incomplete/not yet implemented.

## Hardware

The software should work on most ESP32 boards. Internally we use **LILYGO® TTGO ESP32 WiFi + Bluetooth 18650 Battery Protection Board**, mainly because of usability of 18650 batteries.

<img src="doc\images\ESP_Front.jpg" style="zoom: 40%;" /><img src="doc\images\ESP_Back.jpg" style="zoom: 40%;" />

Depending on how the pins on your board are configured you may need to change them.

## How you can built your own tracker?

Choose a suitable ESP32 board. This tutorial will use the TODO.

1. Download and install [Visual Studio Code](https://code.visualstudio.com/). 

2. Install [PlatformIO](https://platformio.org/) for VS Code. ([Instructions](https://platformio.org/install/ide?install=vscode)) There may be a prompt to restart VS Code after installing.

3. Clone or download this project. You can use `Download ZIP`on the top right of this site. Unzip it in a suitable directory.![](doc\images\git_download.png)

4. Open the project with PlatformIO. 

   1. Open Platform IO Home.

      ![](doc\images\PIO_Home.png)

   2. Click on `Open Folder`and search the downloaded project.

      ![](doc\images\PIO_Open.png)

   3. Open the `tracker` folder. It might take awhile until the project is fully loaded.

      ![](doc\images\PIO_OpenFolder.png)

5. Connect your ESP32 via USB and upload the project.

   <img src="doc\images\PIO_Upload.png" alt="PIO_Upload"  />

   This message should appear at the bottom of the terminal if the upload was successful.

   ![](doc\images\PIO_Success.png)

6. The ESP will now start an Access Point called `Coronatracker`to set up WiFi. Connect to the AP and open `192.168.4.1`. (Depending on the device you might get a notification that there i no connection to the Internet) Enter your WiFi Credentials and click safe.

   <img src="doc\images\WiFI_1.png" style="zoom: 33%;" /><img src="doc\images\WiFi_2.png" style="zoom: 33%;" />

7. The ESP will now start initialization and afterwards simulating the CWA-App.