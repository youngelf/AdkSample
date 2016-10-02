package com.eggwall.AdkUnoUsbHostExample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;
import com.eggwall.AdkUnoUsbHostExample.control.AccessoryControl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class AccessoryActivity extends Activity {
    TextView mConnectionStatusView;
    TextView mResultView;
    AccessoryControl mControl;
    DeviceListener mListener = null;

    class DeviceListener implements AccessoryControl.ConnectedListener{
        final Activity mActivity;

        DeviceListener(Activity mActivity) {
            this.mActivity = mActivity;
        }

        @Override
        public void onConnectionChange(final boolean connectionStatus) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setConnectionStatus(connectionStatus);
                }
            });
        }

        @Override
        public void onMessageReceived(final byte[] message) {
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

    public void blinkLED(View v) {
        byte buffer = (byte) ((((ToggleButton) v).isChecked()) ? 1 : 0); // Read button
        mControl.sendMessage(buffer);
    }

}