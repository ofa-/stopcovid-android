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

internal data class RemoteRssiAndPayload(
    val rssi: Int,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteRssiAndPayload

        if (rssi != other.rssi) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rssi
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

internal sealed class BleGattManagerException(
    override val message: String? = null,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    data class ConnectionFailed(override val cause: Throwable? = null) :
        BleGattManagerException(cause = cause)

    data class OperationFailed(override val cause: Throwable? = null) :
        BleGattManagerException(cause = cause)

    class IncorrectPayloadService(override val message: String) :
        BleGattManagerException(message = message)
}

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