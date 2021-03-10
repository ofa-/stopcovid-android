/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/17/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.model

import android.os.Build
import com.google.gson.annotations.SerializedName
import com.lunabeestudio.domain.model.Calibration
import com.lunabeestudio.domain.model.CalibrationEntry
import com.lunabeestudio.robert.RobertConstant

internal class ApiCalibration(
    @SerializedName("version")
    val version: Int,
    @SerializedName("calibration")
    val calibration: List<Map<String, List<Double>>>,
)

internal fun ApiCalibration.toDomain(): Calibration {
    val defaultAndDeviceEntries = calibration.mapNotNull { entry ->
        entry.keys.firstOrNull()?.let { key ->
            val tx = entry[key]?.get(0)
            val rx = entry[key]?.get(1)
            val keyIsValid = key == Build.MODEL || key == RobertConstant.DEFAULT_CALIBRATION_KEY
            if (keyIsValid && tx != null && rx != null) {
                CalibrationEntry(key, tx, rx)
            } else {
                null
            }
        }
    }

    return Calibration(
        version = version,
        entry = defaultAndDeviceEntries.firstOrNull { it.deviceHandsetModel == Build.MODEL } ?: defaultAndDeviceEntries.firstOrNull()
    )
}