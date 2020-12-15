/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert

import android.content.Context
import com.lunabeestudio.domain.model.VenueQrCode

interface RobertApplication {
    val robertManager: RobertManager
    fun getAppContext(): Context
    fun refreshProximityService()
    fun atRiskDetected()
    fun warningAtRiskDetected()
    suspend fun sendClockNotAlignedNotification()
    fun refreshInfoCenter()
    fun getVenueQrCodeList(startTime: Long?): List<VenueQrCode>?
    fun clearVenueQrCodeList()
}