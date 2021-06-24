package com.lunabeestudio.stopcovid.model

import android.util.Base64
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.framework.crypto.BouncyCastleSignatureVerifier
import com.lunabeestudio.stopcovid.extension.certificateType
import com.lunabeestudio.stopcovid.extension.recoveryValidFrom
import com.lunabeestudio.stopcovid.extension.testDateTimeOfCollection
import com.lunabeestudio.stopcovid.extension.vaccineDate
import dgca.verifier.app.decoder.base45.DefaultBase45Service
import dgca.verifier.app.decoder.base64ToX509Certificate
import dgca.verifier.app.decoder.cbor.DefaultCborService
import dgca.verifier.app.decoder.compression.DefaultCompressorService
import dgca.verifier.app.decoder.cose.DefaultCoseService
import dgca.verifier.app.decoder.cose.VerificationCryptoService
import dgca.verifier.app.decoder.model.GreenCertificate
import dgca.verifier.app.decoder.model.VerificationResult
import dgca.verifier.app.decoder.prefixvalidation.DefaultPrefixValidationService
import dgca.verifier.app.decoder.schema.DefaultSchemaValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.security.SignatureException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class WalletCertificate(
    open val value: String,
) {
    abstract val type: WalletCertificateType

    var keyCertificateId: String = ""
    var firstName: String? = null
    var name: String? = null

    abstract val timestamp: Long

    abstract fun parse()

    @Throws(IllegalArgumentException::class, WalletCertificateInvalidSignatureException::class, IllegalStateException::class)
    abstract fun verifyKey(publicKey: String)

    companion object {
        suspend fun fromValue(value: String): WalletCertificate? {
            return withContext(Dispatchers.Default) {
                SanitaryCertificate.fromValue(value) ?: VaccinationCertificate.fromValue(value) ?: EuropeanCertificate.fromValue(value)
            }
        }

        suspend fun getTypeFromValue(value: String): WalletCertificateType? {
            return withContext(Dispatchers.Default) {
                SanitaryCertificate.getTypeFromValue(value)
                    ?: VaccinationCertificate.getTypeFromValue(value)
                    ?: EuropeanCertificate.getTypeFromValue(value)
            }
        }
    }
}

sealed class FrenchCertificate(value: String) : WalletCertificate(value) {
    var keyAuthority: String = ""
    var keySignature: String = ""

    enum class Separator(
        val ascii: String,
    ) {
        UNIT("\u001F"),
    }

    protected fun parseBirthDate(dateString: String): String {
        return "${dateString.substring(0, 2)}-${dateString.substring(2, 4)}-${dateString.substring(4)}"
    }

    override fun verifyKey(publicKey: String) {
        val split = value.split(Separator.UNIT.ascii)
        val message = split.getOrNull(0)
        val signature = split.getOrNull(1)

        if (message == null || signature == null) {
            throw WalletCertificateMalformedException()
        }

        val verifySignature = try {
            BouncyCastleSignatureVerifier.verifySignature(
                rawPublicKey = publicKey,
                message = message,
                rawSignature = signature,
            )
        } catch (e: SignatureException) {
            throw WalletCertificateInvalidSignatureException()
        }

        if (!verifySignature) {
            throw WalletCertificateInvalidSignatureException()
        }
    }
}

class SanitaryCertificate private constructor(override val value: String) : FrenchCertificate(value) {
    override val type: WalletCertificateType = WalletCertificateType.SANITARY

    var birthDate: String? = null
    var gender: String? = null
    var testResult: String? = null
    var analysisDate: Long? = null
    var analysisCode: String? = null

    override val timestamp: Long
        get() = analysisDate ?: System.currentTimeMillis()

    override fun parse() {
        val matchResult = validationRegex.find(value)

        if (matchResult != null && matchResult.groups.size == SanitaryCertificateFields.values().size) {
            keyAuthority = matchResult.groups[SanitaryCertificateFields.KEY_AUTHORITY.ordinal]?.value ?: ""
            keyCertificateId = matchResult.groups[SanitaryCertificateFields.CERTIFICATE_ID.ordinal]?.value ?: ""

            firstName = matchResult.groups[SanitaryCertificateFields.FIRST_NAME.ordinal]?.value?.replace("/", ", ") ?: ""
            name = matchResult.groups[SanitaryCertificateFields.NAME.ordinal]?.value ?: ""

            matchResult.groups[VaccinationCertificate.VaccinationCertificateFields.BIRTH_DATE.ordinal]?.value?.let {
                birthDate = parseBirthDate(it)
            }

            gender = matchResult.groups[SanitaryCertificateFields.GENDER.ordinal]?.value ?: ""
            analysisCode = matchResult.groups[SanitaryCertificateFields.ANALYSIS_CODE.ordinal]?.value ?: ""
            testResult = matchResult.groups[SanitaryCertificateFields.TEST_RESULT.ordinal]?.value ?: ""

            val analysisDateParser = SimpleDateFormat("ddMMyyyyHHmm", Locale.US)
            analysisDate = matchResult.groups[SanitaryCertificateFields.ANALYSIS_DATE.ordinal]?.value?.let {
                analysisDateParser.parse(it)?.time
            }

            keySignature = matchResult.groups[SanitaryCertificateFields.SIGNATURE.ordinal]?.value ?: ""
        } else {
            throw WalletCertificateMalformedException()
        }
    }

    enum class SanitaryCertificateFields(val code: String?) {
        CONTENT(null), // not used but this is to avoid the +1 for all fields.
        KEY_AUTHORITY(null),
        CERTIFICATE_ID(null),
        FIRST_NAME("F0"),
        NAME("F1"),
        BIRTH_DATE("F2"),
        GENDER("F3"),
        ANALYSIS_CODE("F4"),
        TEST_RESULT("F5"),
        ANALYSIS_DATE("F6"),
        SIGNATURE(null)
    }

    companion object {
        private val validationRegex: Regex = "^[A-Z\\d]{4}" // Characters 0 to 3 are ignored. They represent the document format version.
            .plus("([A-Z\\d]{4})") // 1 - Characters 4 to 7 represent the document signing authority.
            .plus("([A-Z\\d]{4})") // 2 - Characters 8 to 11 represent the id of the certificate used to sign the document.
            .plus("[A-Z\\d]{8}") // Characters 12 to 19 are ignored.
            .plus("B2") // Characters 20 and 21 represent the wallet certificate type (sanitary, ...)
            .plus("[A-Z\\d]{4}") // Characters 22 to 25 are ignored.
            .plus("F0([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 3 - We capture the field F0. It must have at least one character.
            .plus("F1([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 4 - We capture the field F1. It must have at least one character.
            .plus("F2(\\d{8})") // 5 - We capture the field F2. It can only contain digits.
            .plus("F3([FMU]{1})") // 6 - We capture the field F3. It can only contain "F", "M" or "U".
            .plus("F4([A-Z\\d]{3,7})\\x1D?") // 7 - We capture the field F4. It can contain 3 to 7 uppercase letters and/or digits. It can also be ended by the GS ASCII char (29) if the field reaches its max length.
            .plus("F5([PNIX]{1})") // 8 - We capture the field F5. It can only contain "P", "N", "I" or "X".
            .plus("F6(\\d{12})") // 9 - We capture the field F6. It can only contain digits.
            .plus("\\x1F{1}") // This character is separating the message from its signature.
            .plus("([A-Z\\d\\=]+)$").toRegex() // 10 - This is the message signature.

        private val headerDetectionRegex: Regex = "^[A-Z\\d]{4}" // Characters 0 to 3 are ignored. They represent the document format version.
            .plus("([A-Z\\d]{4})") // 1 - Characters 4 to 7 represent the document signing authority.
            .plus("([A-Z\\d]{4})") // 2 - Characters 8 to 11 represent the id of the certificate used to sign the document.
            .plus("[A-Z\\d]{8}") // Characters 12 to 19 are ignored.
            .plus("B2") // Characters 20 and 21 represent the wallet certificate type (sanitary, ...)
            .toRegex()

        fun fromValue(value: String): SanitaryCertificate? = if (validationRegex.matches(value)) {
            SanitaryCertificate(value)
        } else {
            null
        }

        fun getTypeFromValue(value: String): WalletCertificateType? = if (headerDetectionRegex.containsMatchIn(value)) {
            WalletCertificateType.SANITARY
        } else {
            null
        }
    }
}

class VaccinationCertificate private constructor(override val value: String) : FrenchCertificate(value) {
    override val type: WalletCertificateType = WalletCertificateType.VACCINATION

    var birthDate: String? = null
    var diseaseName: String? = null
    var prophylacticAgent: String? = null
    var vaccineName: String? = null
    var vaccineMaker: String? = null
    var lastVaccinationStateRank: String? = null
    var completeCycleDosesCount: String? = null
    var lastVaccinationDate: Date? = null
    var vaccinationCycleState: String? = null

    override val timestamp: Long
        get() = lastVaccinationDate?.time ?: 0L

    override fun parse() {
        val matchResult = validationRegex.find(value)

        if (matchResult != null && matchResult.groups.size == VaccinationCertificateFields.values().size) {
            keyAuthority = matchResult.groups[VaccinationCertificateFields.KEY_AUTHORITY.ordinal]?.value ?: ""
            keyCertificateId = matchResult.groups[VaccinationCertificateFields.CERTIFICATE_ID.ordinal]?.value ?: ""

            firstName = matchResult.groups[VaccinationCertificateFields.FIRST_NAME.ordinal]?.value?.replace("/", ", ") ?: ""
            name = matchResult.groups[VaccinationCertificateFields.NAME.ordinal]?.value ?: ""

            matchResult.groups[VaccinationCertificateFields.BIRTH_DATE.ordinal]?.value?.let {
                birthDate = parseBirthDate(it)
            }

            diseaseName = matchResult.groups[VaccinationCertificateFields.DISEASE_NAME.ordinal]?.value
            prophylacticAgent = matchResult.groups[VaccinationCertificateFields.PROPHYLACTIC_AGENT.ordinal]?.value
            vaccineName = matchResult.groups[VaccinationCertificateFields.VACCINE_NAME.ordinal]?.value
            vaccineMaker = matchResult.groups[VaccinationCertificateFields.VACCINE_MAKER.ordinal]?.value

            lastVaccinationStateRank = matchResult.groups[VaccinationCertificateFields.LAST_VACCINATION_STATE_RANK.ordinal]?.value
            completeCycleDosesCount = matchResult.groups[VaccinationCertificateFields.COMPLETE_CYCLE_DOSE_COUNT.ordinal]?.value

            matchResult.groups[VaccinationCertificateFields.LAST_VACCINATION_DATE.ordinal]?.value?.let {
                lastVaccinationDate = parseVaccinationDate(it)
            }

            vaccinationCycleState = matchResult.groups[VaccinationCertificateFields.VACCINATION_CYCLE_STATE.ordinal]?.value
        } else {
            throw WalletCertificateMalformedException()
        }
    }

    private fun parseVaccinationDate(dateString: String): Date? {
        val dateParser = SimpleDateFormat("ddMMyyyy", Locale.US)
        return dateParser.parse(dateString)
    }

    enum class VaccinationCertificateFields(val code: String?) {
        CONTENT(null), // not used but this is to avoid the +1 for all fields.
        KEY_AUTHORITY(null),
        CERTIFICATE_ID(null),
        NAME("L0"),
        FIRST_NAME("L1"),
        BIRTH_DATE("L2"), // Can be a lunar date.
        DISEASE_NAME("L3"),
        PROPHYLACTIC_AGENT("L4"),
        VACCINE_NAME("L5"),
        VACCINE_MAKER("L6"),
        LAST_VACCINATION_STATE_RANK("L7"),
        COMPLETE_CYCLE_DOSE_COUNT("L8"),
        LAST_VACCINATION_DATE("L9"),
        VACCINATION_CYCLE_STATE("LA"),
        SIGNATURE(null)
    }

    companion object {
        private val validationRegex: Regex = "^[A-Z\\d]{4}" // Characters 0 to 3 are ignored. They represent the document format version.
            .plus("([A-Z\\d]{4})") // 1 - Characters 4 to 7 represent the document signing authority.
            .plus("([A-Z\\d]{4})") // 2 - Characters 8 to 11 represent the id of the certificate used to sign the document.
            .plus("[A-Z\\d]{8}") // Characters 12 to 19 are ignored.
            .plus("L1") // Characters 20 and 21 represent the wallet certificate type (sanitary, ...)
            .plus("[A-Z\\d]{4}") // Characters 22 to 25 are ignored.
            .plus("L0([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 3 - We capture the field L0. It can contain uppercased letters and spaces. It can also be ended by the GS ASCII char (29) if the field reaches its max length.
            .plus("L1([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 4 - We capture the field L1. It must have at least one character.
            .plus("L2(\\d{8})\\x1D?") // 5 - We capture the field L2. It can only contain 8 digits.
            .plus("L3([^\\x1D\\x1E]*)[\\x1D\\x1E]") // // 6 - We capture the field L3. It can contain any characters.
            .plus("L4([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 7 - We capture the field L4. It must have at least one character
            .plus("L5([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 8 - We capture the field L5. It must have at least one character
            .plus("L6([^\\x1D\\x1E]+)[\\x1D\\x1E]") // 9 - We capture the field L6. It must have at least one character
            .plus("L7(\\d{1})") // 10 - We capture the field L7. It can contain only one digit.
            .plus("L8(\\d{1})") // 11 - We capture the field L8. It can contain only one digit.
            .plus("L9(\\d{8})") // 12 - We capture the field L9. It can only contain 8 digits.
            .plus("LA([A-Z\\d]{2})") // 13 - We capture the field LA. 2 characters letters or digits
            .plus("\\x1F{1}") // This character is separating the message from its signature.
            .plus("([A-Z\\d\\=]+)$").toRegex() // 14 - This is the message signature.

        private val headerDetectionRegex: Regex = "^[A-Z\\d]{4}" // Characters 0 to 3 are ignored. They represent the document format version.
            .plus("([A-Z\\d]{4})") // 1 - Characters 4 to 7 represent the document signing authority.
            .plus("([A-Z\\d]{4})") // 2 - Characters 8 to 11 represent the id of the certificate used to sign the document.
            .plus("[A-Z\\d]{8}") // Characters 12 to 19 are ignored.
            .plus("L1") // Characters 20 and 21 represent the wallet certificate type (sanitary, ...)
            .toRegex()

        fun fromValue(value: String): VaccinationCertificate? = if (validationRegex.matches(value)) {
            VaccinationCertificate(value)
        } else {
            null
        }

        fun getTypeFromValue(value: String): WalletCertificateType? = if (headerDetectionRegex.containsMatchIn(value)) {
            WalletCertificateType.VACCINATION
        } else {
            null
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
class EuropeanCertificate private constructor(value: String) : WalletCertificate(value) {
    override val timestamp: Long
    override val type: WalletCertificateType
    private val kid: ByteArray
    private val cose: ByteArray
    val greenCertificate: GreenCertificate

    init {
        val verificationResult = VerificationResult()
        val plainInput = DefaultPrefixValidationService().decode(value, verificationResult)
        val compressedCose = DefaultBase45Service().decode(plainInput, verificationResult)
        cose = DefaultCompressorService().decode(compressedCose, verificationResult)
        val coseData = checkNotNull(DefaultCoseService().decode(cose, verificationResult))
        DefaultSchemaValidator().validate(coseData.cbor, verificationResult)
        if (!verificationResult.isSchemaValid) {
            throw WalletCertificateMalformedException()
        }
        kid = checkNotNull(coseData.kid)
        greenCertificate = checkNotNull(DefaultCborService().decode(coseData.cbor, verificationResult))
        type = checkNotNull(greenCertificate.certificateType)
        timestamp = when (type) {
            WalletCertificateType.SANITARY,
            WalletCertificateType.VACCINATION -> null
            WalletCertificateType.SANITARY_EUROPE -> greenCertificate.testDateTimeOfCollection?.time
            WalletCertificateType.VACCINATION_EUROPE -> greenCertificate.vaccineDate?.time
            WalletCertificateType.RECOVERY_EUROPE -> greenCertificate.recoveryValidFrom?.time
        } ?: -1
    }

    override fun parse() {
        keyCertificateId = Base64.encodeToString(kid, Base64.NO_WRAP)
        firstName = greenCertificate.person.givenName
        name = greenCertificate.person.familyName
    }

    override fun verifyKey(publicKey: String) {
        val verificationResult = VerificationResult()
        val certificate = publicKey.base64ToX509Certificate()
        checkNotNull(certificate) { "Fail to get X509 certificate from public key $publicKey" }
        VerificationCryptoService().validate(cose, certificate, verificationResult)
        if (!verificationResult.coseVerified) {
            throw WalletCertificateInvalidSignatureException()
        }
    }

    companion object {
        fun fromValue(value: String): EuropeanCertificate? {
            return try {
                EuropeanCertificate(value)
            } catch (e: IllegalStateException) {
                Timber.e(e)
                null
            }
        }

        fun getTypeFromValue(value: String): WalletCertificateType? {
            return try {
                val verificationResult = VerificationResult()
                val plainInput = DefaultPrefixValidationService().decode(value, verificationResult)
                val compressedCose = DefaultBase45Service().decode(plainInput, verificationResult)
                val cose = DefaultCompressorService().decode(compressedCose, verificationResult)
                val coseData = DefaultCoseService().decode(cose, verificationResult)
                val greenCertificate = coseData?.let { DefaultCborService().decode(it.cbor, verificationResult) }

                greenCertificate?.certificateType
            } catch (e: IllegalStateException) {
                Timber.e(e)
                null
            }
        }
    }
}