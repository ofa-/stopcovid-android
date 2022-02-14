/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/2/4 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.usecase

import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.domain.model.smartwallet.SmartWalletEligibilityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletEligibilityRuleWithAge
import com.lunabeestudio.domain.model.smartwallet.SmartWalletPositiveTestEligibilityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletRecoveryEligibilityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletVaccineEligibilityPivot
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.extension.isRecoveryOrTestPositive
import com.lunabeestudio.stopcovid.extension.parseOrNull
import com.lunabeestudio.stopcovid.extension.positiveTestOrRecoveryDate
import com.lunabeestudio.stopcovid.extension.vaccineDate
import com.lunabeestudio.stopcovid.extension.vaccineDose
import com.lunabeestudio.stopcovid.extension.vaccineMedicinalProduct
import com.lunabeestudio.stopcovid.extension.yearMonthDayUsParser
import com.lunabeestudio.stopcovid.extension.yearsOldAt
import com.lunabeestudio.stopcovid.manager.SmartWalletEligibilityManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import java.util.Calendar
import java.util.Date
import kotlin.time.DurationUnit

class ComputeDccEligibilityUseCase(
    private val robertManager: RobertManager,
    private val smartWalletEligibilityManager: SmartWalletEligibilityManager,
) {
    private val yearMonthDayUsParser = yearMonthDayUsParser()

    operator fun invoke(
        dcc: EuropeanCertificate,
        nowDate: Date = Date(),
    ): Date? {

        val birthdate = yearMonthDayUsParser.parseOrNull(dcc.greenCertificate.dateOfBirth) ?: return null
        val currentAge = birthdate.yearsOldAt(nowDate)

        val filterPivotType: List<SmartWalletEligibilityPivot>.() -> List<SmartWalletEligibilityPivot>
        val runPivot: (ruleWithAge: SmartWalletEligibilityRuleWithAge<out Any>) -> Date?

        when {
            dcc.type == WalletCertificateType.VACCINATION_EUROPE -> {
                val configuration = robertManager.configuration
                filterPivotType = {
                    filterIsInstance<SmartWalletVaccineEligibilityPivot>()
                }
                runPivot = { ruleWithAge ->
                    runVaccinePivotRules(
                        (ruleWithAge as? SmartWalletEligibilityRuleWithAge<SmartWalletVaccineEligibilityPivot.Rule>)?.rules.orEmpty(),
                        dcc,
                        configuration,
                    )
                }
            }
            dcc.type == WalletCertificateType.RECOVERY_EUROPE -> {
                filterPivotType = {
                    filterIsInstance<SmartWalletRecoveryEligibilityPivot>()
                }
                runPivot = { ruleWithAge ->
                    runRecoveryPivotRules(
                        (ruleWithAge as? SmartWalletEligibilityRuleWithAge<SmartWalletRecoveryEligibilityPivot.Rule>)?.rules.orEmpty(),
                        dcc,
                    )
                }
            }
            dcc.type == WalletCertificateType.SANITARY_EUROPE && dcc.greenCertificate.isRecoveryOrTestPositive -> {
                filterPivotType = {
                    filterIsInstance<SmartWalletPositiveTestEligibilityPivot>()
                }
                runPivot = { ruleWithAge ->
                    runPositiveTestPivotRules(
                        (ruleWithAge as? SmartWalletEligibilityRuleWithAge<SmartWalletPositiveTestEligibilityPivot.Rule>)?.rules.orEmpty(),
                        dcc,
                    )
                }
            }
            else -> return null
        }

        val applicablePivots = smartWalletEligibilityManager.smartWalletEligibilityPivot
            .filterPivotType()
            .filter { pivot ->
                (nowDate >= pivot.startDate && currentAge >= pivot.ageMin) ||
                    (birthdate.yearsOldAt(pivot.startDate) >= pivot.ageMin)
            }

        var currentEligibilityDate: Date? = null

        applicablePivots.forEach { pivot ->
            if (pivot.startDate > nowDate && pivot.startDate > currentEligibilityDate ?: Date(Long.MAX_VALUE)) {
                return@forEach
            }

            val ruleWithAge = pivot.rulesWithAge.firstOrNull { (ageMin) -> currentAge >= ageMin }
            val pivotEligibility = ruleWithAge?.let(runPivot)?.let { it ->
                maxOf(
                    it,
                    Calendar.getInstance().apply {
                        time = birthdate
                        add(Calendar.YEAR, ruleWithAge.ageMin)
                    }.time
                )
            }

            pivotEligibility?.let {
                currentEligibilityDate = minOf(it, currentEligibilityDate ?: Date(Long.MAX_VALUE))
            }
        }

        return currentEligibilityDate
    }

    private fun runVaccinePivotRules(
        rules: List<SmartWalletVaccineEligibilityPivot.Rule>,
        dcc: EuropeanCertificate,
        configuration: Configuration,
    ): Date? {
        dcc.greenCertificate.vaccineDate?.let { vaccineDate ->
            rules.forEach { rule ->
                val fullProducts = rule.products.toMutableList()

                if (fullProducts.remove("#AR")) {
                    fullProducts.addAll(configuration.smartWalletVacc?.ar.orEmpty())
                }
                if (fullProducts.remove("#JA")) {
                    fullProducts.addAll(configuration.smartWalletVacc?.ja.orEmpty())
                }
                if (fullProducts.remove("#AZ")) {
                    fullProducts.addAll(configuration.smartWalletVacc?.az.orEmpty())
                }

                if (
                    rule.dose?.current?.let { dcc.greenCertificate.vaccineDose?.first == rule.dose?.current } != false &&
                    rule.dose?.target?.let { dcc.greenCertificate.vaccineDose?.second == rule.dose?.target } != false &&
                    fullProducts.contains(dcc.greenCertificate.vaccineMedicinalProduct)
                ) {
                    return rule.eligibleAfter?.let { eligibleAfter ->
                        Calendar.getInstance().apply {
                            time = vaccineDate
                            add(Calendar.DAY_OF_YEAR, eligibleAfter.toInt(DurationUnit.DAYS))
                        }.time
                    }
                }
            }
        }

        return null
    }

    private fun runRecoveryPivotRules(
        rules: List<SmartWalletRecoveryEligibilityPivot.Rule>,
        dcc: EuropeanCertificate,
    ): Date? {
        dcc.greenCertificate.positiveTestOrRecoveryDate?.let { recoveryDate ->
            rules.forEach { rule ->
                if (
                    rule.prefix?.any {
                        dcc.greenCertificate.recoveryStatements?.firstOrNull()?.certificateIssuer?.startsWith(it) == true
                    } != false
                ) {
                    return rule.eligibleAfter?.let { eligibleAfter ->
                        Calendar.getInstance().apply {
                            time = recoveryDate
                            add(Calendar.DAY_OF_YEAR, eligibleAfter.toInt(DurationUnit.DAYS))
                        }.time
                    }
                }
            }
        }

        return null
    }

    private fun runPositiveTestPivotRules(
        rules: List<SmartWalletPositiveTestEligibilityPivot.Rule>,
        dcc: EuropeanCertificate,
    ): Date? {
        dcc.greenCertificate.positiveTestOrRecoveryDate?.let { positiveTestDate ->
            rules.forEach { rule ->
                if (
                    rule.prefix?.any {
                        dcc.greenCertificate.recoveryStatements?.firstOrNull()?.certificateIssuer?.startsWith(it) == true
                    } != false
                ) {
                    return rule.eligibleAfter?.let { eligibleAfter ->
                        Calendar.getInstance().apply {
                            time = positiveTestDate
                            add(Calendar.DAY_OF_YEAR, eligibleAfter.toInt(DurationUnit.DAYS))
                        }.time
                    }
                }
            }
        }

        return null
    }
}