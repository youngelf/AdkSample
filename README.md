# AdkSample

Android ADK and Arduino code using USB host shield.

This code is hosted at:
https://github.com/youngelf/AdkSample.git

To try this code out, you need the following hardware:

1. An Android phone on which you can develop code.  This means you
   have enabled USB debugging and have your IDE setup to compile code.

2. An Arduino board: like Arduino UNO.

3. A USB Host Shield.

You need to download the following code:

1. This project using git clone.  This gives you both the Android half and the Arduino half.

2. [USB Host Library 2.0](https://github.com/felis/USB_Host_Shield_2.0).  You need version 2.0


## Compile the Android code
Compile
[AdkSample.ino](https://github.com/youngelf/AdkSample/blob/master/Arduino/AdkSample/AdkSample.ino)
using the Arduino IDE.

## Compile Android code

Import the cloned directory using  IntelliJ or Android Studio, compile
and install it on the attached Android device.



![Use this app by default](https://github.com/youngelf/AdkSample/blob/master/adk.png "Android dialog for accessory")

------
The code is released under the GNU General Public License, version 2
or higher at your choice.

------
This code builds upon an example by
Kristian Lauszus, TKJ Electronics 2012
Blog by Miguel:
http://allaboutee.com/2011/12/31/arduino-adk-board-blink-an-led-with-your-phone-code-and-explanation/

Icon by Double-J Design:
http://www.iconarchive.com/show/electronics-icons-by-double-j-design/PCB-icon.html