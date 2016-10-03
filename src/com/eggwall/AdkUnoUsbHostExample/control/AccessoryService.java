package com.eggwall.AdkUnoUsbHostExample.control;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.eggwall.AdkUnoUsbHostExample.AccessoryActivity;
import com.eggwall.AdkUnoUsbHostExample.R;
import com.eggwall.AdkUnoUsbHostExample.network.ServerThread;

/**
 *
 * <p>Controls the USB accessory and remains persistent.</p>
 * <p>The main issue with ADK and Android is that Android processes can die at any point.  Since
 * an activity cannot stay alive forever, it is much better to put the Accessory half of the code
 * in a persistent service.  This service demonstrates how that can be achieved.</p>
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
public class AccessoryService extends Service {
    /** Broadcast signifying message received */
    public static final String BROADCAST_MESSAGE = "Accessory.Message";
    public static final String BROADCAST_MESSAGE_EXTRA = "Accessory.Message.bytes";
    /** Broadcast signifying connectivity change received */
    public static final String BROADCAST_CONNECTIVITY = "Accessory.Message";
    public static final String BROADCAST_CONNECTIVITY_EXTRA = "Accessory.Message.status";
    private static final String TAG = "AccessoryService";
    /** The namespace of our application, used for qualifying local messages. */
    private static String NAMESPACE = "com.eggwall.AdkUnoUsbHostExample";

    /** When sending a request from the Activity, this is the extra to specify. */
    public static final String REQUEST_EXTRA_NAME = "request";
    /** Start the service */
    public static final int REQUEST_START = 0;

    // An example of what can be sent to this service
    /** Turn the LED ON */
    public static final int REQUEST_LED_ON = 101;
    /** Turn the LED OFF */
    public static final int REQUEST_LED_OFF = 100;

    /** Message to send Arduino to turn on LED */
    public static final byte LED_ON = (byte) 0x01;
    /** Message to send Arduino to turn off LED */
    public static final byte LED_OFF = (byte) 0x00;

    /** The SDK version */
    private final static int SDK = Build.VERSION.SDK_INT;
    /** The ID for the global notification we post. Insane!  Setting this to zero fails!  Read the documentation about
     * {@link Service#startForeground(int, android.app.Notification)}*/
    private final static int NOTIFICATION_ID = 42;

    /** Port at which this server listens for connections.  Set to 0 if you don't want to open a network socket. */
    final int PORT = 2021;

    /** The global manager for notifications */
    private NotificationManager mNotificationManager;
    /** The object that controls the Accessory Board */
    AccessoryControl mControl;

    /** To schedule reverting back to tablet and speaker */
    private AlarmManager mAlarmManager;

    /** The server thread responding to network requests. */
    private ServerThread mNetwork;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        if (mAlarmManager == null) {
            mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }

        // If we don't get an extra (impossible), start the service.
        int request = intent.getIntExtra(REQUEST_EXTRA_NAME, REQUEST_START);
        if (request == REQUEST_START) {
            if (mNetwork == null && mControl == null) {
                createAccessory();
                return START_NOT_STICKY;
            }
        } else {
            handleRequest(request);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mControl.closeAccessory();
        super.onDestroy();
    }

    DeviceListener mListener = null;
    /**
     * An example listener.  This listens to messages to print out the message received.  It also
     * updates connectivity status on the UI.  If these callbacks were not modifying the UI, you wouldn't
     * need to run on UI thread.
     */
    class DeviceListener implements AccessoryControl.ConnectedListener{
        DeviceListener() {
        }

        @Override
        public void onConnectionChange(final boolean connectionStatus) {
            final Intent i = new Intent(BROADCAST_CONNECTIVITY);
            i.putExtra(BROADCAST_CONNECTIVITY_EXTRA, connectionStatus);
            sendBroadcast(i);
        }

        @Override
        public void onMessageReceived(final byte[] message) {
            final Intent i = new Intent(BROADCAST_MESSAGE);
            i.putExtra(BROADCAST_MESSAGE_EXTRA, message);
            sendBroadcast(i);
        }
    }


    /**
     * Handle the request from the UI.  You can modify this method to account for what your UI is asking
     * to do, and translate this into accessory control messages, if needed.
     * @param request integers from the {@link #REQUEST_LED_OFF} or {@link #REQUEST_LED_ON} to control
     *                the LED.
     * @return
     */
    public boolean handleRequest(int request) {
        switch (request) {
            case REQUEST_LED_OFF:
                if (mControl != null) {
                    mControl.sendMessage(LED_ON);
                }
                break;
            case REQUEST_LED_ON:
                if (mControl != null) {
                    mControl.sendMessage(LED_OFF);
                }
                break;
            default:
                Log.wtf(TAG, "Sent the wrong message " + request, new Throwable());
                break;
        }
        return false;
    }

    /** Create a new accessory */
    private void createAccessory() {
        Log.d(TAG, "onStartCommand: Starting");
        mControl = new AccessoryControl(this);
        mControl.onCreate();
        setForegroundService();
        if (mNetwork == null && PORT > 0) {
            mNetwork = new ServerThread(this, PORT);
            mNetwork.start();
        }
        mListener = new DeviceListener();
        mControl.registerConnectedListener(mListener);
    }

    void removeNotificationAndStop() {
        mNotificationManager.cancel(NOTIFICATION_ID);
        stopSelf();
    }

    /**
     * The idea here is to set the notification so that the service can always run. However, ths is not
     * happening correctly right now.
     */
    private void setForegroundService() {
        final Intent showClock = new Intent(this, AccessoryActivity.class);
        final PendingIntent pending = PendingIntent.getActivity(this, 0, showClock, PendingIntent.FLAG_UPDATE_CURRENT);
        // Show the circuit board icon and the ADK name.
        final int icon = R.drawable.ic_launcher;
        final String title = "AdkSample";

        final Notification n;
        if (SDK >= 11) {
            // TODO: Set an icon that is the input and output conjoined
            final Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle(title)
                    .setSmallIcon(icon)
                    .setOngoing(true);
            builder.setContentIntent(pending);
            n = builder.build();
        } else {
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(title)
                    .setSmallIcon(icon)
                    .setOngoing(true);
            builder.setContentIntent(pending);
            n = builder.build();
        }
        Log.d(TAG, "Starting foreground");
        startForeground(NOTIFICATION_ID, n);
    }

    /**
     * Handle the data provided by the accessory.
     * @param b
     */
    public void handleData(byte b) {
        mControl.sendMessage(b);
    }

    public void handleError(String message) {
        Log.e(TAG, "handleError(SEVERE)" + message);
        removeNotificationAndStop();
    }
}
