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

import com.lunabeestudio.domain.model.smartwallet.SmartWalletPositiveTestValidityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletRecoveryValidityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletValidityRuleWithAge
import com.lunabeestudio.domain.model.smartwallet.SmartWalletVaccineValidityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletValidityPivot
import com.lunabeestudio.stopcovid.extension.yearMonthDayUsParser
import timber.log.Timber
import kotlin.time.Duration.Companion.days

class ApiSmartWalletValidityPivot(
    val pivot: String,
    val rulesForAge: List<ApiRulesForAge>,
) {

    class ApiRulesForAge(
        val ageMin: Int,
        val ageMinExpDays: Int,
        val v: List<ApiVaccineValidityRule>?,
        val r: List<ApiRecoveryValidityRule>?,
        val p: List<ApiPositiveTestValidityRule>?,
    ) {
        fun toVaccineRule(): SmartWalletValidityRuleWithAge<SmartWalletVaccineValidityPivot.Rule> =
            SmartWalletValidityRuleWithAge(
                ageMin, ageMinExpDays.days, v?.map(ApiVaccineValidityRule::toRule).orEmpty()
            )

        fun toRecoveryRule(): SmartWalletValidityRuleWithAge<SmartWalletRecoveryValidityPivot.Rule> =
            SmartWalletValidityRuleWithAge(
                ageMin, ageMinExpDays.days, r?.map(ApiRecoveryValidityRule::toRule).orEmpty()
            )

        fun toPositiveTestRule(): SmartWalletValidityRuleWithAge<SmartWalletPositiveTestValidityPivot.Rule> =
            SmartWalletValidityRuleWithAge(
                ageMin, ageMinExpDays.days, p?.map(ApiPositiveTestValidityRule::toRule).orEmpty()
            )
    }

    fun toSmartWalletValidityPivots(): List<SmartWalletValidityPivot<out Any>> {
        val dateParser = yearMonthDayUsParser()

        val pivots = mutableListOf<SmartWalletValidityPivot<out Any>>()

        try {
            pivots += SmartWalletVaccineValidityPivot(
                startDate = dateParser.parse(pivot)!!,
                ageMin = rulesForAge.minOf { it.ageMin },
                rulesWithAge = rulesForAge.map(ApiRulesForAge::toVaccineRule),
            )
        } catch (e: NullPointerException) {
            Timber.e("Unable to map vaccine $this")
        }

        try {
            pivots += SmartWalletPositiveTestValidityPivot(
                startDate = dateParser.parse(pivot)!!,
                ageMin = rulesForAge.minOf { it.ageMin },
                rulesWithAge = rulesForAge.map(ApiRulesForAge::toPositiveTestRule),
            )
        } catch (e: NullPointerException) {
            Timber.e("Unable to map positive test $this")
        }

        try {
            pivots += SmartWalletRecoveryValidityPivot(
                startDate = dateParser.parse(pivot)!!,
                ageMin = rulesForAge.minOf { it.ageMin },
                rulesWithAge = rulesForAge.map(ApiRulesForAge::toRecoveryRule),
            )
        } catch (e: NullPointerException) {
            Timber.e("Unable to map recovery $this")
        }

        return pivots
    }
}