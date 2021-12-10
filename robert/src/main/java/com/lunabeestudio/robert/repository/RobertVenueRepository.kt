/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/25/11 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.repository

import com.lunabeestudio.domain.model.VenueQrCode

interface RobertVenueRepository {
    suspend fun getVenuesQrCode(
        startNtpTimestamp: Long? = null,
        endNtpTimestamp: Long? = null,
    ): List<VenueQrCode>

    suspend fun clearAllData()
}