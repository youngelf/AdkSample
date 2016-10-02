package com.eggwall.AdkUnoUsbHostExample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.eggwall.AdkUnoUsbHostExample.control.AccessoryControl;

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
    AccessoryControl mControl;
    DeviceListener mListener = null;

    /**
     * An example listener.  This listens to messages to print out the message received.  It also
     * updates connectivity status on the UI.  If these callbacks were not modifying the UI, you wouldn't
     * need to run on UI thread.
     */
    class DeviceListener implements AccessoryControl.ConnectedListener{
        final Activity mActivity;

        DeviceListener(Activity mActivity) {
            this.mActivity = mActivity;
        }

        @Override
        public void onConnectionChange(final boolean connectionStatus) {
            // Switch back to the UI thread to modify the UI.
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setConnectionStatus(connectionStatus);
                }
            });
        }

        @Override
        public void onMessageReceived(final byte[] message) {
            // Switch back to the UI thread to modify the UI.
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    receiveMessage(message);
                }
            });
        }
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
        mControl = new AccessoryControl(this);
        mControl.onCreate();
        mListener = new DeviceListener(this);
        mControl.registerConnectedListener(mListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        mControl.onResume();
    }

    @Override
    public void onBackPressed() {
        // Kill this activity.
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mControl.closeAccessory();
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
        byte buffer = (byte) ((((ToggleButton) v).isChecked()) ? 1 : 0);
        mControl.sendMessage(buffer);
    }

}
