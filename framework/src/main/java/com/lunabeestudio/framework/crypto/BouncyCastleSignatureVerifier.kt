package com.lunabeestudio.framework.crypto

import android.util.Base64
import com.lunabeestudio.framework.extension.removePublicKeyDecoration
import org.apache.commons.codec.binary.Base32
import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.X509EncodedKeySpec

object BouncyCastleSignatureVerifier {
    private const val DEFAULT_PUB_KEY_ALGORITHM: String = "ECDSA"
    private const val DEFAULT_SIGNATURE_ALGORITHM: String = "SHA256withECDSA"

    /**
     * Verify the message against the provided signature
     *
     * @param rawPublicKey The raw public key encoded in base64
     * @param message The message to verify
     * @param rawSignature The raw signature used to verify the message (R & S components concatenated) encoded in base32
     * @param publicKeyAlgorithm The algorithm used to generate the public key
     * @param signatureKeyAlgorithm The algorithm used to generate the signature
     */
    @Throws(SignatureException::class)
    fun verifySignature(
        rawPublicKey: String,
        message: String,
        rawSignature: String,
        publicKeyAlgorithm: String = DEFAULT_PUB_KEY_ALGORITHM,
        signatureKeyAlgorithm: String = DEFAULT_SIGNATURE_ALGORITHM,
    ): Boolean {
        val bouncyCastleProvider = BouncyCastleProvider()

        val publicKeySpec = X509EncodedKeySpec(Base64.decode(rawPublicKey.removePublicKeyDecoration(), Base64.NO_WRAP))
        val keyFactory = KeyFactory.getInstance(publicKeyAlgorithm, bouncyCastleProvider)
        val publicKey: PublicKey = keyFactory.generatePublic(publicKeySpec)
        val ecdsaVerify: Signature = Signature.getInstance(signatureKeyAlgorithm)

        val rawMessage = message.toByteArray(StandardCharsets.US_ASCII)
        val decodedSignature = Base32().decode(rawSignature)

        val derSequence = getDERSequenceFromSignature(decodedSignature)
        ecdsaVerify.initVerify(publicKey)
        ecdsaVerify.update(rawMessage)

        return ecdsaVerify.verify(derSequence.encoded)
    }

    private fun getDERSequenceFromSignature(decodedSignature: ByteArray): DERSequence {
        val length = decodedSignature.size / 2
        val bytes = ByteArray(length)
        val asn1EncodableVector = ASN1EncodableVector()

        System.arraycopy(decodedSignature, 0, bytes, 0, length)
        asn1EncodableVector.add(ASN1Integer(BigInteger(1, bytes)))

        System.arraycopy(decodedSignature, length, bytes, 0, length)
        asn1EncodableVector.add(ASN1Integer(BigInteger(1, bytes)))

        return DERSequence(asn1EncodableVector)
    }
}