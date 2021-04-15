package com.lunabeestudio.stopcovid.extension

import android.annotation.SuppressLint
import com.lunabeestudio.domain.extension.certificateValidityThresholdInMs
import com.lunabeestudio.domain.extension.walletOldCertificateThresholdInMs
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.model.SanitaryCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import java.text.SimpleDateFormat

fun WalletCertificate.shortDescription(): String {
    return when (this) {
        is SanitaryCertificate -> {
            "$firstName $name"
        }
    }
}

@SuppressLint("SimpleDateFormat")
fun WalletCertificate.fullDescription(strings: LocalizedStrings, configuration: Configuration): String {
    var text = strings["wallet.proof.${type.stringKey}.description"]
    val dateFormat = SimpleDateFormat("dd/MM/yyyy")
    return when (this) {
        is SanitaryCertificate -> {
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.FIRST_NAME.code}>", firstName ?: "N/A")
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.NAME.code}>", name ?: "N/A")
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.BIRTH_DATE.code}>",
                birthDate?.let { dateFormat.format(it) } ?: "N/A")

            val genderString = strings["wallet.proof.${type.stringKey}.${SanitaryCertificate.SanitaryCertificateFields.GENDER.code}.$gender"]
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.GENDER.code}>", genderString ?: "N/A")

            val analysisCodeString = strings.stringsFormat("wallet.proof.${type.stringKey}.loinc.$analysisCode", analysisCode)
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.ANALYSIS_CODE.code}>",
                analysisCodeString ?: "LOINC:$analysisCode")

            val analysisDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm")
            val dateString = analysisDate?.let { analysisDateFormat.format(it) } ?: "N/A"
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.ANALYSIS_DATE.code}>", dateString)

            val resultString = strings["wallet.proof.${type.stringKey}.${SanitaryCertificate.SanitaryCertificateFields.TEST_RESULT.code}.$testResult"]
            text = text?.replace("<${SanitaryCertificate.SanitaryCertificateFields.TEST_RESULT.code}>", resultString ?: "N/A")

            if (testResult == "N") {
                val timeIndicator = if (isValid(configuration)) {
                    strings.stringsFormat("wallet.proof.lessThanSpecificHours", configuration.testCertificateValidityThresholdInHours)
                } else {
                    strings.stringsFormat("wallet.proof.moreThanSpecificHours", configuration.testCertificateValidityThresholdInHours)
                }
                text = text?.plus("\n$timeIndicator")
            }

            text ?: ""
        }
    }
}

fun WalletCertificate.tagStringKey(): String {
    return "wallet.proof.${type.stringKey}.pillTitle"
}

fun WalletCertificate.isRecent(configuration: Configuration): Boolean {
    return !isOld(configuration)
}

fun WalletCertificate.isOld(configuration: Configuration): Boolean {
    return System.currentTimeMillis() - timestamp > configuration.walletOldCertificateThresholdInMs(type)
}

fun SanitaryCertificate.isValid(configuration: Configuration): Boolean {
    return System.currentTimeMillis() - timestamp < configuration.certificateValidityThresholdInMs()
}
