/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/4 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager.model

import com.lunabeestudio.domain.model.smartwallet.SmartWalletRecoveryEligibilityPivot
import kotlin.time.Duration.Companion.days

class ApiRecoveryEligibilityRule(
    val elg: Int?,
    val prefix: List<String>,
) {
    fun toRule(): SmartWalletRecoveryEligibilityPivot.Rule {
        return SmartWalletRecoveryEligibilityPivot.Rule(
            eligibleAfter = elg?.days,
            prefix = prefix,
        )
    }
}