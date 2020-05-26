/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.ble.extension

import android.content.Context
import com.google.gson.Gson
import com.lunabeestudio.framework.ble.RobertBleSettings
import com.lunabeestudio.framework.ble.model.DeviceParameterCorrection
import com.orange.proximitynotification.ble.BleSettings

fun RobertBleSettings.toBleSettings(context: Context): BleSettings {
    val deviceParameterCorrectionString = context.assets
        .open("device_parameters_correction.json")
        .bufferedReader()
        .use { it.readText() }
    val deviceParameterCorrections =
        Gson().fromJson(deviceParameterCorrectionString,
            Array<DeviceParameterCorrection>::class.java)
    val deviceParameterCorrection = deviceParameterCorrections.firstOrNull {
        it.device_handset_model == android.os.Build.MODEL
    } ?: DeviceParameterCorrection("", 6.0, -2.0)
    return BleSettings(serviceUuid = serviceUuid,
        servicePayloadCharacteristicUuid = servicePayloadCharacteristicUuid,
        backgroundServiceManufacturerDataIOS = backgroundServiceManufacturerDataIOS,
        rxCompensationGain = deviceParameterCorrection.rx_RSS_correction_factor.toInt(),
        txCompensationGain = deviceParameterCorrection.tx_RSS_correction_factor.toInt())
}