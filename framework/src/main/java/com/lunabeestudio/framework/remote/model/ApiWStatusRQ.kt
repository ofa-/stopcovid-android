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

internal class ApiWStatusRQ(
    val visitTokens: List<ApiWStatusTokenRQ>,
) {
    companion object {
        fun fromVenueQrCodeList(venueQrCodeList: List<VenueQrCode>): ApiWStatusRQ {
            val tokens = venueQrCodeList.map { venueQrCode ->
                ApiWStatusTokenRQ(
                    type = when (venueQrCode.qrType) {
                        VenueQrType.STATIC -> "STATIC"
                        VenueQrType.DYNAMIC -> "DYNAMIC"
                    },
                    payload = venueQrCode.payload,
                    timestamp = venueQrCode.ntpTimestamp.toString()
                )
            }
            return ApiWStatusRQ(tokens)
        }
    }
}

internal class ApiWStatusTokenRQ(
    val type: String,
    val payload: String,
    val timestamp: String,
)