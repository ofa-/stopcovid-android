/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/18 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.model

import com.lunabeestudio.domain.model.Configuration
import kotlin.time.Duration.Companion.hours

internal class ApiMultipassConfig(
    val testMaxHours: Int,
    val maxDcc: Int,
    val minDcc: Int,
) {
    fun toMultipassConfig(isEnabled: Boolean): Configuration.MultipassConfig {
        return Configuration.MultipassConfig(
            isEnabled = isEnabled,
            testMaxDuration = testMaxHours.hours,
            maxDcc = maxDcc,
            minDcc = minDcc,
        )
    }
}