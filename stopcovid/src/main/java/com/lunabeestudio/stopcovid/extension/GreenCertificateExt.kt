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
import dgca.verifier.app.decoder.model.GreenCertificate
import dgca.verifier.app.decoder.model.RecoveryStatement
import dgca.verifier.app.decoder.model.Test
import dgca.verifier.app.decoder.model.Vaccination
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val GreenCertificate.certificateType: WalletCertificateType?
    get() {
        val dccType = tests?.lastOrNull() ?: recoveryStatements?.lastOrNull() ?: vaccinations?.lastOrNull()
        return when (dccType) {
            is Test -> WalletCertificateType.SANITARY_EUROPE
            is RecoveryStatement -> WalletCertificateType.RECOVERY_EUROPE
            is Vaccination -> WalletCertificateType.VACCINATION_EUROPE
            else -> null
        }
    }

fun GreenCertificate.formattedDateOfBirthDate(dateFormat: DateFormat): String =
    yearMonthDayUsParser().parseOrNull(dateOfBirth)?.let(dateFormat::format) ?: dateOfBirth

val GreenCertificate.countryCode: String?
    get() = when {
        vaccinations?.lastOrNull() != null -> vaccinations?.lastOrNull()?.countryOfVaccination
        tests?.lastOrNull() != null -> tests?.lastOrNull()?.countryOfVaccination
        recoveryStatements?.lastOrNull() != null -> recoveryStatements?.lastOrNull()?.countryOfVaccination
        else -> null
    }

// French Polynesia, New Caledonia, Wallis and Futuna, and Saint Pierre and Miquelon are French territory
val GreenCertificate.isFrench: Boolean
    get() = listOf(Locale.FRANCE.country, "NC", "WF", "PM", "PF").contains(this.countryCode)

val GreenCertificate.vaccineMedicinalProduct: String?
    get() = vaccinations?.lastOrNull()?.medicinalProduct?.trim()

val GreenCertificate.vaccineDate: Date?
    get() = vaccinations?.lastOrNull()?.dateOfVaccination?.let(yearMonthDayUsParser()::parseOrNull)

val GreenCertificate.vaccineDose: Pair<Int, Int>?
    get() {
        val vaccination = vaccinations?.lastOrNull() ?: return null
        return vaccination.doseNumber to vaccination.totalSeriesOfDoses
    }

val GreenCertificate.testType: String?
    get() = tests?.lastOrNull()?.typeOfTest?.trim()

val GreenCertificate.testResultIsNegative: Boolean?
    get() = tests?.lastOrNull()?.isResultNegative()

val GreenCertificate.testResultCode: String?
    get() = tests?.lastOrNull()?.isResultNegative()?.let { isNegative ->
        if (isNegative) "negative" else "positive"
    }

val GreenCertificate.testDateTimeOfCollection: Date?
    get() = tests?.lastOrNull()?.dateTimeOfCollection
        ?.let(::parseToOffsetDateTimeOrNull)?.toInstant()?.let { Date.from(it) }

val GreenCertificate.recoveryDateOfFirstPositiveTest: Date?
    get() = recoveryStatements?.lastOrNull()?.dateOfFirstPositiveTest?.let(yearMonthDayUsParser()::parseOrNull)

val GreenCertificate.manufacturer: String?
    get() = when {
        vaccinations?.lastOrNull() != null -> vaccinations?.lastOrNull()?.manufacturer
        tests?.lastOrNull() != null -> tests?.lastOrNull()?.testNameAndManufacturer
        else -> null
    }

val GreenCertificate.isAutoTest: Boolean
    get() = manufacturer == Constants.Certificate.MANUFACTURER_AUTOTEST

private fun yearMonthDayUsParser() = SimpleDateFormat("yyyy-MM-dd", Locale.US)