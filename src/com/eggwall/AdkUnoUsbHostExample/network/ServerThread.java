package com.eggwall.AdkUnoUsbHostExample.network;

import android.util.Log;
import com.eggwall.AdkUnoUsbHostExample.control.AccessoryService;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

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
public class ServerThread extends Thread {
    public static final String TAG = "ServerThread";

    /** Port at which the server listens. */
    final int port;
    /** Calling Router service for callbacks. */
    private final AccessoryService service;

    public ServerThread(AccessoryService s, int p) {
        service = s;
        port = p;
    }

    @Override
    public void run() {
        try {
            ServerSocket mSocket = new ServerSocket(port);
            Log.d(TAG, "I'm waiting here: " + getIpAddress() + ":" + mSocket.getLocalPort());
            while (true) {
                final Socket socket = mSocket.accept();
                // Start a thread to reply to the message.
                (new ReplyThread(service, socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            final String message = e.toString();
            service.handleError(message);
        }
    }

    /** Returns the current IP address. */
    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface anInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = anInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress anAddress = addresses.nextElement();
                    if (anAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: " + anAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }
        return ip;
    }
}
