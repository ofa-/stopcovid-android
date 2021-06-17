package com.lunabeestudio.stopcovid.model

import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.framework.crypto.BouncyCastleSignatureVerifier
import java.security.SignatureException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class WalletCertificate(
    val type: WalletCertificateType,
    open val value: String,
) {

    var keyCertificateId: String = ""
    var keyAuthority: String = ""
    var keySignature: String = ""

    var firstName: String? = null
    var name: String? = null

    abstract val timestamp: Long

    abstract fun parse()

    enum class Separator(
        val ascii: String,
    ) {
        UNIT("\u001F"),
    }

    @Throws(IllegalArgumentException::class, WalletCertificateInvalidSignatureException::class)
    fun verifyKey(publicKey: String) {
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

    protected fun parseBirthDate(dateString: String): String {
        return "${dateString.substring(0, 2)}-${dateString.substring(2, 4)}-${dateString.substring(4)}"
    }
}

class SanitaryCertificate(override val value: String) : WalletCertificate(WalletCertificateType.SANITARY, value) {

    var birthDate: String? = null
    var gender: String? = null
    var testResult: String? = null
    var analysisDate: Long? = null
    var analysisCode: String? = null

    override val timestamp: Long
        get() = analysisDate ?: System.currentTimeMillis()

    override fun parse() {
        val regex = type.validationRegexp
        val matchResult = regex.find(value)

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
}

class VaccinationCertificate(override val value: String) : WalletCertificate(WalletCertificateType.VACCINATION, value) {

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
        val regex = type.validationRegexp
        val matchResult = regex.find(value)

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
}
