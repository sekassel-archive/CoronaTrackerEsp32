# CoronaTrackerEsp32

An alternative for the [Corona-Warn-App](https://www.coronawarn.app/de/) for the ESP32. 

## Overview

Goal of the project is to create a do it yourself coronatracker and replace the CWA-App on your phone with this device. Advantages of using it is its cheapness and availability, as well as not having to use the app on your phone.

The program mimics the [Exposure Notification Protocol](https://www.google.com/covid19/exposurenotifications/) by Apple/Google, that is used by CWA. Parts that could not be done on the microcontroller itself are done by our server (mainly Googles [Protocol Buffers](https://developers.google.com/android/exposure-notifications/exposure-key-file-format))

This project is still work in progress and some features are incomplete/not yet implemented.

## Hardware

The software should work on most ESP32 boards. Internally we use **LILYGO® TTGO ESP32 WiFi + Bluetooth 18650 Battery Protection Board**, mainly because of usability of 18650 batteries.

<img src="doc\images\ESP_Front.jpg" style="zoom: 25%;" /><img src="doc\images\ESP_Back.jpg" style="zoom: 25%;" />

Depending on how the pins on your board are configured you may need to change them.

## How you can built your own tracker?

Choose a suitable ESP32 board.

1. Download and install [Visual Studio Code](https://code.visualstudio.com/). 

2. Install [PlatformIO](https://platformio.org/) for VS Code. ([Instructions](https://platformio.org/install/ide?install=vscode)) There may be a prompt to restart VS Code after installing.

3. Clone or download this project. You can use `Download ZIP`on the top right of this site. Unzip it in a suitable directory.<img src="doc\images\git_download.png"  />

4. Open the project with PlatformIO. 

   1. Open Platform IO Home.

      <img src="doc\images\PIO_Home.png"  />

   2. Click on `Open Folder`and search for the downloaded project.

      <img src="doc\images\PIO_Open.png"  />

   3. Open the `tracker` folder. It might take awhile until the project is fully loaded.

      <img src="doc\images\PIO_OpenFolder.png"  />

5. Connect your ESP32 via USB and upload the project. (If you are using our board you have to press the `Boot` button, when the console reads `Connecting...`)

   <img src="doc\images\PIO_Upload.png"  />

   This message should appear at the bottom of the terminal if the upload was successful.

   ![](doc\images\PIO_Success.png)

6. The ESP will now start an access point called `Coronatracker`to set up WiFi. Connect to the AP and open `192.168.4.1`. (Depending on the device you might get a notification that there is no connection to the internet) Enter your WiFi credentials and click safe.

   <img src="doc\images\WiFI_1.png" style="zoom: 10%;" /><img src="doc\images\WiFi_2.png" style="zoom: 10%;" />

7. The ESP will now start initialization and afterwards simulating the CWA-App.