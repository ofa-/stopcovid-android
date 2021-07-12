package com.lunabeestudio.stopcovid.extension

import android.annotation.SuppressLint
import com.lunabeestudio.domain.extension.walletOldCertificateThresholdInMs
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.SanitaryCertificate
import com.lunabeestudio.stopcovid.model.VaccinationCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

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
        is EuropeanCertificate -> when (this.type) {
            WalletCertificateType.SANITARY,
            WalletCertificateType.VACCINATION -> {
                Timber.e("Unexpected type ${this.type} with ${this.javaClass.simpleName}")
                ""
            }
            WalletCertificateType.VACCINATION_EUROPE -> {
                text = strings["wallet.proof.europe.vaccine.description"]
                text = text?.replace("<FULL_NAME>", fullName())
                text = text?.replace("<BIRTHDATE>", this.greenCertificate.formattedDateOfBirthDate(dateFormat))
                val vacName = this.greenCertificate.vaccineMedicinalProduct?.let { strings["vac.product.$it"] }
                text = text?.replace("<VACCINE_NAME>", vacName.orNA())
                text = text?.replace("<DATE>", this.greenCertificate.vaccineDate?.let(dateFormat::format).orNA())
                text ?: ""
            }
            WalletCertificateType.RECOVERY_EUROPE -> {
                text = strings["wallet.proof.europe.recovery.description"]
                text = text?.replace("<FULL_NAME>", fullName())
                text = text?.replace("<BIRTHDATE>", this.greenCertificate.formattedDateOfBirthDate(dateFormat))
                text = text?.replace("<DATE>", this.greenCertificate.recoveryDateOfFirstPositiveTest?.let(dateFormat::format).orNA())
                text ?: ""
            }
            WalletCertificateType.SANITARY_EUROPE -> {
                text = strings["wallet.proof.europe.test.description"]
                text = text?.replace("<FULL_NAME>", fullName())
                text = text?.replace("<BIRTHDATE>", this.greenCertificate.formattedDateOfBirthDate(dateFormat))
                val testName = this.greenCertificate.testType?.let { strings["test.man.$it"] }
                text = text?.replace("<ANALYSIS_CODE>", testName.orNA())
                val testResult = this.greenCertificate.testResultCode?.let { strings["wallet.proof.europe.test.$it"] }
                text = text?.replace("<ANALYSIS_RESULT>", testResult.orNA())
                text = text?.replace(
                    "<FROM_DATE>",
                    this.greenCertificate.testDateTimeOfCollection?.let(analysisDateFormat::format).orNA()
                )
                text ?: ""
            }
        }
    }
}

fun EuropeanCertificate.tagStringKey(): String {
    return when (this.type) {
        WalletCertificateType.VACCINATION,
        WalletCertificateType.VACCINATION_EUROPE,
        -> "wallet.proof.vaccinationCertificate.pillTitle"
        else -> ""
    }
}

fun EuropeanCertificate.statusStringKey(): String {
    return when (this.type) {
        WalletCertificateType.VACCINATION,
        WalletCertificateType.VACCINATION_EUROPE,
        -> "wallet.proof.vaccinationCertificate.LA." + this.greenCertificate.vaccineDose?.let {
                                (first, second) -> when { (first == second) -> "TE" else -> "CO" }}
        WalletCertificateType.RECOVERY_EUROPE -> "enum.HCertType.recovery"
        else -> ""
   }
}

fun WalletCertificate.tagStringKey() = when (this) {
    is EuropeanCertificate -> tagStringKey()
    is FrenchCertificate -> "wallet.proof.${type.stringKey}.pillTitle"
}

fun WalletCertificate.statusStringKey() = when (this) {
    is EuropeanCertificate -> statusStringKey()
    is VaccinationCertificate -> statusStringKey() // french only
    is FrenchCertificate -> ""
}

fun VaccinationCertificate.statusStringKey(): String {
    val vaxCode = VaccinationCertificate.VaccinationCertificateFields.VACCINATION_CYCLE_STATE.code
    return "wallet.proof.${type.stringKey}.$vaxCode.$vaccinationCycleState"
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
    get() = RawWalletCertificate(type, value, timestamp)