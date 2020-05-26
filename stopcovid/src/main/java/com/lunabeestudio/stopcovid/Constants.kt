/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid

import com.lunabeestudio.framework.ble.RobertBleSettings
import java.nio.ByteBuffer
import java.util.UUID

object Constants {
    object SharedPrefs {
        const val ON_BOARDING_DONE: String = "On.Boarding.Done"
    }

    object ServerConstant {
        val ACCEPTED_REPORT_CODE_LENGTH: List<Int> = listOf(6, 32)
    }

    object BleSettings : RobertBleSettings {
        override val serviceUuid: UUID = UUID.fromString(BuildConfig.SERVICE_UUID)
        override val servicePayloadCharacteristicUuid: UUID = UUID.fromString(BuildConfig.SERVICE_PAYLOAD_CHARACTERISTIC_UUID)
        override val backgroundServiceManufacturerDataIOS: ByteArray = BuildConfig.BACKGROUND_SERVICE_MANUFACTURER_DATA_IOS.toByteArray()
    }

    private fun String.toByteArray(): ByteArray {
        val split = split('.')
        val byteBuffer = ByteBuffer.allocate(split.size)
        split.forEach {
            byteBuffer.put(it.toByte())
        }
        return byteBuffer.array()
    }
}