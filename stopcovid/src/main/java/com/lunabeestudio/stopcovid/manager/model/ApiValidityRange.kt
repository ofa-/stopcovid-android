/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/7 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager.model

import com.lunabeestudio.domain.model.smartwallet.SmartWalletValidityRange
import kotlin.time.Duration.Companion.days

class ApiValidityRange(
    val e: Int?,
    val s: Int?,
) {
    fun toSmartWalletValidityRange(): SmartWalletValidityRange {
        return SmartWalletValidityRange(
            startAfter = s?.days,
            endAfter = e?.days,
        )
    }
}