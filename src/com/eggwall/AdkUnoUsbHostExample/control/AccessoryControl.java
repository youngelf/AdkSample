package com.eggwall.AdkUnoUsbHostExample.control;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.eggwall.AdkUnoUsbHostExample.BuildConfig;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * <p>
 * Encapsulates all control of the USB accessory.  This class can be used inside a service
 * or in an Activity (as demonstrated here).  To create a control object, provide a context (Activity or Service context
 * is fine).  You must call the lifecycle methods in order.  First you must call {@link #onCreate()} immediately
 * after creating the object.  Through an activity, you must call {@link #onResume()} to get back a handle to the
 * Accessory.  You can query the control object to see if it is connected by calling {@link #isConnected}
 *</p>
 * <p>You can register a single {@link com.eggwall.AdkUnoUsbHostExample.control.AccessoryControl.ConnectedListener}</p>
 * to receive changes to connection status and messages from the Arduino device.
 * <p>Finally, to send messages, you can call {@link #sendMessage(byte)} with a byte that is transmitted to the
 * Arduino device which must be equipped with the correct code to handle the messages.</p>
 *
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
public class AccessoryControl {
    /** Set this to true to get more information at runtime */
    public static final boolean D = BuildConfig.DEBUG;
    /**
     * The tag under which all of the logging messages show.  To turn on, run this command on your terminal:
     * adb shell setprop log.tag.AccessoryControl VERBOSE
     */
    private static final String TAG = "AccessoryControl";
    /** A permission to request on older versions of the platform. */
    private static final String ACTION_USB_PERMISSION = "com.eggwall.AdkUnoUsbHostExample.USB_PERMISSION";

    /** Context of the parent activity. */
    private final Context mContext;
    /** A handle to the Arduino device */
    private UsbAccessory mAccessory;

    /** File descriptor over USB. */
    private ParcelFileDescriptor mFileDescriptor;
    /** Stream to read from */
    private FileInputStream mInputStream;
    /** Stream to write to */
    private FileOutputStream mOutputStream;
    /** System service that gives us access to USB accessory. */
    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;
    /** Thread that listens to incoming messages. */
    private ConnectedThread mConnectedThread;

    /** BroadcastReceiver that listens to USB device connectivity. */
    private BroadcastReceiver mUsbReceiver = null;
    /** Status of USB accessory connectivity. */
    private boolean mConnected;

    public AccessoryControl(Context context) {
        mContext = context;
    }

    public void onCreate() {
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        mUsbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    synchronized (this) {

                        UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                            openAccessory(accessory);
                        else {
                            if (D)
                                Log.d(TAG, "Permission denied for accessory " + accessory);
                        }
                        mPermissionRequestPending = false;
                    }
                } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                    UsbAccessory accessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (accessory != null && accessory.equals(mAccessory))
                        closeAccessory();
                }
            }
        };
        mContext.registerReceiver(mUsbReceiver, filter);

        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory))
                openAccessory(accessory);
            else {
                setConnectionStatus(false);
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory, mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            setConnectionStatus(false);
            if (D)
                Log.d(TAG, "mAccessory is null");
        }
        setConnectionStatus(true);
    }

    /** Returns true if a USB accessory is currently connected. */
    public boolean isConnected() {
        return mConnected;
    }

    /**
     * A listener that received connectivity changes and incoming messages.
     */
    public interface ConnectedListener {
        /** Delivered when a connection change takes place. */
        public void onConnectionChange(boolean connectionStatus);
        /** Delivered when a message is received from the Arduino device. */
        public void onMessageReceived(byte[] message);
    }

    /**
     * Sends a message to the Arduino device.
     * @param message to be sent
     * @return true if sending was successful.  This is no guarantee of delivery on the Arduino since there could also
     * be failure at the Arduino to receive the message.
     */
    public boolean sendMessage(byte message) {
        if (mOutputStream != null) {
            try {
                mOutputStream.write(message);
            } catch (IOException e) {
                if (D)
                    Log.e(TAG, "write failed", e);
                return false;
            }
        }
        return true;
    }

    private ConnectedListener mConnectionListener = null;

    /**
     * Registers a single listener.  If a listener was already registers, this method fails and returns false.  In such
     * cases, call {@link #unregisterConnectedListener()} first.
     * @param l a listener
     * @return true if a listener was registered.
     */
    public boolean registerConnectedListener(ConnectedListener l) {
        if (mConnectionListener != null) {
            // Did not register a listener, a listener exists
            return false;
        }
        // Registers a listener.
        mConnectionListener = l;
        return true;
    }

    /**
     * Unregisters any listener from receiving messages.
     */
    public void unregisterConnectedListener() {
        mConnectionListener = null;
    }

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);

            mConnectedThread = new ConnectedThread();
            mConnectedThread.start();

            setConnectionStatus(true);

            if (D)
                Log.d(TAG, "Accessory opened");
        } else {
            setConnectionStatus(false);
            if (D)
                Log.d(TAG, "Accessory open failed");
        }
    }

    private void setConnectionStatus(boolean connected) {
        // Only notify on changes to connection status.
        if (mConnected != connected) {
            if (mConnectionListener != null) {
                mConnectionListener.onConnectionChange(connected);
            }
            mConnected = connected;
        }
    }

    public void closeAccessory() {
        setConnectionStatus(false);

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Close all streams
        try {
            if (mInputStream != null)
                mInputStream.close();
        } catch (Exception ignored) {
        } finally {
            mInputStream = null;
        }
        try {
            if (mOutputStream != null)
                mOutputStream.close();
        } catch (Exception ignored) {
        } finally {
            mOutputStream = null;
        }
        try {
            if (mFileDescriptor != null)
                mFileDescriptor.close();
        } catch (IOException ignored) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
        if (mUsbReceiver != null) {
            mContext.unregisterReceiver(mUsbReceiver);
            mUsbReceiver = null;
        }
    }

    private class ConnectedThread extends Thread {
        byte[] buffer = new byte[1024];
        boolean running;

        ConnectedThread() {
            running = true;
            setConnectionStatus(true);
        }

        public void run() {
            while (running) try {
                int bytes = mInputStream.read(buffer);
                if (bytes > 3) { // The message is 4 bytes long
                    // Copy the response to a new buffer to return back
                    final byte[] response = new byte[bytes];
                    System.arraycopy(buffer, 0, response, 0, bytes);
                    if (mConnectionListener != null) {
                        mConnectionListener.onMessageReceived(response);
                    }
                }
            } catch (Exception ignore) {
                // Looks like we disconnected.
                setConnectionStatus(false);
            }
        }

        public void cancel() {
            running = false;
            setConnectionStatus(false);
        }
    }

}
