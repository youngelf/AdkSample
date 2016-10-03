package com.eggwall.AdkUnoUsbHostExample.control;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 *
 * <p>Controls the USB accessory and remains persistent.</p>
 * <p>The main </p>
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
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Handle the data provided by the accessory.
     * @param b
     */
    public void handleData(byte b) {

    }

    public void handleError(String message) {

    }
}
