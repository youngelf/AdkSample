package com.eggwall.AdkUnoUsbHostExample.network;

import android.util.Log;
import com.eggwall.AdkUnoUsbHostExample.control.AccessoryService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;

/**
 * <p>
 * For more information, see the page at:
 * https://github.com/youngelf/AdkSample
 * </p>
 * <p>
 * Adk Sample: connects Android and Arduino using Accessory Developer Kit (ADK)
 * Copyright (C) 2016  Vikram Aggarwal
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * </p>
*/
class ReplyThread extends Thread {

    private static final String TAG = "ReplyThread";
    private Socket socket;
    /** Counter for number of messages received. */
    int counter = 0;
    final AccessoryService service;
    public static final boolean VERBOSE_LOGGING = false;

    ReplyThread(AccessoryService ac, Socket socket) {
        service = ac;
        this.socket = socket;
    }

    @Override
    public void run() {
        String message = "";
        OutputStream outputStream;
        String msgReply = "Hello from Android, you are #" + counter + "\n";

        try {
            InputStream in = socket.getInputStream();
            // 80 chars should be good enough, though we'll get fewer.
            byte[] data = new byte[255];
            int readCount = in.read(data);
            if (VERBOSE_LOGGING) {
                Log.d(TAG, "Input had " + readCount + "bytes\n");
            }
            for (int i=0; i<readCount; i++) {
                if (VERBOSE_LOGGING) {
                    Log.d(TAG, "" + data[i]);
                }
                handleData(data[i]);
            }

            outputStream = socket.getOutputStream();
            PrintStream printStream = new PrintStream(outputStream);
            printStream.print(msgReply);
            printStream.close();
            in.close();

            if (VERBOSE_LOGGING) {
                message += "I replied: " + msgReply + "\n";
                Log.d(TAG, message);
            }
        } catch (IOException e) {
            e.printStackTrace();

            message += "Something wrong! " + e.toString() + "\n";
            // Here I should quit the original service.
            handleError(message);
        }
        if (VERBOSE_LOGGING) {
            Log.d(TAG, message);
        }
    }

    /**
     * Hands the data to the service.  This is provided as an example to translate incoming network bytes into
     * different control for the service.
     * @param b
     */
    private void handleData(byte b) {
        if (b == 'o' || b == 'O' || b == '1') {
            Log.d(TAG, "Turning On.");
            service.handleData((byte) 0x01);
            return;
        }
        if (b == 'f' || b == 'F' || b == '0') {
            Log.d(TAG, "Turning OFF.");
            service.handleData((byte) 0x00);
            return;
        }
    }

    /**
     * Hands the data to the service.  This is provided as an example to translate incoming network bytes into
     * different control for the service.
     * @param message The reason for the error
     */
    private void handleError(String message) {
        service.handleError(message);
    }
}
