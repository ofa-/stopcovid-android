/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote.model

import com.lunabeestudio.domain.model.VenueQrCode

internal class ApiWReportClea(
    val pivotDate: Long,
    val visits: List<ApiWReportCleaVisit>,
) {
    companion object {
        fun fromVenueQrCodeList(pivotDate: Long, venueQrCodeList: List<VenueQrCode>): ApiWReportClea {
            val visits = venueQrCodeList.map { venueQrCode ->
                ApiWReportCleaVisit(
                    qrCodeScanTime = venueQrCode.ntpTimestamp,
                    qrCode = venueQrCode.base64URL
                )
            }
            return ApiWReportClea(pivotDate = pivotDate, visits = visits)
        }
    }
}

internal class ApiWReportCleaVisit(
    val qrCodeScanTime: Long,
    val qrCode: String,
)

