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

import com.lunabeestudio.domain.model.smartwallet.SmartWalletEligibilityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletEligibilityRuleWithAge
import com.lunabeestudio.domain.model.smartwallet.SmartWalletPositiveTestEligibilityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletRecoveryEligibilityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletVaccineEligibilityPivot
import com.lunabeestudio.stopcovid.extension.yearMonthDayUsParser
import timber.log.Timber

class ApiSmartWalletEligibilityPivot(
    val pivot: String,
    val rulesForAge: List<ApiRulesForAge>
) {
    class ApiRulesForAge(
        val ageMin: Int,
        val v: List<ApiVaccineEligibilityRule>?,
        val r: List<ApiRecoveryEligibilityRule>?,
        val p: List<ApiPositiveTestEligibilityRule>?,
    ) {
        fun toVaccineRule(): SmartWalletEligibilityRuleWithAge<SmartWalletVaccineEligibilityPivot.Rule> =
            SmartWalletEligibilityRuleWithAge(
                ageMin, v?.map(ApiVaccineEligibilityRule::toRule).orEmpty()
            )

        fun toRecoveryRule(): SmartWalletEligibilityRuleWithAge<SmartWalletRecoveryEligibilityPivot.Rule> =
            SmartWalletEligibilityRuleWithAge(
                ageMin, r?.map(ApiRecoveryEligibilityRule::toRule).orEmpty()
            )

        fun toPositiveTestRule(): SmartWalletEligibilityRuleWithAge<SmartWalletPositiveTestEligibilityPivot.Rule> =
            SmartWalletEligibilityRuleWithAge(
                ageMin, p?.map(ApiPositiveTestEligibilityRule::toRule).orEmpty()
            )
    }

    fun toSmartWalletEligibilityPivots(): List<SmartWalletEligibilityPivot> {
        val dateParser = yearMonthDayUsParser()

        val pivots = mutableListOf<SmartWalletEligibilityPivot>()

        try {
            pivots += SmartWalletVaccineEligibilityPivot(
                startDate = dateParser.parse(pivot)!!,
                ageMin = rulesForAge.minOf { it.ageMin },
                rulesWithAge = rulesForAge.map(ApiRulesForAge::toVaccineRule),
            )
        } catch (e: NullPointerException) {
            Timber.e("Unable to map vaccine $this")
        }

        try {
            pivots += SmartWalletRecoveryEligibilityPivot(
                startDate = dateParser.parse(pivot)!!,
                ageMin = rulesForAge.minOf { it.ageMin },
                rulesWithAge = rulesForAge.map(ApiRulesForAge::toRecoveryRule),
            )
        } catch (e: NullPointerException) {
            Timber.e("Unable to map recovery $this")
        }

        try {
            pivots += SmartWalletPositiveTestEligibilityPivot(
                startDate = dateParser.parse(pivot)!!,
                ageMin = rulesForAge.minOf { it.ageMin },
                rulesWithAge = rulesForAge.map(ApiRulesForAge::toPositiveTestRule),
            )
        } catch (e: NullPointerException) {
            Timber.e("Unable to positive test $this")
        }

        return pivots
    }
}