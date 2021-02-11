/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble.gatt

import android.bluetooth.BluetoothDevice
import com.orange.proximitynotification.ble.BleSettings
import com.orange.proximitynotification.tools.Result

internal class RemoteRssiAndPayload(
    val rssi: Int,
    val payload: ByteArray
)

internal data class BleGattConnectionException(
    override val cause: Throwable? = null
) : Exception(cause)

internal interface BleGattManager {
    val settings: BleSettings

    interface Callback {
        enum class PayloadReceivedStatus {
            INVALID_PAYLOAD,
            UNKNOWN_DEVICE_REQUEST_RSSI_NEEDED,
            PAYLOAD_HANDLED
        }

        fun onPayloadReceived(device: BluetoothDevice, payload: ByteArray): PayloadReceivedStatus
    }

    fun start(callback: Callback): Boolean
    fun stop()

    suspend fun requestRemoteRssi(device: BluetoothDevice): Result<Int>
    suspend fun exchangePayload(
        device: BluetoothDevice,
        value: ByteArray,
        shouldReadRemotePayload: Boolean
    ): Result<RemoteRssiAndPayload?>
}