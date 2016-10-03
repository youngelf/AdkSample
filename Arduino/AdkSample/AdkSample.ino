/*
 * Arduino half of ADK sample code.
 * 
 * This code is a fully functional Arduino UNO code that used a USB Host Shield (from funduino) to
 * communicate back and forth with an Android phone.
 * 
 * The circuit for it is very simple:  Connect the positive end of an LED to Pin 3 with a 1k resistor
 * to limit current.  Connect the other end to Ground.  The Ground pin should be negative on the LED
 * (longer internal metal piece in the LED is the negative pole).
 * Pin 3 ---> LED (positive end) ---> 1k resistor ---> Ground.
 * 
 * A breadboard is excellent for such a simple circuit.
 * 
 * 
 * For more information, including the Android half of code, visit:
 * https://github.com/youngelf/AdkSample
 * 
 * This code uses the USB Host Shield library Version 2.0 available at:
 * https://github.com/felis/USB_Host_Shield_2.0
 * It should show up as Sketch -> Include Library -> 'Usb Host Shield Library 2.0'
 * 
 * If you see 'USB_Host_Shield, it won't work.  You need version 2.0 of the library
 * to resolve adk.h and the Usb class.
 * 
 *      
    ADK Sample: communicate between Android and Arduino using USB Host Shield.
    Copyright (C) 2016  Vikram Aggarwal

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

  */


#include <adk.h>

// Pin assigned for LED output.
// Use Pin 3 (Fourth from bottom on the digital output section)
// Do not use pins 8-13, since they are used by the USB Host Shield.  And you want to avoid
// using Input/Output pins which are pin 0 and pin 1.  That still leaves all analog pins and
// pins 2-7 for digital input output on an Arduino UNO.
// You can use an Arduino Mega if you want more input/output capabilities.
// You could also choose to remove the Serial lines below giving you two more pins.
//
//
// From the datasheet of the USB Host Shield:
// http://www.thaieasyelec.com/downloads/EFDV521/Datasheet_Keyes_USBHostShield.pdf
// Digital I/O pins 8-13. In this group, the shield in its default configuration uses pins 9 and 10 for
// INT and SS interface signals. However, standard-sized Arduino boards, such as Duemilanove and
// UNO have SPI signals routed to pins 11-13 in addition to ICSP connector, therefore shields using
// pins 11-13 combined with standard-sized Arduinos will interfere with SPI. INT and SS signals can
// be re-assigned to other pins (see below); SPI signals cannot.

#define LED 3


// Turn this to 1 to enable verbose serial logging.
// #define VERBOSE 1

// Satisfy IDE, which only needs to see the include statment in the ino.
#ifdef dobogusinclude
#include <spi4teensy3.h>
#include <SPI.h>
#endif

// The object that communicates with the Android
USB Usb;

// These *must* match res/xml/accessory_filter.xml values.  That file should look like this:
// <?xml version="1.0" encoding="utf-8"?>
// <resources>
//    <usb-accessory manufacturer="Eggwall" model="AdkSample" version="1.0" />
// </resources>
//
// You can change the model name to allow the same phone to have more than one accessory.
// Each Android app will target a single accessory, and the setup here will start the
// correct app based on the Manufacturer and model name.
ADK adk(&Usb, "Eggwall", // Manufacturer Name
              "AdkSample", // Model Name
              "Sample ADK sketch for USB Host Shield", // Description (user-visible string)
              "1.0", // Version
              "https://github.com/youngelf/AdkSample", // URL (web page to visit if no installed apps support the accessory)
              "1"); // Serial Number (optional)

uint32_t timer;
bool connected;

void setup() {
  // Allow Serial monitor at 115200 bps
  Serial.begin(115200);

// Wait for serial port to connect - used on Leonardo, Teensy and other boards with built-in USB CDC serial connection
#if !defined(__MIPSEL__)
  while (!Serial);
#endif

  // The USB Host shield was not able to initialize.
  // This problem can happen for a variety of reasons:
  // 1. The power to the board is insufficient.
  // 2. The power is un-necessary, try just a USB connection to Arduino,
  //    or just the 9V power connection.
  // 3. Your Arduino board is faulty: try using a known-good board.
  // 4. Your Host shield is busted.
  if (Usb.Init() == -1) {
    Serial.print("\r\nOSCOKIRQ failed to assert");
    while (1); // halt
  }

  // Use this pin for output.
  pinMode(LED, OUTPUT);

  // Debugging only.
  Serial.print("\r\nArduino Sample ADK code started");
  Serial.print("\r\nhttps://github.com/youngelf/AdkSample");
  Serial.print("\r\nWaiting for Android..  If this persists, reset the Arduino.");
}

void loop() {
  Usb.Task();

  if (adk.isReady()) {
    if (!connected) {
      connected = true;
      Serial.print(F("\r\nConnected to Android"));
    }

    uint8_t msg[1];
    uint16_t len = sizeof(msg);
    uint8_t rcode = adk.RcvData(&len, msg);
    if (rcode && rcode != hrNAK) {
      // Data received
#ifdef VERBOSE
      Serial.print(F("\r\nData rcv: "));
#endif
      Serial.print(rcode, HEX);
    } else if (len > 0) {
      Serial.print(F("\r\nData Packet: "));
      Serial.print(msg[0]);
      digitalWrite(LED, msg[0] ? HIGH : LOW);
    }

    // Send data every second to avoid overloading the connection.
    if (millis() - timer >= 1000) {
      timer = millis();
      rcode = adk.SndData(sizeof(timer), (uint8_t*)&timer);
      if (rcode && rcode != hrNAK) {
        // Data sent.
#ifdef VERBOSE
        Serial.print(F("\r\nData send: "));
        Serial.print(rcode, HEX);
#endif
      } else if (rcode != hrNAK) {
#ifdef VERBOSE
        Serial.print(F("\r\nTimer: "));
        Serial.print(timer);
#endif
      }
    }
  } else {
    if (connected) {
      connected = false;
      Serial.print(F("\r\nDisconnected from Android"));
      digitalWrite(LED, LOW);
    }
  }
}
