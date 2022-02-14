/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/8 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.domain.model.smartwallet

import kotlin.time.Duration

data class SmartWalletValidityRuleWithAge<Rule>(
    val ageMin: Int,
    val ageMinExpireAfter: Duration,
    val rules: List<Rule>,
)