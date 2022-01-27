/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/14/6 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.Constants
import dgca.verifier.app.decoder.model.CertificateType
import dgca.verifier.app.decoder.model.GreenCertificate
import java.text.DateFormat
import java.util.Date
import java.util.Locale

val GreenCertificate.certificateType: WalletCertificateType
    get() {
        return when (getType()) {
            CertificateType.VACCINATION -> WalletCertificateType.VACCINATION_EUROPE
            CertificateType.RECOVERY -> WalletCertificateType.RECOVERY_EUROPE
            CertificateType.TEST -> WalletCertificateType.SANITARY_EUROPE
            CertificateType.EXEMPTION -> WalletCertificateType.EXEMPTION
            else -> WalletCertificateType.DCC_LIGHT
        }
    }

fun GreenCertificate.formattedDateOfBirthDate(dateFormat: DateFormat): String =
    yearMonthDayUsParser().parseOrNull(dateOfBirth)?.let(dateFormat::format) ?: dateOfBirth

val GreenCertificate.countryCode: String?
    get() = when (certificateType) {
        WalletCertificateType.SANITARY,
        WalletCertificateType.VACCINATION -> null
        WalletCertificateType.DCC_LIGHT -> Locale.FRANCE.country
        WalletCertificateType.SANITARY_EUROPE,
        WalletCertificateType.VACCINATION_EUROPE,
        WalletCertificateType.RECOVERY_EUROPE,
        WalletCertificateType.EXEMPTION,
        WalletCertificateType.MULTI_PASS -> getIssuingCountry()
    }

// French Polynesia, New Caledonia, Wallis and Futuna, and Saint Pierre and Miquelon are French territory
val GreenCertificate.isFrench: Boolean
    get() = listOf(Locale.FRANCE.country, "NC", "WF", "PM", "PF").contains(this.countryCode?.uppercase())

val GreenCertificate.vaccineMedicinalProduct: String?
    get() = vaccinations?.lastOrNull()?.medicinalProduct?.trim()

val GreenCertificate.vaccineDate: Date?
    get() = vaccinations?.lastOrNull()?.dateOfVaccination?.let(yearMonthDayUsParser()::parseOrNull)

val GreenCertificate.vaccineDateForceTimeZone: Date?
    get() = vaccinations?.lastOrNull()?.dateOfVaccination?.let(yearMonthDayUsParserForceTimeZone()::parseOrNull)

val GreenCertificate.vaccineDose: Pair<Int, Int>?
    get() {
        val vaccination = vaccinations?.lastOrNull() ?: return null
        return vaccination.doseNumber to vaccination.totalSeriesOfDoses
    }

val GreenCertificate.testType: String?
    get() = tests?.lastOrNull()?.typeOfTest?.trim()

val GreenCertificate.isRecovery: Boolean
    get() = (testResultIsNegative == false) || certificateType == WalletCertificateType.RECOVERY_EUROPE

val GreenCertificate.testResultIsNegative: Boolean?
    get() = tests?.lastOrNull()?.isResultNegative()

val GreenCertificate.testResultCode: String?
    get() = tests?.lastOrNull()?.isResultNegative()?.let { isNegative ->
        if (isNegative) "negative" else "positive"
    }

val GreenCertificate.testDateTimeOfCollection: Date?
    get() = tests?.lastOrNull()?.dateTimeOfCollection
        ?.let(::parseToOffsetDateTimeOrNull)?.toInstant()?.let { Date.from(it) }

val GreenCertificate.positiveTestOrRecoveryDate: Date?
    get() = testDateTimeOfCollection.takeIf { this.testResultIsNegative != true } ?: recoveryDateOfFirstPositiveTest

val GreenCertificate.recoveryDateOfFirstPositiveTest: Date?
    get() = recoveryStatements?.lastOrNull()?.dateOfFirstPositiveTest?.let(yearMonthDayUsParser()::parseOrNull)

val GreenCertificate.recoveryDateOfFirstPositiveTestForceTimeZone: Date?
    get() = recoveryStatements?.lastOrNull()?.dateOfFirstPositiveTest?.let(yearMonthDayUsParserForceTimeZone()::parseOrNull)

val GreenCertificate.exemptionCertificateValidFrom: Date?
    get() = exemptionStatement?.certificateValidFrom?.let(yearMonthDayUsParser()::parseOrNull)

val GreenCertificate.exemptionCertificateValidUntil: Date?
    get() = exemptionStatement?.certificateValidUntil?.let(yearMonthDayUsParser()::parseOrNull)

val GreenCertificate.manufacturer: String?
    get() = when {
        vaccinations?.lastOrNull() != null -> vaccinations?.lastOrNull()?.manufacturer
        tests?.lastOrNull() != null -> tests?.lastOrNull()?.testNameAndManufacturer
        else -> null
    }

val GreenCertificate.isAutoTest: Boolean
    get() = manufacturer == Constants.Certificate.MANUFACTURER_AUTOTEST