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

import com.google.gson.annotations.SerializedName
import com.lunabeestudio.domain.model.DeviceParameterCorrection

internal class ApiDeviceParameterCorrection(
    @SerializedName("device_handset_model")
    val deviceHandsetModel: String,
    @SerializedName("tx_RSS_correction_factor")
    val txRSSCorrectionFactor: Double,
    @SerializedName("rx_RSS_correction_factor")
    val rxRSSCorrectionFactor: Double,
)

internal fun ApiDeviceParameterCorrection.toDomain() = DeviceParameterCorrection(
    deviceHandsetModel = deviceHandsetModel,
    txRssCorrectionFactor = txRSSCorrectionFactor,
    rxRssCorrectionFactor = rxRSSCorrectionFactor,
)