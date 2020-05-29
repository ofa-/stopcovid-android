/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble

import com.orange.proximitynotification.ProximityPayload

data class BlePayload(
    val proximityPayload: ProximityPayload,
    val version: Int,
    val txPowerLevel: Int
) {

    companion object {

        private const val PROXIMITY_PAYLOAD_OFFSET = 0
        private const val PROXIMITY_PAYLOAD_SIZE = ProximityPayload.SIZE
        private const val VERSION_OFFSET = PROXIMITY_PAYLOAD_OFFSET + PROXIMITY_PAYLOAD_SIZE
        private const val VERSION_SIZE = 1
        private const val TX_POWER_LEVEL_OFFSET = VERSION_OFFSET + 1
        private const val TX_POWER_LEVEL_SIZE = 1

        private const val SIZE = PROXIMITY_PAYLOAD_SIZE + VERSION_SIZE + TX_POWER_LEVEL_SIZE

        fun from(data: ByteArray): BlePayload {

            require(data.size >= SIZE) { "Expecting a byte array of $SIZE bytes. Got ${data.size}." }

            return BlePayload(
                proximityPayload = ProximityPayload(
                    data.copyOfRange(
                        PROXIMITY_PAYLOAD_OFFSET,
                        PROXIMITY_PAYLOAD_SIZE
                    )
                ),
                version = data[VERSION_OFFSET].toInt(),
                txPowerLevel = data[TX_POWER_LEVEL_OFFSET].toInt()
            )
        }

        fun fromOrNull(data: ByteArray): BlePayload? {
            return try {
                from(data)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    fun toByteArray(): ByteArray {
        val result = ByteArray(SIZE)

        proximityPayload.data.copyInto(result, PROXIMITY_PAYLOAD_OFFSET)
        result[VERSION_OFFSET] = version.toByte()
        result[TX_POWER_LEVEL_OFFSET] = txPowerLevel.toByte()
        return result
    }
}