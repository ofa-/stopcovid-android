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
import com.lunabeestudio.domain.model.WalletCertificateType
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

fun EuropeanCertificate.smartWalletProfileId(): String {
    return ((firstName ?: name).orEmpty() + greenCertificate.dateOfBirth).uppercase()
}

fun EuropeanCertificate.multipassProfileId(): String {
    return (greenCertificate.person.standardisedGivenName.orEmpty() + greenCertificate.person.standardisedFamilyName)
        .replace(Regex("[^a-zA-Z]*"), "")
        .trim()
        .uppercase() + greenCertificate.dateOfBirth
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
    val yearMonthDayUsParserForceTimeZone = yearMonthDayUsParserForceTimeZone()
    val calendar = Calendar.getInstance()

    val datePivot1 = configuration.smartWalletExp?.pivot1?.let(yearMonthDayUsParserForceTimeZone::parse)?.time ?: 0L
    val datePivot2 = configuration.smartWalletExp?.pivot2?.let(yearMonthDayUsParserForceTimeZone::parse)?.time ?: 0L
    val datePivot3 = configuration.smartWalletExp?.pivot3?.let(yearMonthDayUsParserForceTimeZone::parse)?.time ?: 0L

    val agePivotHigh: Long
    val agePivotLow: Long
    try {
        agePivotHigh = calendar.apply {
            timeZone = currentTimeZone()
            time = yearMonthDayUsParserForceTimeZone.parse(greenCertificate.dateOfBirth) ?: Date()
            add(Calendar.YEAR, configuration.smartWalletAges?.high ?: 0)
        }.timeInMillis
        agePivotLow = calendar.apply {
            timeZone = currentTimeZone()
            time = yearMonthDayUsParserForceTimeZone.parse(greenCertificate.dateOfBirth) ?: Date()
            add(Calendar.YEAR, configuration.smartWalletAges?.low ?: 0)
        }.timeInMillis + (configuration.smartWalletAges?.lowExpDays?.days?.inWholeMilliseconds ?: 0)
    } catch (e: ParseException) {
        Timber.e(e)
        return null
    }

    val cutoff = max(datePivot1, min(agePivotHigh, datePivot2))

    return when {
        type == WalletCertificateType.VACCINATION_EUROPE -> {
            val isAr = configuration.smartWalletVacc?.ar?.contains(greenCertificate.vaccineMedicinalProduct) == true
            val isAz = configuration.smartWalletVacc?.az?.contains(greenCertificate.vaccineMedicinalProduct) == true
            val isJa = configuration.smartWalletVacc?.ja?.contains(greenCertificate.vaccineMedicinalProduct) == true
            val vaccinDoseNumber = greenCertificate.vaccineDose?.first ?: 0

            val expDcc = when {
                // Vaccine 1 dose
                (isAr || isAz) && vaccinDoseNumber == 1 -> {
                    val vacc11DosesMillis = configuration.smartWalletExp?.vacc11DosesNbDays?.days?.inWholeMilliseconds ?: 0L
                    val vacc11DosesNewMillis = configuration.smartWalletExp?.vacc11DosesNbNewDays?.days?.inWholeMilliseconds ?: 0L
                    max(
                        max(
                            cutoff,
                            min(datePivot3, greenCertificate.vaccineDateForceTimeZone?.time?.plus(vacc11DosesMillis) ?: 0L)
                        ),
                        greenCertificate.vaccineDateForceTimeZone?.time?.plus(vacc11DosesNewMillis)
                            ?: 0L
                    )
                }
                isJa && vaccinDoseNumber == 1 -> {
                    val vaccJan11DosesMillis = configuration.smartWalletExp?.vaccJan11DosesNbDays?.days?.inWholeMilliseconds ?: 0L
                    max(datePivot1, greenCertificate.vaccineDate?.time?.plus(vaccJan11DosesMillis) ?: 0L)
                }
                // Vaccine 2 doses
                (isAr || isAz) && vaccinDoseNumber == 2 -> {
                    val vacc22DosesMillis = configuration.smartWalletExp?.vacc22DosesNbDays?.days?.inWholeMilliseconds ?: 0L
                    val vacc22DosesNewMillis = configuration.smartWalletExp?.vacc22DosesNbNewDays?.days?.inWholeMilliseconds ?: 0L
                    max(
                        max(
                            cutoff,
                            min(datePivot3, greenCertificate.vaccineDateForceTimeZone?.time?.plus(vacc22DosesMillis) ?: 0L)
                        ),
                        greenCertificate.vaccineDateForceTimeZone?.time?.plus(vacc22DosesNewMillis)
                            ?: 0L
                    )
                }
                isJa && vaccinDoseNumber == 2 -> {
                    val vaccJan22DosesMillis = configuration.smartWalletExp?.vaccJan22DosesNbDays?.days?.inWholeMilliseconds ?: 0L
                    val vaccJan22DosesNewMillis = configuration.smartWalletExp?.vaccJan22DosesNbNewDays?.days?.inWholeMilliseconds ?: 0L
                    max(
                        max(
                            cutoff,
                            min(datePivot3, greenCertificate.vaccineDateForceTimeZone?.time?.plus(vaccJan22DosesMillis) ?: 0L)
                        ),
                        greenCertificate.vaccineDateForceTimeZone?.time?.plus(vaccJan22DosesNewMillis)
                            ?: 0L
                    )
                }
                else -> null
            }

            expDcc?.let { Date(max(agePivotLow, it)) }
        }
        greenCertificate.isRecoveryOrTestPositive -> {
            val yearMonthDayUsParser = yearMonthDayUsParser()
            val age = yearMonthDayUsParser.parseOrNull(greenCertificate.dateOfBirth)?.yearsOld() ?: 0
            val recoveryValidityThreshold = configuration.recoveryValidityThreshold

            val recMillis = configuration.smartWalletExp?.recNbDays?.days?.inWholeMilliseconds ?: 0L
            val recNewMillis = if (age >= configuration.smartWalletAges?.low ?: 0) {
                configuration.smartWalletExp?.recNbNewDays?.days?.inWholeMilliseconds ?: 0L
            } else {
                recoveryValidityThreshold?.min?.plus(recoveryValidityThreshold.max)?.inWholeMilliseconds ?: 0L
            }

            val testDate =
                greenCertificate.recoveryDateOfFirstPositiveTestForceTimeZone
                    ?: greenCertificate.testDateTimeOfCollection?.midnightInCurrentTimeZone(
                        yearMonthDayUsParser,
                        yearMonthDayUsParserForceTimeZone
                    )
            val expDcc = max(
                max(
                    cutoff,
                    min(
                        datePivot3,
                        testDate?.time?.plus(recMillis) ?: 0L
                    )
                ),
                testDate?.time?.plus(recNewMillis) ?: 0L
            )

            Date(max(agePivotLow, expDcc))
        }
        else -> null
    }
}

private fun EuropeanCertificate.eligibleDate(configuration: Configuration): Date? {
    val yearMonthDayUsParser = yearMonthDayUsParser()
    val yearMonthDayUsParserForceTimeZone = yearMonthDayUsParserForceTimeZone()

    val isAr = configuration.smartWalletVacc?.ar?.contains(greenCertificate.vaccineMedicinalProduct) == true
    val isAz = configuration.smartWalletVacc?.az?.contains(greenCertificate.vaccineMedicinalProduct) == true
    val isJa = configuration.smartWalletVacc?.ja?.contains(greenCertificate.vaccineMedicinalProduct) == true
    val vaccinDoseNumber = greenCertificate.vaccineDose?.first ?: 0

    val vacc22Doses: Duration
    val vaccJan11Doses: Duration
    val vaccJan22Doses: Duration
    val rec: Duration
    val age = yearMonthDayUsParser.parseOrNull(greenCertificate.dateOfBirth)?.yearsOld() ?: 0
    if (age >= configuration.smartWalletAges?.low ?: 0) {
        vacc22Doses = configuration.smartWalletElg?.vacc22DosesNbDays?.days ?: Duration.ZERO
        vaccJan11Doses = configuration.smartWalletElg?.vaccJan11DosesNbDays?.days ?: Duration.ZERO
        vaccJan22Doses = configuration.smartWalletElg?.vaccJan22DosesNbDays?.days ?: Duration.ZERO
        rec = configuration.smartWalletElg?.recNbDays?.days ?: Duration.ZERO
    } else {
        vacc22Doses = configuration.smartWalletElg?.vacc22DosesNbDaysLow?.days ?: Duration.ZERO
        vaccJan11Doses = configuration.smartWalletElg?.vaccJan11DosesNbDaysLow?.days ?: Duration.ZERO
        vaccJan22Doses = configuration.smartWalletElg?.vaccJan22DosesNbDaysLow?.days ?: Duration.ZERO
        rec = configuration.smartWalletElg?.recNbDaysLow?.days ?: Duration.ZERO
    }

    val elgDcc = when {
        (isAr || isAz) && (vaccinDoseNumber == 1 || vaccinDoseNumber == 2) -> {
            greenCertificate.vaccineDateForceTimeZone?.time?.plus(vacc22Doses.inWholeMilliseconds) ?: 0L
        }
        isJa && vaccinDoseNumber == 1 -> {
            greenCertificate.vaccineDateForceTimeZone?.time?.plus(vaccJan11Doses.inWholeMilliseconds) ?: 0L
        }
        isJa && vaccinDoseNumber == 2 -> {
            greenCertificate.vaccineDateForceTimeZone?.time?.plus(vaccJan22Doses.inWholeMilliseconds) ?: 0L
        }
        greenCertificate.isRecoveryOrTestPositive -> {
            val testDate = greenCertificate.recoveryDateOfFirstPositiveTestForceTimeZone
                ?: greenCertificate.testDateTimeOfCollection?.midnightInCurrentTimeZone(
                    yearMonthDayUsParser,
                    yearMonthDayUsParserForceTimeZone,
                )
            testDate?.time?.plus(rec.inWholeMilliseconds) ?: 0L
        }
        else -> return null
    }

    val agePivotLow = try {
        Calendar.getInstance().apply {
            timeZone = currentTimeZone()
            time = yearMonthDayUsParserForceTimeZone.parse(greenCertificate.dateOfBirth) ?: Date()
            add(Calendar.YEAR, configuration.smartWalletAges?.lowElg ?: 0)
        }.timeInMillis
    } catch (e: ParseException) {
        Timber.e(e)
        return null
    }

    return Date(max(agePivotLow, elgDcc))
}

// Convert date from it's timeZone to the current timeZone at midnight
// For example, if done in January, Sat Aug 28 00:43:07 GMT+02:00 2021 will give 2021-08-28
// And then it will be converted to Sat Aug 28 01:00:00 GMT+02:00 2021
// if dateFormatCurrentTime only was used it would have given Fri Aug 27 01:00:00 GMT+02:00 2021
// if dateFormatLocalTime only was used it would have given Sat Aug 28 00:00:00 GMT+02:00 2021
// As we compare multiple date at midnight shifted with days, it's important to always have only one timeZone as a reference
private fun Date.midnightInCurrentTimeZone(dateFormatLocalTime: DateFormat, dateFormatCurrentTime: DateFormat): Date? {
    return dateFormatCurrentTime.parse(dateFormatLocalTime.format(this))
}