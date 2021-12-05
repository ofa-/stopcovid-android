/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/01/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.stopcovid.model.Eligible
import com.lunabeestudio.stopcovid.model.EligibleSoon
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.ExpireSoon
import com.lunabeestudio.stopcovid.model.Expired
import com.lunabeestudio.stopcovid.model.SmartWalletState
import com.lunabeestudio.stopcovid.model.Valid
import timber.log.Timber
import java.text.DateFormat
import java.text.ParseException
import java.util.Calendar
import java.util.Date
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.days

fun EuropeanCertificate.profileId(): String {
    return (firstName + greenCertificate.dateOfBirth).uppercase()
}

fun EuropeanCertificate.smartWalletState(configuration: Configuration): SmartWalletState {
    val expirationDate = expirationDate(configuration)
    val eligibleDate = eligibleDate(configuration)
    val limitExpireSoonDisplay = configuration.smartWalletExp?.displayExpDays?.days
    val limitExpireSoonDate = Date(midnightDate().time + (limitExpireSoonDisplay?.inWholeMilliseconds ?: 0L))
    val limitEligibleSoonDisplay = configuration.smartWalletElg?.displayElgDays?.days
    val limitEligibleSoonDate = Date(midnightDate().time + (limitEligibleSoonDisplay?.inWholeMilliseconds ?: 0))
    return when {
        expirationDate?.future() == false && eligibleDate != null -> Expired(expirationDate, eligibleDate)
        expirationDate?.future() == true && eligibleDate != null && expirationDate.before(limitExpireSoonDate) -> ExpireSoon(
            expirationDate,
            eligibleDate
        )
        eligibleDate?.future() == false -> Eligible(expirationDate, eligibleDate)
        eligibleDate?.future() == true && eligibleDate.before(limitEligibleSoonDate) -> EligibleSoon(expirationDate, eligibleDate)
        else -> Valid(expirationDate, eligibleDate)
    }
}

private fun EuropeanCertificate.expirationDate(configuration: Configuration): Date? {
    val yearMonthDayUsParser = yearMonthDayUsParser()
    val calendar = Calendar.getInstance()

    val datePivot1 = configuration.smartWalletExp?.pivot1?.let(yearMonthDayUsParser::parse)?.time ?: 0L
    val datePivot2 = configuration.smartWalletExp?.pivot2?.let(yearMonthDayUsParser::parse)?.time ?: 0L

    val agePivotLow: Long
    val agePivotHigh: Long
    try {
        agePivotLow = calendar.apply {
            time = yearMonthDayUsParser.parse(greenCertificate.dateOfBirth) ?: Date()
            add(Calendar.YEAR, configuration.smartWalletAges?.low ?: 0)
        }.timeInMillis + (configuration.smartWalletAges?.lowExpDays?.days?.inWholeMilliseconds ?: 0)
        agePivotHigh = calendar.apply {
            time = yearMonthDayUsParser.parse(greenCertificate.dateOfBirth) ?: Date()
            add(Calendar.YEAR, configuration.smartWalletAges?.high ?: 0)
        }.timeInMillis
    } catch (e: ParseException) {
        Timber.e(e)
        return null
    }

    val cutoff = max(datePivot1, min(agePivotHigh, datePivot2))

    val isAr = configuration.smartWalletVacc?.ar?.contains(greenCertificate.vaccineMedicinalProduct) == true
    val isAz = configuration.smartWalletVacc?.az?.contains(greenCertificate.vaccineMedicinalProduct) == true
    val isJa = configuration.smartWalletVacc?.ja?.contains(greenCertificate.vaccineMedicinalProduct) == true
    val vaccinDoseNumber = greenCertificate.vaccineDose?.first ?: 0

    val expDcc = when {
        (isAr || isAz) && vaccinDoseNumber == 1 -> {
            val vacc11DosesMillis = configuration.smartWalletExp?.vacc11DosesNbDays?.days?.inWholeMilliseconds ?: 0L
            max(cutoff, greenCertificate.vaccineDate?.time?.plus(vacc11DosesMillis) ?: 0L)
        }
        (isAr || isAz) && vaccinDoseNumber == 2 -> {
            val vacc22DosesMillis = configuration.smartWalletExp?.vacc22DosesNbDays?.days?.inWholeMilliseconds ?: 0L
            max(cutoff, greenCertificate.vaccineDate?.time?.plus(vacc22DosesMillis) ?: 0L)
        }
        greenCertificate.isRecovery -> {
            val recMillis = configuration.smartWalletExp?.recNbDays?.days?.inWholeMilliseconds ?: 0L
            val testDate =
                greenCertificate.recoveryDateOfFirstPositiveTest
                    ?: greenCertificate.testDateTimeOfCollection?.midnightInCurrentTimeZone(yearMonthDayUsParser)
            max(cutoff, testDate?.time?.plus(recMillis) ?: 0L)
        }
        isJa && (vaccinDoseNumber == 1 || vaccinDoseNumber == 2) -> {
            val vaccJan11DosesMillis = configuration.smartWalletExp?.vaccJan11DosesNbDays?.days?.inWholeMilliseconds ?: 0L
            max(datePivot1, greenCertificate.vaccineDate?.time?.plus(vaccJan11DosesMillis) ?: 0L)
        }
        else -> return null
    }

    return Date(max(agePivotLow, expDcc))
}

private fun EuropeanCertificate.eligibleDate(configuration: Configuration): Date? {
    val yearMonthDayUsParser = yearMonthDayUsParser()

    val isAr = configuration.smartWalletVacc?.ar?.contains(greenCertificate.vaccineMedicinalProduct) == true
    val isAz = configuration.smartWalletVacc?.az?.contains(greenCertificate.vaccineMedicinalProduct) == true
    val isJa = configuration.smartWalletVacc?.ja?.contains(greenCertificate.vaccineMedicinalProduct) == true
    val vaccinDoseNumber = greenCertificate.vaccineDose?.first ?: 0

    val elgDcc = when {
        isAr || isAz && (vaccinDoseNumber == 1 || vaccinDoseNumber == 2) -> {
            val vacc22DosesMillis = (configuration.smartWalletElg?.vacc22DosesNbDays ?: 0).days.inWholeMilliseconds
            greenCertificate.vaccineDate?.time?.plus(vacc22DosesMillis) ?: 0L
        }
        isJa && (vaccinDoseNumber == 1 || vaccinDoseNumber == 2) -> {
            val vaccJan11DosesMillis = (configuration.smartWalletElg?.vaccJan11DosesNbDays ?: 0).days.inWholeMilliseconds
            greenCertificate.vaccineDate?.time?.plus(vaccJan11DosesMillis) ?: 0L
        }
        greenCertificate.isRecovery -> {
            val recMillis = (configuration.smartWalletElg?.recNbDays ?: 0).days.inWholeMilliseconds
            val testDate = greenCertificate.recoveryDateOfFirstPositiveTest
                ?: greenCertificate.testDateTimeOfCollection?.midnightInCurrentTimeZone(yearMonthDayUsParser)
            testDate?.time?.plus(recMillis) ?: 0L
        }
        else -> return null
    }

    val agePivotLow = try {
        Calendar.getInstance().apply {
            time = yearMonthDayUsParser().parse(greenCertificate.dateOfBirth) ?: Date()
            add(Calendar.YEAR, configuration.smartWalletAges?.low ?: 0)
        }.timeInMillis
    } catch (e: ParseException) {
        Timber.e(e)
        return null
    }

    return Date(max(agePivotLow, elgDcc))
}

private fun Date.midnightInCurrentTimeZone(dateFormat: DateFormat): Date? {
    return dateFormat.run {
        parse(format(this@midnightInCurrentTimeZone))
    }
}