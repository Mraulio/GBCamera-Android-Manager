# GBCamera Android Manager
Android app to manage a Game Boy Camera gallery and communicate via usb serial with Arduino Printer Emulator and GBxCart to get images.

## About this project
This was my final project for my studies. I decided to make it related to the Game Boy Camera as it's something I enjoy and knew I would feel motivated to work on.
The main goal I wanted to achieve with this app was extracting the Game Boy Camera images using a [GBxCart](https://www.gbxcart.com/) device on an Android phone, as it's a fast solution to extracting the cartridge RAM and there was no app capable of doing that. This is mostly needed if you want to take many pics on the go with the GB Camera as it only has memory for 30 images.
Previous to that I used the [Arduino Printer Emulator](https://github.com/mofosyne/arduino-gameboy-printer-emulator), which is great but slow as it emulates printing from the Game Boy. The app is also compatible with that device.
Aside from that I wanted the app to look like a gallery of images, with the ability to modify the palette and frames, sharing the images and more features that have been added even after the presentation of the project.

## Official releases
Check the [Releases](https://github.com/Mraulio/GBCamera-Android-Manager/releases) link.
I usually share test releases in the [Game Boy Camera Club Discord](http://gameboy.camera).

## Main features
* Extracting GB Camera images via [GBxCart](https://www.gbxcart.com/).(Some recent phones are getting corrupted data, working on fixing that).
* Extracting GB Camera images via [Arduino Printer Emulator](https://github.com/mofosyne/arduino-gameboy-printer-emulator).
* Printing to real Game Boy Printer.
* Importing images, palettes and frames.
* Overlay frames.
* Creating custom palettes.
* Editing images (palettes, frames...).
* Sharing and downloading
* HDR and Animation.
* [Printer Paper Simulation](https://github.com/Raphael-Boichot/GameboyPrinterPaperSimulation).
* Compatibility with the [Gallery web app](https://herrzatacke.github.io/gb-printer-web/#/gallery).
* Creating backups.

## Usage
There is a [Wiki](https://github.com/Mraulio/GBCamera-Android-Manager/wiki/Gallery) where you can learn how to use the app. **To be updated for the latest v0.5 release**

## Building 
Clone this repo, prepare [Android Studio](https://developer.android.com/studio) and open the project. You can test the app in the emulator or build it (you need the adequate Android SDK) and get the apk file. You may need to modify _AppDatabase.java_ file and delete the _autoMigrations_ annotation.

## Libraries used
* [Gameboycameralib](https://github.com/KodeMunkie/gameboycameralib) to decode images, modified to work on Android and adapted to my needs.
* Serial communication: [https://github.com/mik3y/usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android).
* [Color picker](https://github.com/QuadFlask/colorpicker).
* [UnicodeExifInterface](https://github.com/ddyos/UnicodeExifInterface) to save Unicode chars in the User Comments Exif tag. Modified for recent Android APIs.
  
## Special thanks
* [Raphaël Boichot](https://github.com/Raphael-Boichot), for testing, French translations and help implementing some of his great projects as features, such as the [Printer Paper Simulation](https://github.com/Raphael-Boichot/GameboyPrinterPaperSimulation) and the [Arduino interface](https://github.com/Raphael-Boichot/PC-to-Game-Boy-Printer-interface) to print on real Game Boy Printer hardware. 
* [Andreas Hahn](https://github.com/HerrZatacke), for inspiration on his [Game Boy Camera Gallery](https://github.com/HerrZatacke/gb-printer-web), [base palettes](https://www.npmjs.com/package/gb-palettes), German translation and many constructive talks about the Game Boy Camera and compatibility with his gallery.
* [Lesserkuma](https://github.com/lesserkuma), for the great help with the [GBxCart communication protocol](https://github.com/lesserkuma/FlashGBX/blob/master/FlashGBX/hw_GBxCartRW.py) needed, sharing a [reduced version of the Python code](https://github.com/Mraulio/GBCamera-Android-Manager/blob/main/resourcesGithub/gbxcartrw_gbcamera.py) and guiding me through some issues I encountered.
* [Rafael Zenaro](https://github.com/zenaro147), for the Brazilian Portuguese translations and printing tests on real hardware.
* The [Game Boy Camera Club Discord](http://gameboy.camera) community for their continued support and inspiration on the amazing projects they create.
* [KuestenKeks](https://github.com/KuestenKeks), for German translation and feedback.

## License
As the project uses different libraries with different licenses I'll try to adapt it. Please contact me if there is any problem.
* Everything under the folder [gameboycameralib](https://github.com/Mraulio/GBCamera-Android-Manager/tree/main/app/src/main/java/com/mraulio/gbcameramanager/gameboycameralib) is licensed under [APGL](https://www.gnu.org/licenses/#AGPL).
* The modified [UnicodeExifInterface](https://github.com/Mraulio/GBCamera-Android-Manager/tree/main/app/src/main/java/com/mraulio/gbcameramanager/utils/UnicodeExifInterrace.java) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
* Everything else, including my own work is [MIT Licensed](https://opensource.org/license/mit/).

## Contact
You may find me in my Instagram account [@gbcameroid](https://www.instagram.com/gbcameroid/) and on the [Game Boy Camera Club Discord](http://gameboy.camera) as @Mraulio. There is a dedicated forum channel in the Discord server to this app. Feel free to reach out.

## Some useful info
  * [Gameboy 2BPP Graphics Format](https://www.huderlem.com/demos/gameboy2bpp.html) info.
  * Some info on the deleted images [here](https://www.insidegadgets.com/2017/07/11/learning-about-gameboy-camera-saves-and-converting-stored-images-to-bitmap/).
  * More info on deleted images and order in the sav [here](https://github.com/Raphael-Boichot/Inject-pictures-in-your-Game-Boy-Camera-saves#part-1-injecting-custom-pictures-into-the-save).
  * [Structure of the Game Boy Camera Save Data](https://funtography.online/wiki/Structure_of_the_Game_Boy_Camera_Save_Data)

## Some images from the app.
<img src="https://github.com/Mraulio/GBCamera-Android-Manager/assets/12874082/6505c36b-e362-415f-808a-cd4336169fd6" width="300"/>
<img src="https://github.com/Mraulio/GBCamera-Android-Manager/assets/12874082/b62c64bc-7aab-4994-ad5d-6dbaca2dc897" width="300"/>
<img src="https://github.com/Mraulio/GBCamera-Android-Manager/assets/12874082/b736c120-2356-4c3f-8794-96e899de6f5c" width="300"/>
<img src="https://github.com/Mraulio/GBCamera-Android-Manager/assets/12874082/1ba70428-4c9e-44d4-be32-3f6e6ce4fe9e" width="300"/>
<img src="https://github.com/Mraulio/GBCamera-Android-Manager/assets/12874082/da646099-3b16-4b01-9c1d-68e0fdc930de" width="300"/>
<img src="https://github.com/Mraulio/GBCamera-Android-Manager/assets/12874082/9e3e6a9d-5a51-471c-8494-04ec1e7eceb4" width="500"/>
<img src="https://github.com/Mraulio/GBCamera-Android-Manager/assets/12874082/be8eca44-7457-4941-b4dd-25fa205657f1" width="300"/>
<img src="https://github.com/Mraulio/GBCamera-Android-Manager/assets/12874082/434dd961-c4e7-4fa5-8538-ffee7986397a" width="300"/>
<img src="https://github.com/Mraulio/GBCamera-Android-Manager/assets/12874082/64a951cd-0ff5-414d-83cb-d860be1ae3f7" width="300"/>
