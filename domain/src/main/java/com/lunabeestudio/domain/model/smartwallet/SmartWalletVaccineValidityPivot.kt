/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/4 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.domain.model.smartwallet

import java.util.Date

data class SmartWalletVaccineValidityPivot(
    override val startDate: Date,
    override val ageMin: Int,
    override val rulesWithAge: List<SmartWalletValidityRuleWithAge<Rule>>,
) : SmartWalletValidityPivot<SmartWalletVaccineValidityPivot.Rule> {
    data class Rule(
        val dose: SmartWalletDose?,
        val products: List<String>?,
        val validityRange: SmartWalletValidityRange,
    )
}