package com.lunabeestudio.stopcovid.extension

import android.annotation.SuppressLint
import com.lunabeestudio.domain.extension.walletOldCertificateThresholdInMs
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.model.SanitaryCertificate
import com.lunabeestudio.stopcovid.model.VaccinationCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

fun WalletCertificate.shortDescription(): String {
    return when (this) {
        is SanitaryCertificate,
        is VaccinationCertificate -> "$firstName $name"
    }
}

@SuppressLint("SimpleDateFormat")
fun WalletCertificate.fullDescription(strings: LocalizedStrings, configuration: Configuration): String {
    var text = strings["wallet.proof.${type.stringKey}.description"]
    val dateFormat = SimpleDateFormat("d MMM yyyy")
    return when (this) {
        is SanitaryCertificate -> {
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.FIRST_NAME.code}>", firstName ?: "N/A")
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.NAME.code}>", name ?: "N/A")
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.BIRTH_DATE.code}>",
                birthDate ?: "N/A")

            val genderString = strings["wallet.proof.${type.stringKey}.${SanitaryCertificate.SanitaryCertificateFields.GENDER.code}.$gender"]
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.GENDER.code}>", genderString ?: "N/A")

            val analysisCodeString = strings.stringsFormat("wallet.proof.${type.stringKey}.loinc.$analysisCode", analysisCode)
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.ANALYSIS_CODE.code}>",
                analysisCodeString ?: "LOINC:$analysisCode")

            val analysisDateFormat = SimpleDateFormat("d MMM yyyy, HH:mm")
            val dateString = analysisDate?.let { analysisDateFormat.format(it) } ?: "N/A"
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.ANALYSIS_DATE.code}>", dateString)

            val resultString = strings["wallet.proof.${type.stringKey}.${SanitaryCertificate.SanitaryCertificateFields.TEST_RESULT.code}.$testResult"]
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.TEST_RESULT.code}>", resultString ?: "N/A")

            if (testResult == "N") {
                val timeIndicator = validityString(configuration, strings)
                text = text?.plus("\n$timeIndicator")
            }

            text ?: ""
        }
        is VaccinationCertificate -> {
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.FIRST_NAME.code}>", firstName ?: "N/A")
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.NAME.code}>", name ?: "N/A")
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.BIRTH_DATE.code}>",
                birthDate ?: "N/A")
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.DISEASE_NAME.code}>", diseaseName ?: "N/A")
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.PROPHYLACTIC_AGENT.code}>",
                prophylacticAgent ?: "N/A")
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.VACCINE_NAME.code}>", vaccineName ?: "N/A")
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.VACCINE_MAKER.code}>", vaccineMaker ?: "N/A")
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.LAST_VACCINATION_STATE_RANK.code}>",
                lastVaccinationStateRank ?: "N/A")
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.COMPLETE_CYCLE_DOSE_COUNT.code}>",
                completeCycleDosesCount ?: "N/A")

            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.LAST_VACCINATION_DATE.code}>",
                lastVaccinationDate?.let { dateFormat.format(it) } ?: "N/A")

            val vaccinationState = strings["wallet.proof.${type.stringKey}.${VaccinationCertificate.VaccinationCertificateFields.VACCINATION_CYCLE_STATE.code}.$vaccinationCycleState"]
            text = text?.replace("<${VaccinationCertificate.VaccinationCertificateFields.VACCINATION_CYCLE_STATE.code}>",
                vaccinationState ?: "N/A")

            text ?: ""
        }
    }
}

fun VaccinationCertificate.statusStringKey(): String = "wallet.proof.${type.stringKey}.${VaccinationCertificate.VaccinationCertificateFields.VACCINATION_CYCLE_STATE.code}.$vaccinationCycleState"

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
