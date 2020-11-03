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
* [Using the tracker](#Using-the-tracker)
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

   On your phone (or desktop):

   <img src="doc\images\WiFI_1.png" width=25% height=25% /><img src="doc\images\WiFi_2.png" width=25% height=25% /> 

   

   On the device:

   <img src="doc\images\DISPLAY_Wifi_message.jpeg" width=20% height=20%/>

   

   If the WiFi configuration was successful the display says:

   <img src="doc\images\DISPLAY_Wifi-Config_success.jpeg" width=20% height=20%/>

   

   Otherwise it says:

   <img src="doc\images\DISPLAY_Wifi-Config_failed.jpeg" width=20% height=20%/>

   If the WiFi configuration failed the device starts from the beginning. The tracker also restarts and prints "Wifi-Config:  failed!" after a few minutes if the WiFi was not configured.

   

2. After the WiFi configuration is done the device needs a few seconds to initialize and afterwards it starts simulating the CWA-App.



## Using the tracker

You can press the "Boot" button to activate the display and press it again to deactivate the display. Most of the time the screen shows something like this.

<img src="doc\images\DISPLAY_Before_first_scan.jpeg" width=20% height=20%/>



The **time** is shown as marked.

<img src="doc\images\DISPLAY_Before_first_scan_marked1.jpeg" width=20% height=20%/>



The **action** is shown in the second line as marked.

<img src="doc\images\DISPLAY_Before_first_scan_marked2.jpeg" width=20% height=20%/>



Possible other **actions** are:

<img src="doc\images\xACTION_Advertise.png" width=21% height=21%/> <img src="doc\images\DISPLAY_Scan.jpeg" width=21% height=21%/> <img src="doc\images\DISPLAY_CWA_Update.jpeg" width=21% height=21%/>

* Advertise: The tracker announces that it is present.
* Scan: The tracker searches for present devices.
* CWA update: The tracker receives an update from the server.



In the third line you can see how many devices the tracker has seen during the last scan. Before the first scan there is a little dash instead of a number.

<img src="doc\images\DISPLAY_Before_first_scan_marked3.jpeg" width=20% height=20%/>



In the last line you can see your **exposure status**. The status tells you if a contact with a registered person was detected.

<img src="doc\images\DISPLAY_Before_first_scan_marked4.jpeg" width=20% height=20%/>



Possible other **exposure statuses** are:

<img src="doc\images\DISPLAY_Exposures_found.jpeg" width=20% height=20%/> <img src="doc\images\DISPLAY_No_exposures.jpeg" width=20% height=20%/> <img src="doc\images\DISPLAY_Update_failed1.jpeg" width=20% height=20%/>



While the tracker is being updated, the screen is permanently on and remains unchanged until the end of the update.

<img src="doc\images\DISPLAY_CWA_Update.jpeg" width=21% height=21%/>



After the update, the display shows the result and switches back to the previous display after a few seconds. One of the following results are possible.

<img src="doc\images\DISPLAY_Exposure_detected.jpeg" width=20% height=20%/> <img src="doc\images\DISPLAY_Update_failed2.jpeg" width=20% height=20%/> <img src="doc\images\DISPLAY_No_exposure_detected.jpeg" width=20% height=20%/> 

 

## Known issues

- [After initial configuration, WiFi credentials can not be changed without re-flashing the device.](https://github.com/sekassel/CoronaTrackerEsp32/issues/21)
- [WiFi credentials will be lost if the device looses power.](https://github.com/sekassel/CoronaTrackerEsp32/issues/22)
- [There is no official protocol for uploading infected keys.](https://github.com/sekassel/CoronaTrackerEsp32/issues/23)
- [Time displayed is always in CEST.](https://github.com/sekassel/CoronaTrackerEsp32/issues/24)
- [There is no real solution for full storage.](https://github.com/sekassel/CoronaTrackerEsp32/issues/25)
- Power consumption is a problem on small batteries.