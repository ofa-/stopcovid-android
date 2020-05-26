/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/17 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble.gatt

import android.bluetooth.BluetoothDevice
import android.content.Context

internal class BleGattClientProviderImpl(private val context: Context) : BleGattClientProvider {

    override fun fromDevice(bluetoothDevice: BluetoothDevice) = BleGattClientImpl(
        bluetoothDevice = bluetoothDevice,
        context = context
    )
}
