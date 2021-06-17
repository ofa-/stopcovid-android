/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/16 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

internal class BleGattClientException(message: String? = null, cause: Throwable? = null) :
    Exception(message, cause)

internal interface BleGattClient {
    val bluetoothDevice: BluetoothDevice

    suspend fun open()
    suspend fun close()

    suspend fun readRemoteRssi(): Int
    suspend fun discoverServices(): List<BluetoothGattService>
    suspend fun writeCharacteristic(characteristic: BluetoothGattCharacteristic): BluetoothGattCharacteristic
    suspend fun readCharacteristic(characteristic: BluetoothGattCharacteristic): BluetoothGattCharacteristic
}
