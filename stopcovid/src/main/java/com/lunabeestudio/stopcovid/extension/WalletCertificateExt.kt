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
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.SanitaryCertificate
import com.lunabeestudio.stopcovid.model.SmartWalletState
import com.lunabeestudio.stopcovid.model.VaccinationCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.usecase.ComputeDccValidityUseCase
import com.lunabeestudio.stopcovid.usecase.GetSmartWalletStateUseCase
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

fun WalletCertificate.infosDescription(
    strings: LocalizedStrings,
    configuration: Configuration,
    context: Context?,
    smartWalletState: SmartWalletState?
): String {

    val text = when (type) {
        WalletCertificateType.SANITARY,
        WalletCertificateType.VACCINATION -> strings["wallet.proof.${type.stringKey}.infos"]
        WalletCertificateType.MULTI_PASS,
        WalletCertificateType.SANITARY_EUROPE,
        WalletCertificateType.EXEMPTION,
        WalletCertificateType.VACCINATION_EUROPE,
        WalletCertificateType.RECOVERY_EUROPE,
        WalletCertificateType.DCC_LIGHT -> strings["wallet.proof.europe.${type.code}.identity"]
    }
    return formatText(strings, configuration, text, false, context, smartWalletState).trim()
}

fun WalletCertificate.fullDescription(
    strings: LocalizedStrings,
    configuration: Configuration,
    context: Context?,
    smartWalletState: SmartWalletState?
): String {
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
    return formatText(strings, configuration, text, true, context, smartWalletState).trim()
}

@SuppressLint("SimpleDateFormat")
private fun WalletCertificate.formatText(
    strings: LocalizedStrings,
    configuration: Configuration,
    textToFormat: String?,
    shouldAddFlag: Boolean,
    context: Context?,
    smartWalletState: SmartWalletState?,
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
                    smartWalletState,
                )
            )

            descriptionBuilder.append(
                validityString(
                    configuration = configuration,
                    strings = strings,
                    forceEnglish = false,
                    smartWalletState = smartWalletState,
                    dateFormat = dateFormat,
                    analysisDateFormat = analysisDateFormat
                ).let { "\n$it" }
            )
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
                val timeIndicator = validityString(
                    configuration = configuration,
                    strings = strings,
                    forceEnglish = false,
                    smartWalletState = null,
                    dateFormat = dateFormat,
                    analysisDateFormat = analysisDateFormat,
                )
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
    smartWalletState: SmartWalletState?,
): String {
    val descriptionBuilder = StringBuilder()
    val dateFormat = shortDateFormat(Locale.ENGLISH)
    val analysisDateFormat = shortDateTimeFormat(Locale.ENGLISH)
    descriptionBuilder.append(
        formatDccText(
            inputText = strings["europeanCertificate.fullscreen.englishDescription.${type.code}"],
            strings = strings,
            dateFormat = dateFormat,
            dateTimeFormat = analysisDateFormat,
            forceEnglish = true,
            smartWalletState = smartWalletState,
        )
    )

    descriptionBuilder.append(
        validityString(
            configuration = configuration,
            strings = strings,
            forceEnglish = true,
            smartWalletState = null,
            dateFormat = dateFormat,
            analysisDateFormat = analysisDateFormat
        ).let { "\n$it" }
    )

    return descriptionBuilder.toString()
}

fun EuropeanCertificate.fullScreenDescription(
    strings: LocalizedStrings,
    context: Context,
    smartWalletState: SmartWalletState,
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
            val dateTimeFormat = shortDateTimeFormat(context)
            formatDccText(
                // Fix <X_DATE> vs <X_DATE_TIME>
                inputText = strings["multiPassCertificate.fullscreen"]
                    ?.replace("<TO_DATE>", "<TO_DATE_TIME>")
                    ?.replace("<FROM_DATE>", "<FROM_DATE_TIME>"),
                strings = strings,
                dateFormat = dateFormat,
                dateTimeFormat = dateTimeFormat,
                forceEnglish = false,
                smartWalletState = smartWalletState,
            )
        }
    }
}

fun EuropeanCertificate.multipassPickerDescription(
    strings: LocalizedStrings,
    context: Context,
): String {
    return formatDccText(
        inputText = strings["multiPass.selectionScreen.${this.type.code}.description"],
        strings = strings,
        dateFormat = shortDateFormat(context),
        dateTimeFormat = shortDateTimeFormat(context),
        forceEnglish = false,
        smartWalletState = null,
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
    dateTimeFormat: SimpleDateFormat,
    forceEnglish: Boolean,
    smartWalletState: SmartWalletState?,
): String {
    var formattedText = inputText

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
            formattedText = formattedText?.replace(
                "<DATE>",
                this.greenCertificate.recoveryDateOfFirstPositiveTest?.let(dateFormat::format).orNA()
            )
        }
        WalletCertificateType.SANITARY_EUROPE -> {
            val testDateFormat: SimpleDateFormat = if (greenCertificate.testResultIsNegative != true) {
                dateFormat
            } else {
                dateTimeFormat
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
        }
        WalletCertificateType.EXEMPTION,
        WalletCertificateType.DCC_LIGHT,
        WalletCertificateType.MULTI_PASS -> {
            /* no-op */
        }
    }

    // Generic fallback placeholder
    formattedText = formattedText?.replace("<DATE>", dateTimeFormat.format(this.timestamp))
    formattedText = formattedText?.replace("<FULL_NAME>", fullNameUppercase())
    formattedText = formattedText?.replace("<BIRTHDATE>", this.birthDate()?.let(dateFormat::format) ?: greenCertificate.dateOfBirth)
    formattedText = formattedText?.replace(
        "<FROM_DATE>",
        smartWalletState?.smartWalletValidity?.start?.let(dateFormat::format).orNA()
    )
    formattedText = formattedText?.replace(
        "<FROM_DATE_TIME>",
        smartWalletState?.smartWalletValidity?.start?.let(dateTimeFormat::format).orNA()
    )
    formattedText = formattedText?.replace(
        "<TO_DATE>",
        smartWalletState?.smartWalletValidity?.end?.let(dateFormat::format).orNA()
    )
    formattedText = formattedText?.replace(
        "<TO_DATE_TIME>",
        smartWalletState?.smartWalletValidity?.end?.let(dateTimeFormat::format).orNA()
    )

    return formattedText.orEmpty()
}

fun VaccinationCertificate.statusStringKey(): String {
    val vaxCode = VaccinationCertificate.VaccinationCertificateFields.VACCINATION_CYCLE_STATE.code
    return "wallet.proof.${type.stringKey}.$vaxCode.$vaccinationCycleState"
}

fun WalletCertificate.tagStringKey(): String {
    return "wallet.proof.${type.stringKey}.pillTitle"
}

fun WalletCertificate.isRecent(configuration: Configuration, getSmartWalletStateUseCase: GetSmartWalletStateUseCase): Boolean {
    return !isOld(configuration, getSmartWalletStateUseCase)
}

fun WalletCertificate.isOld(configuration: Configuration, getSmartWalletStateUseCase: GetSmartWalletStateUseCase): Boolean {
    return (configuration.walletOldCertificateThresholdInMs(type)?.let { System.currentTimeMillis() - timestamp > it } ?: false)
        || (this as? EuropeanCertificate)?.isSignatureExpired == true
        || (this as? EuropeanCertificate)?.let { getSmartWalletStateUseCase(it) } is SmartWalletState.Expired
}

private fun WalletCertificate.validityString(
    configuration: Configuration,
    strings: LocalizedStrings,
    forceEnglish: Boolean,
    smartWalletState: SmartWalletState?,
    dateFormat: SimpleDateFormat,
    analysisDateFormat: SimpleDateFormat,
): String {
    return if (this is EuropeanCertificate &&
        (
            type != WalletCertificateType.SANITARY &&
                (
                    type != WalletCertificateType.SANITARY_EUROPE
                        || this.greenCertificate.testResultIsNegative != true
                    )
            )
    ) {
        smartWalletState?.smartWalletValidity?.let { validity ->
            val validityStart = validity.start
            val validityEnd = validity.end

            val code = if (type == WalletCertificateType.SANITARY_EUROPE &&
                greenCertificate.testResultIsNegative == false
            ) {
                "positiveTest"
            } else {
                type.code
            }

            when {
                validityStart != null && validityEnd != null -> "smartwallet.$code.valid.startend"
                validityStart != null && validityEnd == null -> "smartwallet.$code.valid.start"
                validityStart == null && validityEnd != null -> "smartwallet.$code.valid.end"
                else -> null
            }
        }?.let { key ->
            formatDccText(
                strings[key],
                strings,
                dateFormat,
                analysisDateFormat,
                false,
                smartWalletState,
            )
        }.orEmpty()
    } else {
        val testCertificateValidityThresholds = configuration.testCertificateValidityThresholds
        val timeSinceCreation = System.currentTimeMillis() - timestamp
        val validityThresholdInHours = testCertificateValidityThresholds
            .filter { TimeUnit.HOURS.toMillis(it.toLong()) > timeSinceCreation }
            .minOrNull()

        val validityStringKeyBuilder = StringBuilder("wallet.proof.")

        if (forceEnglish) {
            validityStringKeyBuilder.append("englishDescription.")
        }

        if (validityThresholdInHours != null) {
            validityStringKeyBuilder.append("lessThanSpecificHours")
            strings.stringsFormat(validityStringKeyBuilder.toString(), validityThresholdInHours)
        } else {
            validityStringKeyBuilder.append("moreThanSpecificHours")
            val maxValidityInHours = testCertificateValidityThresholds.maxOrNull() ?: 0
            strings.stringsFormat(validityStringKeyBuilder.toString(), maxValidityInHours)
        }.orEmpty()
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

suspend fun WalletCertificate.isEligibleForActivityPass(
    blacklistDCCManager: BlacklistDCCManager,
    configuration: Configuration,
    getSmartWalletStateUseCase: GetSmartWalletStateUseCase,
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

    if ((this as? EuropeanCertificate)?.let { getSmartWalletStateUseCase(it) } is SmartWalletState.Expired) {
        return false
    }

    return true
}

suspend fun WalletCertificate.isEligibleForSmartWallet(
    blacklistDCCManager: BlacklistDCCManager,
): Boolean {
    if (this !is EuropeanCertificate) return false

    val isCompleteVaccine = isCompleteVaccine()
    val isRecoveryOrTestPositive = greenCertificate.isRecoveryOrTestPositive
    val isBlacklisted = isBlacklisted(blacklistDCCManager)

    return (isCompleteVaccine || isRecoveryOrTestPositive) && !isBlacklisted && !isSignatureExpired
}

fun EuropeanCertificate.isCompleteVaccine(): Boolean = (
    type == WalletCertificateType.VACCINATION_EUROPE
        && (greenCertificate.vaccineDose?.let { (first, second) -> first >= second } == true)
    )

val EuropeanCertificate.isSignatureExpired: Boolean
    get() = expirationTime < System.currentTimeMillis()

fun EuropeanCertificate.isExpired(
    computeDccValidityUseCase: ComputeDccValidityUseCase,
): Boolean {
    val positiveTestOrRecoveryDate = greenCertificate.positiveTestOrRecoveryDate

    return if (positiveTestOrRecoveryDate != null) {
        computeDccValidityUseCase(this)?.end?.time ?: Long.MAX_VALUE < System.currentTimeMillis()
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
