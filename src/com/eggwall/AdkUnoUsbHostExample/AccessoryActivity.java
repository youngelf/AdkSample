package com.eggwall.AdkUnoUsbHostExample;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.eggwall.AdkUnoUsbHostExample.control.AccessoryControl;
import com.eggwall.AdkUnoUsbHostExample.control.AccessoryService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * An activity shown when the USB Accessory connects.  To show this activity, hit reset on your Arduino device.
 *
 * This is the Android half of the program that uses Accessory Developer Kit with an Arduino device with a USB host
 * shield.
 *
 * For more information, see the page at:
 * https://github.com/youngelf/AdkSample
 *
 *
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
 */
public class AccessoryActivity extends Activity {
    TextView mConnectionStatusView;
    TextView mResultView;

    private BroadcastReceiver mReceiver = null;

    /**
     * Send a command to the service that controls the accessory.
     * @param command
     */
    private void startServiceWithCommand(int command) {
        Intent s = new Intent(this, AccessoryService.class);
        s.putExtra(AccessoryService.REQUEST_EXTRA_NAME, command);
        startService(s);
    }

    private void receiveMessage(byte[] message) {
        long timer = ByteBuffer.wrap(message).order(ByteOrder.LITTLE_ENDIAN).getInt();
        mResultView.setText(Long.toString(timer));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mConnectionStatusView = (TextView) findViewById(R.id.connectionStatus);
        mResultView = (TextView) findViewById(R.id.resultView);

        // Start a service
        startServiceWithCommand(AccessoryService.REQUEST_START);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (TextUtils.equals(intent.getAction(), AccessoryService.BROADCAST_CONNECTIVITY)) {
                    // Connectivity change received.
                    final boolean connectionStatus = intent.getBooleanExtra(AccessoryService.BROADCAST_CONNECTIVITY_EXTRA, false);
                    // Switch back to the UI thread to modify the UI.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setConnectionStatus(connectionStatus);
                        }
                    });
                } else if (TextUtils.equals(intent.getAction(), AccessoryService.BROADCAST_MESSAGE)) {
                    // Message received
                    final byte[] message = intent.getByteArrayExtra(AccessoryService.BROADCAST_MESSAGE_EXTRA);
                    // Switch back to the UI thread to modify the UI.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            receiveMessage(message);
                        }
                    });
                }
            }
        };
        // Register our receiver
        IntentFilter i = new IntentFilter(AccessoryService.BROADCAST_CONNECTIVITY);
        registerReceiver(mReceiver, i);
        i = new IntentFilter(AccessoryService.BROADCAST_MESSAGE);
        registerReceiver(mReceiver, i);
    }

    @Override
    protected void onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Kill this activity.
        finish();
    }

    private void setConnectionStatus(boolean connected) {
        mConnectionStatusView.setText(connected ? "Connected" : "Disconnected");
    }

    /**
     * Handles button press.  This method is defined in the layout.xml file, and must be public and must have the
     * signature provided here
     * @param v the view element that was tapped.
     */
    public void blinkLED(View v) {
        int command = (byte) ((((ToggleButton) v).isChecked()) ?
                AccessoryService.REQUEST_LED_OFF : AccessoryService.REQUEST_LED_ON);
        startServiceWithCommand(command);
    }

}
