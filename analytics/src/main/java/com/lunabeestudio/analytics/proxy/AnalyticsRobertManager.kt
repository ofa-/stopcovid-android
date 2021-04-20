/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.analytics.proxy

import com.lunabeestudio.domain.model.AtRiskStatus
import com.lunabeestudio.domain.model.Configuration

interface AnalyticsRobertManager {
    val configuration: Configuration
    val atRiskStatus: AtRiskStatus?
    suspend fun getLocalProximityCount(): Int
}