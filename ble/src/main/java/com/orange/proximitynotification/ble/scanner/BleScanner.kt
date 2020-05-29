/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble.scanner

import android.bluetooth.BluetoothDevice
import com.orange.proximitynotification.ble.BleSettings
import java.util.Date

data class BleScannedDevice(
    val device: BluetoothDevice,
    val rssi: Int = 0,
    val serviceData: ByteArray? = null,
    val timestamp: Date = Date()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleScannedDevice

        if (device != other.device) return false
        if (rssi != other.rssi) return false
        if (serviceData != null) {
            if (other.serviceData == null) return false
            if (!serviceData.contentEquals(other.serviceData)) return false
        } else if (other.serviceData != null) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + rssi
        result = 31 * result + (serviceData?.contentHashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

interface BleScanner {

    val settings: BleSettings

    interface Callback {
        fun onResult(results: List<BleScannedDevice>)
        fun onError(errorCode: Int)
    }

    fun start(callback: Callback)
    fun stop()
}