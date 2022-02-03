package com.lunabeestudio.stopcovid.extension

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableStringBuilder
import com.lunabeestudio.domain.extension.walletOldCertificateThresholdInMs
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLocale
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.manager.Blacklist2DDOCManager
import com.lunabeestudio.stopcovid.manager.BlacklistDCCManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.Expired
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.SanitaryCertificate
import com.lunabeestudio.stopcovid.model.VaccinationCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours

fun WalletCertificate.fullNameList(configuration: Configuration): String {
    val prefix =
        configuration.dccKidsEmoji?.let { dccKidsEmoji ->
            birthDate()?.let { birthDate ->
                val maxKidEmojiCal = Calendar.getInstance().apply {
                    time = birthDate
                    add(Calendar.YEAR, dccKidsEmoji.age)
                }
                val nowCal = Calendar.getInstance()
                if (nowCal.before(maxKidEmojiCal)) {
                    val emojiIdx = (rawBirthDate() + fullName()).iOSCommonHash().mod(dccKidsEmoji.emojis.size)
                    dccKidsEmoji.emojis.getOrNull(emojiIdx).orEmpty() + " "
                } else {
                    null
                }
            }
        }.orEmpty()

    return (prefix + fullNameUppercase()).trim()
}

fun WalletCertificate.rawBirthDate(): String? {
    return when (this) {
        is EuropeanCertificate -> greenCertificate.dateOfBirth
        is FrenchCertificate -> birthDate
    }
}

fun WalletCertificate.birthDate(): Date? {
    return when (this) {
        is EuropeanCertificate -> yearMonthDayUsParserForceTimeZone().parseOrNull(greenCertificate.dateOfBirth)
        is FrenchCertificate -> birthDate?.let { dayMonthYearUsParser().parseOrNull(it) }
    }
}

fun WalletCertificate.fullNameUppercase(): String =
    fullName().uppercase()

fun WalletCertificate.fullName(): String =
    "${firstName.orEmpty()} ${name.orEmpty()}".trim()

fun WalletCertificate.titleDescription(strings: LocalizedStrings): String {
    val titleBuilder = SpannableStringBuilder()
    if ((this as? EuropeanCertificate)?.greenCertificate?.isFrench == false) {
        titleBuilder.append(this.greenCertificate.countryCode?.countryCodeToFlagEmoji?.plus(" ") ?: "")
    }
    titleBuilder.append(
        when (this) {
            is EuropeanCertificate -> {
                if (this.type == WalletCertificateType.MULTI_PASS) {
                    strings["wallet.proof.${type.code}.title"] ?: ""
                } else {
                    strings["wallet.proof.europe.${type.code}.title"] ?: ""
                }
            }
            else -> strings["wallet.proof.${type.stringKey}.title"] ?: ""
        }.trim()
    )
    return titleBuilder.toString()
}

fun WalletCertificate.infosDescription(strings: LocalizedStrings, configuration: Configuration, context: Context?): String {
    val text = when (type) {
        WalletCertificateType.SANITARY,
        WalletCertificateType.VACCINATION -> strings["wallet.proof.${type.stringKey}.infos"]
        WalletCertificateType.MULTI_PASS -> strings["wallet.proof.${type.code}.infos"]
        WalletCertificateType.SANITARY_EUROPE -> {
            if ((this as? EuropeanCertificate)?.greenCertificate?.testResultIsNegative == true) {
                strings["wallet.proof.europe.${type.code}.infos"]
            } else {
                strings["wallet.proof.europe.positiveTest.infos"]
            }
        }
        WalletCertificateType.EXEMPTION,
        WalletCertificateType.VACCINATION_EUROPE,
        WalletCertificateType.RECOVERY_EUROPE,
        WalletCertificateType.DCC_LIGHT -> strings["wallet.proof.europe.${type.code}.infos"]
    }
    return formatText(strings, configuration, text, false, context).trim()
}

fun WalletCertificate.fullDescription(strings: LocalizedStrings, configuration: Configuration, context: Context?): String {
    val text = when (this) {
        is EuropeanCertificate -> {
            if (this.type == WalletCertificateType.MULTI_PASS) {
                strings["wallet.proof.${type.code}.description"]
            } else {
                strings["wallet.proof.europe.${type.code}.description"]
            }
        }
        else -> strings["wallet.proof.${type.stringKey}.description"]
    }
    return formatText(strings, configuration, text, true, context).trim()
}

@SuppressLint("SimpleDateFormat")
private fun WalletCertificate.formatText(
    strings: LocalizedStrings,
    configuration: Configuration,
    textToFormat: String?,
    shouldAddFlag: Boolean,
    context: Context?,
): String {
    val dateFormat = shortDateFormat(context)
    val analysisDateFormat = shortDateTimeFormat(context)
    return when (this) {
        is FrenchCertificate -> formatFrenchCertificateText(
            strings = strings,
            configuration = configuration,
            textToFormat = textToFormat,
            dateFormat = dateFormat,
            analysisDateFormat = analysisDateFormat,
        )
        is EuropeanCertificate -> {
            val descriptionBuilder = StringBuilder()
            if (!this.greenCertificate.isFrench && shouldAddFlag) {
                descriptionBuilder.append(this.greenCertificate.countryCode?.countryCodeToFlagEmoji ?: "")
            }

            descriptionBuilder.append(
                formatDccText(
                    textToFormat,
                    strings,
                    dateFormat,
                    analysisDateFormat,
                    false,
                    configuration,
                )
            )

            descriptionBuilder.append(validityString(configuration, strings, false).let { "\n$it" })
            descriptionBuilder.toString()
        }
    }
}

private fun FrenchCertificate.formatFrenchCertificateText(
    strings: LocalizedStrings,
    configuration: Configuration,
    textToFormat: String?,
    dateFormat: SimpleDateFormat,
    analysisDateFormat: SimpleDateFormat,
): String {
    var text = textToFormat
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
                val timeIndicator = validityString(configuration, strings, false)
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
    }
}

fun EuropeanCertificate.fullScreenBorderDescription(
    strings: LocalizedStrings,
    configuration: Configuration,
): String {
    val descriptionBuilder = StringBuilder()
    descriptionBuilder.append(
        formatDccText(
            strings["europeanCertificate.fullscreen.englishDescription.${type.code}"],
            strings,
            shortDateFormat(Locale.ENGLISH),
            shortDateTimeFormat(Locale.ENGLISH),
            true,
        )
    )
    descriptionBuilder.append(validityString(configuration, strings, true).let { "\n$it" })

    return descriptionBuilder.toString()
}

fun EuropeanCertificate.fullScreenDescription(
    strings: LocalizedStrings,
    context: Context,
): String {
    return when (this.type) {
        WalletCertificateType.SANITARY,
        WalletCertificateType.VACCINATION,
        WalletCertificateType.SANITARY_EUROPE,
        WalletCertificateType.VACCINATION_EUROPE,
        WalletCertificateType.RECOVERY_EUROPE,
        WalletCertificateType.EXEMPTION,
        WalletCertificateType.DCC_LIGHT -> fullNameUppercase()
        WalletCertificateType.MULTI_PASS -> {
            val dateFormat = shortDateFormat(context)
            val analysisDateFormat = shortDateTimeFormat(context)
            formatDccText(
                inputText = strings["multiPassCertificate.fullscreen"],
                strings = strings,
                dateFormat = dateFormat,
                analysisDateFormat = analysisDateFormat,
                forceEnglish = false,
            )
        }
    }
}

fun EuropeanCertificate.multipassPickerDescription(
    strings: LocalizedStrings,
    context: Context,
): String {
    return formatDccText(
        strings["multiPass.selectionScreen.${this.type.code}.description"],
        strings,
        shortDateFormat(context),
        shortDateTimeFormat(context),
        false
    )
}

private fun shortDateTimeFormat(context: Context?): SimpleDateFormat = shortDateTimeFormat(context.getApplicationLocale())
private fun shortDateTimeFormat(locale: Locale): SimpleDateFormat = SimpleDateFormat("d MMM yyyy, HH:mm", locale)
fun shortDateFormat(context: Context?): SimpleDateFormat = shortDateFormat(context.getApplicationLocale())
private fun shortDateFormat(locale: Locale): SimpleDateFormat = SimpleDateFormat("d MMM yyyy", locale)

private fun EuropeanCertificate.formatDccText(
    inputText: String?,
    strings: LocalizedStrings,
    dateFormat: SimpleDateFormat,
    analysisDateFormat: SimpleDateFormat,
    forceEnglish: Boolean,
    configuration: Configuration? = null,
): String {
    var formattedText = inputText
    formattedText = formattedText?.replace("<FULL_NAME>", fullNameUppercase())
    formattedText = formattedText?.replace("<BIRTHDATE>", this.greenCertificate.formattedDateOfBirthDate(dateFormat))
    when (this.type) {
        WalletCertificateType.SANITARY,
        WalletCertificateType.VACCINATION -> {
            Timber.e("Unexpected type ${this.type} with ${this.javaClass.simpleName}")
        }
        WalletCertificateType.VACCINATION_EUROPE -> {
            val vaccineName = this.greenCertificate.vaccineMedicinalProduct?.let { strings["vac.product.$it"] ?: it }
            val vaccineDoseString = this.greenCertificate.vaccineDose?.let { "${it.first}/${it.second}" }

            formattedText = formattedText?.replace("<VACCINE_NAME>", vaccineName.toString().orNA())
            formattedText = formattedText?.replace("<VACCINE_DOSES>", vaccineDoseString.orEmpty())
            formattedText = formattedText?.replace("<DATE>", this.greenCertificate.vaccineDate?.let(dateFormat::format).orNA())
        }
        WalletCertificateType.RECOVERY_EUROPE -> {
            var fromDate: Date? = null
            var toDate: Date? = null
            configuration?.recoveryValidityThreshold?.let { recoveryValidityThreshold ->
                fromDate = greenCertificate.recoveryDateOfFirstPositiveTest?.time
                    ?.plus(recoveryValidityThreshold.min.inWholeMilliseconds)
                    ?.let { Date(it) }
                toDate = smartWalletState(configuration).expirationDate
            }

            formattedText = formattedText?.replace(
                "<DATE>",
                this.greenCertificate.recoveryDateOfFirstPositiveTest?.let(dateFormat::format).orNA()
            )
            formattedText = formattedText?.replace(
                "<FROM_DATE>",
                fromDate?.let(dateFormat::format).orNA()
            )
            formattedText = formattedText?.replace(
                "<TO_DATE>",
                toDate?.let(dateFormat::format).orNA()
            )
        }
        WalletCertificateType.SANITARY_EUROPE -> {
            val testDateFormat: SimpleDateFormat
            var fromDate: Date? = null
            var toDate: Date? = null
            if (greenCertificate.testResultIsNegative != true) {
                testDateFormat = dateFormat

                configuration?.recoveryValidityThreshold?.let { recoveryValidityThreshold ->
                    fromDate = greenCertificate.testDateTimeOfCollection?.time
                        ?.plus(recoveryValidityThreshold.min.inWholeMilliseconds)
                        ?.let { Date(it) }
                    toDate = smartWalletState(configuration).expirationDate
                }
            } else {
                testDateFormat = analysisDateFormat
                fromDate = this.greenCertificate.testDateTimeOfCollection
                toDate = null
            }

            val testName = this.greenCertificate.testType?.let {
                val testNameStringKey = StringBuilder("test.man.")
                if (forceEnglish) {
                    testNameStringKey.append("englishDescription.")
                }
                testNameStringKey.append(it)
                strings[testNameStringKey.toString()]
            }
            formattedText = formattedText?.replace("<ANALYSIS_CODE>", testName.orNA())

            val testResult = this.greenCertificate.testResultCode?.let {
                val testResultStringKey = StringBuilder("wallet.proof.europe.test.")
                if (forceEnglish) {
                    testResultStringKey.append("englishDescription.")
                }
                testResultStringKey.append(it)
                strings[testResultStringKey.toString()]
            }
            formattedText = formattedText?.replace("<ANALYSIS_RESULT>", testResult.orNA())
            formattedText = formattedText?.replace(
                "<DATE>",
                this.greenCertificate.testDateTimeOfCollection?.let(testDateFormat::format).orNA()
            )
            formattedText = formattedText?.replace(
                "<FROM_DATE>",
                fromDate?.let(testDateFormat::format).orNA()
            )
            formattedText = formattedText?.replace(
                "<TO_DATE>",
                toDate?.let(testDateFormat::format).orNA()
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
        WalletCertificateType.DCC_LIGHT -> {
            /* no-op */
        }
        WalletCertificateType.MULTI_PASS -> {
            val testResult = this.greenCertificate.testResultCode?.let {
                val testResultStringKey = StringBuilder("wallet.proof.europe.test.")
                testResultStringKey.append(it)
                strings[testResultStringKey.toString()]
            }
            formattedText = formattedText?.replace("<ANALYSIS_RESULT>", testResult.orNA())
            formattedText = formattedText?.replace(
                "<TO_DATE>",
                analysisDateFormat.format(this.expirationTime).orNA(),
            )
        }
    }
    return formattedText.orEmpty()
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
    if (this.isSignatureExpired)
        return "wallet.expired.pillTitle"

    return when (this.type) {
        WalletCertificateType.VACCINATION,
        WalletCertificateType.VACCINATION_EUROPE,
        -> "wallet.proof.vaccinationCertificate.LA." + this.greenCertificate.vaccineDose?.let {
                                (first, second) -> when { (first == second) -> "TE" else -> "CO" }}
        WalletCertificateType.RECOVERY_EUROPE -> "enum.HCertType.recovery"
        WalletCertificateType.SANITARY_EUROPE -> "enum.HCertType.test"
        WalletCertificateType.EXEMPTION -> "enum.HCertType.exemption"
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
    return (configuration.walletOldCertificateThresholdInMs(type)?.let { System.currentTimeMillis() - timestamp > it } ?: false)
        || (this as? EuropeanCertificate)?.isSignatureExpired == true
        || (this as? EuropeanCertificate)?.smartWalletState(configuration) is Expired
}

private fun WalletCertificate.validityString(configuration: Configuration, strings: LocalizedStrings, forceEnglish: Boolean): String {
    if (type != WalletCertificateType.SANITARY &&
        type != WalletCertificateType.SANITARY_EUROPE
        || (this as? EuropeanCertificate)?.greenCertificate?.testResultIsNegative != true
    ) {
        return ""
    }

    val testCertificateValidityThresholds = configuration.testCertificateValidityThresholds
    val timeSinceCreation = System.currentTimeMillis() - timestamp
    val validityThresholdInHours = testCertificateValidityThresholds
        .filter { TimeUnit.HOURS.toMillis(it.toLong()) > timeSinceCreation }
        .minOrNull()

    val validityStringKeyBuilder = StringBuilder("wallet.proof.")

    if (forceEnglish) {
        validityStringKeyBuilder.append("englishDescription.")
    }

    return if (validityThresholdInHours != null) {
        validityStringKeyBuilder.append("lessThanSpecificHours")
        strings.stringsFormat(validityStringKeyBuilder.toString(), validityThresholdInHours)
    } else {
        validityStringKeyBuilder.append("moreThanSpecificHours")
        val maxValidityInHours = testCertificateValidityThresholds.maxOrNull() ?: 0
        strings.stringsFormat(validityStringKeyBuilder.toString(), maxValidityInHours)
    }.orEmpty()
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

suspend fun WalletCertificate.isEligibleForActivityPass(
    blacklistDCCManager: BlacklistDCCManager,
    configuration: Configuration,
): Boolean {
    if (this.type !in arrayOf(
            WalletCertificateType.SANITARY_EUROPE,
            WalletCertificateType.RECOVERY_EUROPE,
            WalletCertificateType.VACCINATION_EUROPE
        )
    ) return false

    if ((this as? EuropeanCertificate)?.isSignatureExpired == true) {
        return false
    }

    if ((this as? EuropeanCertificate)?.canRenewActivityPass == false) {
        return false
    }

    if (this.type == WalletCertificateType.SANITARY_EUROPE &&
        timestamp + configuration.activityPassSkipNegTestHours.hours.inWholeMilliseconds < System.currentTimeMillis()
    ) {
        return false
    }

    if (blacklistDCCManager.isBlacklisted(sha256)) return false

    if ((this as? EuropeanCertificate)?.smartWalletState(configuration) is Expired) {
        return false
    }

    return true
}

suspend fun WalletCertificate.isEligibleForSmartWallet(
    blacklistDCCManager: BlacklistDCCManager,
): Boolean {
    if (this !is EuropeanCertificate) return false

    val isCompleteVaccine =
        type == WalletCertificateType.VACCINATION_EUROPE
            && (greenCertificate.vaccineDose?.let { (first, second) -> first == second } == true)
    val isRecovery = greenCertificate.isRecoveryOrTestPositive
    val isBlacklisted = isBlacklisted(blacklistDCCManager)

    return (isCompleteVaccine || isRecovery) && !isBlacklisted && !isSignatureExpired
}

val EuropeanCertificate.isSignatureExpired: Boolean
    get() = expirationTime < System.currentTimeMillis()

fun EuropeanCertificate.isExpired(configuration: Configuration): Boolean {
    val positiveTestOrRecoveryDate = greenCertificate.positiveTestOrRecoveryDate

    return if (positiveTestOrRecoveryDate != null) {
        smartWalletState(configuration).expirationDate?.time ?: Long.MAX_VALUE < System.currentTimeMillis()
    } else {
        isSignatureExpired
    }
}

suspend fun EuropeanCertificate.isBlacklisted(blacklistDCCManager: BlacklistDCCManager): Boolean =
    blacklistDCCManager.isBlacklisted(sha256)

suspend fun FrenchCertificate.isBlacklisted(blacklist2DDOCManager: Blacklist2DDOCManager): Boolean =
    blacklist2DDOCManager.isBlacklisted(sha256)

// Use 1sec of threshold due to sec -> ms rounding
fun EuropeanCertificate.activityPassValidFuture(): Boolean = if (type == WalletCertificateType.DCC_LIGHT) {
    System.currentTimeMillis() < timestamp - 1_000
} else {
    false
}

fun TacResult<List<WalletCertificate>>.toRaw(): TacResult<List<RawWalletCertificate>> {
    return when (this) {
        is TacResult.Failure -> TacResult.Failure(throwable, failureData?.map { it.raw })
        is TacResult.Loading -> TacResult.Loading(partialData?.map { it.raw }, progress)
        is TacResult.Success -> TacResult.Success(successData.map { it.raw })
    }
}
