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
}

class SanitaryCertificate(override val value: String) : WalletCertificate(WalletCertificateType.SANITARY, value) {

    var firstName: String? = null
    var name: String? = null
    var birthDate: Date? = null
    var gender: String? = null
    var testResult: String? = null
    var analysisDate: Long? = null
    var analysisCode: String? = null

    override val timestamp: Long
        get() = analysisDate ?: System.currentTimeMillis()

    override fun parse() {
        val regex = type.validationRegexp
        val matchResult = regex.find(value)

        if (matchResult != null && matchResult.groups.size == 12) {
            keyAuthority = matchResult.groups[1]?.value ?: ""
            keyCertificateId = matchResult.groups[2]?.value ?: ""

            firstName = matchResult.groups[4]?.value?.replace("/", ", ") ?: ""
            name = matchResult.groups[5]?.value ?: ""

            val birthDateParser = SimpleDateFormat("ddMMyyyy", Locale.US)
            matchResult.groups[6]?.value?.let { birthDate = birthDateParser.parse(it) }

            gender = matchResult.groups[7]?.value ?: ""
            analysisCode = matchResult.groups[8]?.value ?: ""
            testResult = matchResult.groups[9]?.value ?: ""

            val analysisDateParser = SimpleDateFormat("ddMMyyyyHHmm", Locale.US)
            analysisDate = matchResult.groups[10]?.value?.let { analysisDateParser.parse(it)?.time }

            keySignature = matchResult.groups[11]?.value ?: ""
        } else {
            throw IllegalArgumentException("Could not parse Sanitary Certificate.")
        }
    }

    enum class SanitaryCertificateFields(val code: String) {
        FIRST_NAME("F0"),
        NAME("F1"),
        BIRTH_DATE("F2"),
        GENDER("F3"),
        ANALYSIS_CODE("F4"),
        TEST_RESULT("F5"),
        ANALYSIS_DATE("F6")
    }
}
