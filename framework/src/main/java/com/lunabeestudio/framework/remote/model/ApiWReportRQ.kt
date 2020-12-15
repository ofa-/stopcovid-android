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
import com.lunabeestudio.domain.model.VenueQrType

internal class ApiWReportRQ(
    val visits: List<ApiWReportTokenRQ>,
) {
    companion object {
        fun fromVenueQrCodeList(venueQrCodeList: List<VenueQrCode>): ApiWReportRQ {
            val tokens = venueQrCodeList.map { venueQrCode ->
                ApiWReportTokenRQ(
                    timestamp = venueQrCode.ntpTimestamp.toString(),
                    qrCode = ApiWQrCodeRQ.fromVenueQrCode(venueQrCode)
                )
            }
            return ApiWReportRQ(tokens)
        }
    }
}

internal class ApiWReportTokenRQ(
    val timestamp: String,
    val qrCode: ApiWQrCodeRQ,
)

internal class ApiWQrCodeRQ(
    val type: String,
    val venueType: String,
    val venueCategory: Int?,
    val venueCapacity: Int?,
    val uuid: String,
) {
    companion object {
        fun fromVenueQrCode(venueQrCode: VenueQrCode): ApiWQrCodeRQ {
            return ApiWQrCodeRQ(
                type = when (venueQrCode.qrType) {
                    VenueQrType.STATIC -> "STATIC"
                    VenueQrType.DYNAMIC -> "DYNAMIC"
                },
                venueType = venueQrCode.venueType,
                venueCategory = venueQrCode.venueCategory,
                venueCapacity = venueQrCode.venueCapacity,
                uuid = venueQrCode.uuid
            )
        }
    }
}
