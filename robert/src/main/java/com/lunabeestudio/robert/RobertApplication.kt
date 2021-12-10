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
import com.lunabeestudio.analytics.proxy.AnalyticsInfosProvider

interface RobertApplication : AnalyticsInfosProvider {
    val robertManager: RobertManager
    var isAppInForeground: Boolean
    fun getAppContext(): Context
    fun refreshProximityService()
    fun notifyAtRiskLevelChange(prevRiskLevel: Float)
    fun atRiskLevelChange(prevRiskLevel: Float)
    suspend fun sendClockNotAlignedNotification()
    fun refreshInfoCenter()
}