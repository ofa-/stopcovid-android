package com.lunabeestudio.stopcovid.extension

import android.annotation.SuppressLint
import com.lunabeestudio.domain.extension.walletOldCertificateThresholdInMs
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.manager.Blacklist2DDOCManager
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.SanitaryCertificate
import com.lunabeestudio.stopcovid.model.VaccinationCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

fun WalletCertificate.fullName(): String =
    "${firstName.orEmpty()} ${name.orEmpty()}".trim().uppercase()

fun WalletCertificate.shortDescription(): String {
    return when (this) {
        is FrenchCertificate,
        is EuropeanCertificate -> fullName()
    }
}

@SuppressLint("SimpleDateFormat")
fun WalletCertificate.fullDescription(strings: LocalizedStrings, configuration: Configuration): String {
    var text = strings["wallet.proof.${type.stringKey}.description"]
    val dateFormat = SimpleDateFormat("d MMM yyyy")
    val analysisDateFormat = SimpleDateFormat("d MMM yyyy, HH:mm")
    return when (this) {
        is SanitaryCertificate -> {
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.FIRST_NAME.code}>", firstName.orNA())
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.NAME.code}>", name.orNA())
            text = text?.replace(
                "<${SanitaryCertificate.SanitaryCertificateFields.BIRTH_DATE.code}>",
                birthDate.orNA()
            )

            val genderString =
                strings["wallet.proof.${type.stringKey}.${SanitaryCertificate.SanitaryCertificateFields.GENDER.code}.$gender"]
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.GENDER.code}>", genderString.orNA())

            val analysisCodeString = strings.stringsFormat("wallet.proof.${type.stringKey}.loinc.$analysisCode", analysisCode)
            text = text?.replace(
                "<${SanitaryCertificate.SanitaryCertificateFields.ANALYSIS_CODE.code}>",
                analysisCodeString ?: "LOINC:$analysisCode"
            )

            val dateString = analysisDate?.let { analysisDateFormat.format(it) }.orNA()
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.ANALYSIS_DATE.code}>", dateString)

            val resultString =
                strings["wallet.proof.${type.stringKey}.${SanitaryCertificate.SanitaryCertificateFields.TEST_RESULT.code}.$testResult"]
            text = text?.replace(
                oldValue = "<${SanitaryCertificate.SanitaryCertificateFields.TEST_RESULT.code}>",
                newValue = resultString.orNA()
            )

            if (testResult == "N") {
                val timeIndicator = validityString(configuration, strings)
                text = text?.plus("\n$timeIndicator")
            }

            text ?: ""
        }
        is VaccinationCertificate -> {
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.FIRST_NAME.code}>", firstName.orNA())
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.NAME.code}>", name.orNA())
            text = text?.replace(
                "<${VaccinationCertificate.VaccinationCertificateFields.BIRTH_DATE.code}>",
                birthDate.orNA()
            )
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.DISEASE_NAME.code}>", diseaseName.orNA())
            text = text?.replace(
                "<${VaccinationCertificate.VaccinationCertificateFields.PROPHYLACTIC_AGENT.code}>",
                prophylacticAgent.orNA()
            )
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.VACCINE_NAME.code}>", vaccineName.orNA())
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.VACCINE_MAKER.code}>", vaccineMaker.orNA())
            text = text?.replace(
                "<${VaccinationCertificate.VaccinationCertificateFields.LAST_VACCINATION_STATE_RANK.code}>",
                lastVaccinationStateRank.orNA()
            )
            text = text?.replace(
                "<${VaccinationCertificate.VaccinationCertificateFields.COMPLETE_CYCLE_DOSE_COUNT.code}>",
                completeCycleDosesCount.orNA()
            )

            text = text?.replace(
                "<${VaccinationCertificate.VaccinationCertificateFields.LAST_VACCINATION_DATE.code}>",
                lastVaccinationDate?.let { dateFormat.format(it) }.orNA()
            )

            val vaxCode = VaccinationCertificate.VaccinationCertificateFields.VACCINATION_CYCLE_STATE.code
            val vaccinationState = strings["wallet.proof.${type.stringKey}.$vaxCode.$vaccinationCycleState"]
            text = text?.replace("<$vaxCode>", vaccinationState.orNA())
            text ?: ""
        }
        is EuropeanCertificate -> {
            val prefix = if (this.greenCertificate.isFrench) {
                ""
            } else {
                this.greenCertificate.countryCode?.countryCodeToFlagEmoji ?: ""
            }
            formatDccText(
                prefix + strings["wallet.proof.europe.${this.type.code}.description"],
                strings,
                dateFormat,
                analysisDateFormat
            )
        }
    }
}

fun EuropeanCertificate.formatDccText(
    inputText: String?,
    strings: LocalizedStrings,
    dateFormat: SimpleDateFormat,
    analysisDateFormat: SimpleDateFormat
): String {
    var formattedText = inputText
    formattedText = formattedText?.replace("<FULL_NAME>", fullName())
    formattedText = formattedText?.replace("<BIRTHDATE>", this.greenCertificate.formattedDateOfBirthDate(dateFormat))
    when (this.type) {
        WalletCertificateType.SANITARY,
        WalletCertificateType.VACCINATION -> {
            Timber.e("Unexpected type ${this.type} with ${this.javaClass.simpleName}")
        }
        WalletCertificateType.VACCINATION_EUROPE -> {
            val vacName = this.greenCertificate.vaccineMedicinalProduct?.let { strings["vac.product.$it"] ?: it }
            formattedText = formattedText?.replace("<VACCINE_NAME>", vacName.orNA())
            formattedText = formattedText?.replace("<DATE>", this.greenCertificate.vaccineDate?.let(dateFormat::format).orNA())
        }
        WalletCertificateType.RECOVERY_EUROPE -> {
            formattedText = formattedText?.replace(
                "<DATE>",
                this.greenCertificate.recoveryDateOfFirstPositiveTest?.let(dateFormat::format).orNA()
            )
        }
        WalletCertificateType.SANITARY_EUROPE -> {
            val testName = this.greenCertificate.testType?.let { strings["test.man.$it"] ?: it }
            formattedText = formattedText?.replace("<ANALYSIS_CODE>", testName.orNA())
            val testResult = this.greenCertificate.testResultCode?.let { strings["wallet.proof.europe.test.$it"] }
            formattedText = formattedText?.replace("<ANALYSIS_RESULT>", testResult.orNA())
            formattedText = formattedText?.replace(
                "<FROM_DATE>",
                this.greenCertificate.testDateTimeOfCollection?.let(analysisDateFormat::format).orNA()
            )
        }
        WalletCertificateType.EXEMPTION -> {
            formattedText = formattedText?.replace(
                "<FROM_DATE>",
                this.greenCertificate.exemptionCertificateValidFrom?.let(dateFormat::format).orNA()
            )
            formattedText = formattedText?.replace(
                "<TO_DATE>",
                this.greenCertificate.exemptionCertificateValidUntil?.let(dateFormat::format).orNA()
            )
        }
        WalletCertificateType.ACTIVITY_PASS -> {
            /* no-op */
        }
    }
    return formattedText.orEmpty()
}

fun VaccinationCertificate.statusStringKey(): String {
    val vaxCode = VaccinationCertificate.VaccinationCertificateFields.VACCINATION_CYCLE_STATE.code
    return "wallet.proof.${type.stringKey}.$vaxCode.$vaccinationCycleState"
}

fun WalletCertificate.tagStringKey(): String {
    return "wallet.proof.${type.stringKey}.pillTitle"
}

fun WalletCertificate.isRecent(configuration: Configuration): Boolean {
    return !isOld(configuration)
}

fun WalletCertificate.isOld(configuration: Configuration): Boolean {
    return configuration.walletOldCertificateThresholdInMs(type)?.let { System.currentTimeMillis() - timestamp > it } ?: false
}

fun SanitaryCertificate.validityString(configuration: Configuration, strings: LocalizedStrings): String? {
    val testCertificateValidityThresholds = configuration.testCertificateValidityThresholds
    val maxValidityInHours = testCertificateValidityThresholds.maxOrNull() ?: 0
    val timeSinceCreation = System.currentTimeMillis() - timestamp
    val validityThresholdInHours = testCertificateValidityThresholds
        .filter { TimeUnit.HOURS.toMillis(it.toLong()) > timeSinceCreation }
        .minOrNull()
    return if (validityThresholdInHours != null) {
        strings.stringsFormat("wallet.proof.lessThanSpecificHours", validityThresholdInHours)
    } else {
        strings.stringsFormat("wallet.proof.moreThanSpecificHours", maxValidityInHours)
    }
}

val WalletCertificate.raw: RawWalletCertificate
    get() = RawWalletCertificate(
        id = id,
        type = type,
        value = value,
        timestamp = timestamp,
        isFavorite = (this as? EuropeanCertificate)?.isFavorite ?: false,
        canRenewDccLight = (this as? EuropeanCertificate)?.canRenewActivityPass,
        rootWalletCertificateId = (this as? EuropeanCertificate)?.rootWalletCertificateId,
    )

@OptIn(ExperimentalTime::class)
fun WalletCertificate.isEligibleForActivityPass(blacklistDCCManager: BlacklistDCCManager, activityPassSkipNegTestHours: Int): Boolean {
    if (this.type !in arrayOf(
            WalletCertificateType.SANITARY_EUROPE,
            WalletCertificateType.RECOVERY_EUROPE,
            WalletCertificateType.VACCINATION_EUROPE
        )
    ) return false

    if ((this as? EuropeanCertificate)?.isExpired == true) {
        return false
    }

    if ((this as? EuropeanCertificate)?.canRenewActivityPass == false) {
        return false
    }

    if (this.type == WalletCertificateType.SANITARY_EUROPE &&
        timestamp + Duration.hours(activityPassSkipNegTestHours).inWholeMilliseconds < System.currentTimeMillis()
    ) {
        return false
    }

    if (blacklistDCCManager.blacklistedDCCHashes.value?.contains(sha256) == true) return false

    return true
}

val EuropeanCertificate.isExpired: Boolean
    get() = expirationTime < System.currentTimeMillis()

fun EuropeanCertificate.isBlacklisted(blacklistDCCManager: BlacklistDCCManager): Boolean =
    blacklistDCCManager.blacklistedDCCHashes.value?.contains(sha256) == true

fun FrenchCertificate.isBlacklisted(blacklist2DDOCManager: Blacklist2DDOCManager): Boolean =
    blacklist2DDOCManager.blacklisted2DDOCHashes.value?.contains(sha256) == true

// Use 1sec of threshold due to sec -> ms rounding
fun EuropeanCertificate.activityPassValidFuture(): Boolean = if (type == WalletCertificateType.ACTIVITY_PASS) {
    System.currentTimeMillis() < timestamp - 1_000
} else {
    false
}