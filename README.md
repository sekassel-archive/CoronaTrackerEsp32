# CoronaTrackerEsp32

An alternative for the [Corona-Warn-App](https://www.coronawarn.app/de/) for the ESP32. 

## Table of contents

* [Overview](#Overview)
* [Hardware](#Hardware)
* [Building your own tracker](#Building-your-own-tracker)
  * [Flashing the ESP32](#Flashing-the-ESP32)
    * [Using binaries](#Using-binaries)
    * [Using source code](#Using-source-code)
  * [Setting up the ESP](#Setting-up-the-ESP)
* [Known issues](#Known-issues)



## Overview

Goal of the project is to create a do it yourself coronatracker and replace the CWA-App on your phone with this device. Advantages of using it is its cheapness and availability, as well as not having to use the app on your phone.

The program mimics the [Exposure Notification Protocol](https://www.google.com/covid19/exposurenotifications/) by Apple/Google, that is used by CWA. Parts that could not be done on the microcontroller itself are done by our server (mainly Google's [Protocol Buffers](https://developers.google.com/android/exposure-notifications/exposure-key-file-format))

This project is still work in progress and some features are incomplete/not yet implemented.

## Hardware

The software should work on most ESP32 boards. Internally we use **LILYGO® TTGO ESP32 WiFi + Bluetooth 18650 Battery Protection Board**, mainly because of usability of 18650 batteries. We recommend using this board as the software as well as the installation process was tested on it.

<img src="doc\images\ESP_Front.jpg"  width=25% height=25%  /><img src="doc\images\ESP_Back.jpg" width=25% height=25% />

Depending on how the pins on your board are configured you may need to change them.

## Building your own tracker

All you need to do is follow this tutorial as well as a suitable ESP32 board.

### Flashing the ESP32

You can either flash the ESP with our precompiled [binaries](#using-binaries) or use the [source code](#using-source-code) from this repository. There are tutorial for both approaches below.

#### Using binaries

1. Download the [Flash Download Tools](https://www.espressif.com/en/support/download/other-tools) from Espressif.

2. Download and unzip the [latest release binaries](https://github.com/sekassel/CoronaTrackerEsp32/releases). (These are called `release_[version].zip`)

3. Unpack the Flash Download Tool and start it.

   <img src="doc\images\EXPLORER_START_TOOL.png"  />

   1. Select `Developer Mode`

      <img src="doc\images\FDT_DEV.png"  />

   2. Select `ESP32 DownloadTool`

      <img src="doc\images\FDT_ESP32.png"  />

   

4. Add the binary files.

   1. First click on the shown button to open file explorer.

      <img src="doc\images\FDT_ADD_BIN.png"  />

   2. Navigate to the downloaded binaries and open the first one. (`_a_0x[...].bin`)

      <img src="doc\images\EXPLORER_CHOOSE_BIN.png"  />

   3. Repeat these steps until all binaries are added. Select them in alphabetical order. Afterwards it should look like this:

      <img src="doc\images\FDT_BINS_ADDED.png"  />

5. Input the addresses to the corresponding binaries. Those are located after the second `_` of the file name and before `.bin`. For example, the address corresponding to  `_a_0x1000.bin` would be `0x1000`.

   <img src="doc\images\FDT_ADDRESS_ADDED.png"  />

6. Check all checkmarks for the added binaries.

   <img src="doc\images\FDT_CHECKMARKS.png"  />

7. If not already connected plug in your ESP32. Make sure the Flash Download Tool has recognized your device. Open the `COM` drop-down menu and select the available port. (There should only be one port selectable if only one device is connected)

   You can also check this image to see if your settings are correct.

   <img src="doc\images\FDT_COM_PORT.png"  />

8. Hold down the `Boot` button on your ESP and then press the `EN` button to enter bootloader mode. Let go of both buttons.

9. Flash your ESP.

   1. Click on `Start`. The status should now change to `Sync`.

      <p float="left">
      	<img src="doc\images\FDT_START.png" width=35% height=25%  />
      	<img src="doc\images\FDT_SYNCH.png" width=50% height=50%  />
      </p>

   2. Now press the `Boot` button again. The status should change to `Download`.

      <img src="doc\images\FDT_DOWNLOAD.png"  />

   3. When the ESP is successfully flashed the status will show `Finish`.

      <img src="doc\images\FDT_FINISH.png"  />

10. Press the `EN` button. The ESP should restart now and the display will turn on.

11. You can now [setup](#setting-up-the-esp) your ESP.

#### Using source code

1. Download and install [Visual Studio Code](https://code.visualstudio.com/). 

2. Install [PlatformIO](https://platformio.org/) for VS Code. ([Instructions](https://platformio.org/install/ide?install=vscode)) There may be a prompt to restart VS Code after installing.

3. Clone or download this project. You can use `Download ZIP`on the top right of this site. Alternatively the source code can be downloaded [here](https://github.com/sekassel/CoronaTrackerEsp32/releases).
   Unzip it in a suitable directory.

   <img src="doc\images\git_download.png"  />

4. Open the project with PlatformIO. 

   1. Open Platform IO Home.

      <img src="doc\images\PIO_Home.png"  />

   2. Click on `Open Folder`and search for the downloaded project.

      <img src="doc\images\PIO_Open.png"  />

   3. Open the `tracker` folder. It might take awhile until the project is fully loaded.

      <img src="doc\images\PIO_OpenFolder.png"  />

5. Connect your ESP32 via USB and upload the project. (If you are using our board you have to press the `Boot` button, when the console reads `Connecting...`)

   <img src="doc\images\PIO_Upload.png" width=50% height=50% />

   This message should appear at the bottom of the terminal if the upload was successful.

   <img src="doc\images\PIO_Success.png"/>

6. You can now [setup](#setting-up-the-esp) your ESP.

### Setting up the ESP

1. After a successful flash the ESP will start an access point called `Coronatracker`to set up WiFi. Connect to the AP and open `192.168.4.1`. (Depending on the device you might get a notification that there is no connection to the internet) Enter your WiFi credentials and click safe.

   <img src="doc\images\WiFI_1.png" width=25% height=25% /><img src="doc\images\WiFi_2.png" width=25% height=25% />

2. The ESP will now start initialization and afterwards simulating the CWA-App.

## Known issues

- [After initial configuration, WiFi credentials can not be changed without re-flashing the device.](https://github.com/sekassel/CoronaTrackerEsp32/issues/21)
- [WiFi credentials will be lost if the device looses power.](https://github.com/sekassel/CoronaTrackerEsp32/issues/22)
- [There is no official protocol for uploading infected keys.](https://github.com/sekassel/CoronaTrackerEsp32/issues/23)
- [Time displayed is always in CEST.](https://github.com/sekassel/CoronaTrackerEsp32/issues/24)
- [There is no real solution for full storage.](https://github.com/sekassel/CoronaTrackerEsp32/issues/25)
- Power consumption is a problem on small batteries.