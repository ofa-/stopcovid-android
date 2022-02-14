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
import com.lunabeestudio.domain.model.smartwallet.SmartWalletPositiveTestValidityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletRecoveryValidityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletVaccineValidityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletValidity
import com.lunabeestudio.domain.model.smartwallet.SmartWalletValidityPivot
import com.lunabeestudio.domain.model.smartwallet.SmartWalletValidityRuleWithAge
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.extension.exemptionCertificateValidFrom
import com.lunabeestudio.stopcovid.extension.exemptionCertificateValidUntil
import com.lunabeestudio.stopcovid.extension.isCompleteVaccine
import com.lunabeestudio.stopcovid.extension.parseOrNull
import com.lunabeestudio.stopcovid.extension.positiveTestOrRecoveryDate
import com.lunabeestudio.stopcovid.extension.testResultIsNegative
import com.lunabeestudio.stopcovid.extension.vaccineDate
import com.lunabeestudio.stopcovid.extension.vaccineDose
import com.lunabeestudio.stopcovid.extension.vaccineMedicinalProduct
import com.lunabeestudio.stopcovid.extension.yearMonthDayUsParser
import com.lunabeestudio.stopcovid.extension.yearsOldAt
import com.lunabeestudio.stopcovid.manager.SmartWalletValidityManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import java.util.Calendar
import java.util.Date
import kotlin.time.Duration
import kotlin.time.DurationUnit

class ComputeDccValidityUseCase(
    private val robertManager: RobertManager,
    private val smartWalletValidityManager: SmartWalletValidityManager,
) {
    operator fun invoke(
        dcc: EuropeanCertificate,
        nowDate: Date = Date(),
    ): SmartWalletValidity? {
        val yearMonthDayUsParser = yearMonthDayUsParser()

        val dccDate = dcc.greenCertificate.vaccineDate ?: dcc.greenCertificate.positiveTestOrRecoveryDate ?: Date(dcc.timestamp)
        val birthdate = yearMonthDayUsParser.parseOrNull(dcc.greenCertificate.dateOfBirth) ?: return null
        val currentAge = birthdate.yearsOldAt(nowDate)

        val filterPivotType: List<SmartWalletValidityPivot<*>>.() -> List<SmartWalletValidityPivot<*>>
        val runRuleWithAge: (ruleWithAge: SmartWalletValidityRuleWithAge<out Any>) -> Pair<Duration?, Duration?>

        when {
            dcc.isCompleteVaccine() -> {
                val configuration = robertManager.configuration
                filterPivotType = {
                    filterIsInstance<SmartWalletVaccineValidityPivot>()
                }
                runRuleWithAge = { ruleWithAge ->
                    runVaccinePivotRules(
                        (ruleWithAge as? SmartWalletValidityRuleWithAge<SmartWalletVaccineValidityPivot.Rule>)?.rules.orEmpty(),
                        dcc,
                        configuration,
                    )
                }
            }
            dcc.type == WalletCertificateType.RECOVERY_EUROPE -> {
                filterPivotType = {
                    filterIsInstance<SmartWalletRecoveryValidityPivot>()
                }
                runRuleWithAge = { ruleWithAge ->
                    runRecoveryPivotRules(
                        (ruleWithAge as? SmartWalletValidityRuleWithAge<SmartWalletRecoveryValidityPivot.Rule>)?.rules.orEmpty(),
                        dcc,
                    )
                }
            }
            dcc.type == WalletCertificateType.SANITARY_EUROPE &&
                dcc.greenCertificate.testResultIsNegative == false -> {
                filterPivotType = {
                    filterIsInstance<SmartWalletPositiveTestValidityPivot>()
                }
                runRuleWithAge = { ruleWithAge ->
                    runPositiveTestPivotRules(
                        (ruleWithAge as? SmartWalletValidityRuleWithAge<SmartWalletPositiveTestValidityPivot.Rule>)?.rules.orEmpty(),
                        dcc,
                    )
                }
            }
            dcc.type == WalletCertificateType.EXEMPTION -> return SmartWalletValidity(
                start = dcc.greenCertificate.exemptionCertificateValidFrom,
                end = dcc.greenCertificate.exemptionCertificateValidUntil,
            )
            dcc.type == WalletCertificateType.MULTI_PASS -> return SmartWalletValidity(
                start = null,
                end = Date(dcc.expirationTime)
            )
            else -> return null
        }

        val applicablePivots = smartWalletValidityManager.smartWalletValidityPivot
            .filterPivotType()
            .filter { pivot ->
                (nowDate >= pivot.startDate && currentAge >= pivot.ageMin) ||
                    (birthdate.yearsOldAt(pivot.startDate) >= pivot.ageMin)
            }

        var minStart: Date? = null
        var maxEnd: Date? = null

        applicablePivots.forEach { pivot ->
            if (pivot.startDate > nowDate && pivot.startDate > maxEnd ?: Date(Long.MAX_VALUE)) {
                return@forEach
            }

            val ruleWithAge = pivot.rulesWithAge.firstOrNull { (ageMin) -> currentAge >= ageMin }
            val pivotValidity = ruleWithAge?.let(runRuleWithAge)?.let {
                getValidityDateRange(birthdate, pivot.startDate, ruleWithAge, it, dccDate)
            }

            pivotValidity?.start?.let {
                minStart = minOf(it, minStart ?: Date(Long.MAX_VALUE))
            }

            maxEnd = pivotValidity?.end
        }

        return minStart?.let { SmartWalletValidity(it, maxEnd) }
    }

    private fun runVaccinePivotRules(
        ruleWithAges: List<SmartWalletVaccineValidityPivot.Rule>,
        dcc: EuropeanCertificate,
        configuration: Configuration,
    ): Pair<Duration?, Duration?> {
        ruleWithAges.forEach { rule ->
            val fullProducts = rule.products?.toMutableList()?.apply {
                if (remove("#AR")) {
                    addAll(configuration.smartWalletVacc?.ar.orEmpty())
                }
                if (remove("#JA")) {
                    addAll(configuration.smartWalletVacc?.ja.orEmpty())
                }
                if (remove("#AZ")) {
                    addAll(configuration.smartWalletVacc?.az.orEmpty())
                }
            }?.map(String::uppercase).orEmpty()

            if (
                rule.dose?.current?.let { dcc.greenCertificate.vaccineDose?.first == rule.dose?.current } != false &&
                rule.dose?.target?.let { dcc.greenCertificate.vaccineDose?.second == rule.dose?.target } != false &&
                fullProducts.contains(dcc.greenCertificate.vaccineMedicinalProduct?.uppercase())
            ) {
                return rule.validityRange.startAfter to rule.validityRange.endAfter
            }
        }

        return null to null
    }

    private fun runRecoveryPivotRules(
        ruleWithAges: List<SmartWalletRecoveryValidityPivot.Rule>,
        dcc: EuropeanCertificate,
    ): Pair<Duration?, Duration?> {
        ruleWithAges.forEach { rule ->
            if (
                rule.issuerPrefix?.any {
                    dcc.greenCertificate.recoveryStatements?.firstOrNull()?.certificateIssuer?.startsWith(it) == true
                } != false
            ) {
                return rule.validityRange.startAfter to rule.validityRange.endAfter
            }
        }

        return null to null
    }

    private fun runPositiveTestPivotRules(
        rules: List<SmartWalletPositiveTestValidityPivot.Rule>,
        dcc: EuropeanCertificate,
    ): Pair<Duration?, Duration?> {
        rules.forEach { rule ->
            if (
                rule.testingCentrePrefix?.any {
                    dcc.greenCertificate.tests?.firstOrNull()?.testingCentre?.startsWith(it) == true
                } != false
            ) {
                return rule.validityRange.startAfter to rule.validityRange.endAfter
            }
        }

        return null to null
    }

    private fun getValidityDateRange(
        birthdate: Date,
        pivotDate: Date,
        ruleWithAge: SmartWalletValidityRuleWithAge<out Any>,
        ruleValidity: Pair<Duration?, Duration?>,
        dccDate: Date,
    ): SmartWalletValidity {

        val calendar = Calendar.getInstance()

        val startDate = ruleValidity.first?.let {
            calendar.apply {
                time = dccDate
                add(Calendar.DAY_OF_YEAR, it.toInt(DurationUnit.DAYS))
            }.time
        }

        val endDate = ruleValidity.second?.let {
            calendar.apply {
                time = dccDate
                add(Calendar.DAY_OF_YEAR, it.toInt(DurationUnit.DAYS))
            }.time
        }

        val expireMinTime = Calendar.getInstance().apply {
            time = birthdate
            add(Calendar.YEAR, ruleWithAge.ageMin)
        }.timeInMillis + ruleWithAge.ageMinExpireAfter.inWholeMilliseconds

        val ruleValidityDateRangeEnd = endDate?.time ?: Long.MAX_VALUE
        val validityDateRange = when {
            ruleValidityDateRangeEnd < expireMinTime ->
                SmartWalletValidity(startDate, Date(expireMinTime))
            ruleValidityDateRangeEnd < pivotDate.time ->
                SmartWalletValidity(startDate, pivotDate)
            else -> SmartWalletValidity(startDate, endDate)
        }

        return validityDateRange
    }
}