/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/9/7 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.testutils

import android.os.Build
import android.util.Base64
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import java.io.ByteArrayOutputStream
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val HASH_HMACSHA256 = "HmacSHA256"
    private const val NAMED_CURVE_SPEC = "secp256r1"
    private const val ALGORITHM_ECDH = "ECDH"
    private const val ALGORITHM_AES = "AES"

    private const val AES_GCM_CIPHER_TYPE = "AES/GCM/NoPadding"
    private const val AES_GCM_IV_LENGTH = 12
    private const val AES_GCM_TAG_LENGTH_IN_BITS = 128

    private const val CONVERSION_STRING_INPUT: String = "conversion"

    internal fun generateKey(privateKey: PrivateKey, encodedPublicKey: String): ByteArray {
        val bouncyCastleProvider = BouncyCastleProvider()
        val keyFactory = KeyFactory.getInstance(ALGORITHM_ECDH, bouncyCastleProvider)
        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(Base64.decode(encodedPublicKey, Base64.NO_WRAP)))
        val keyAgreement = KeyAgreement.getInstance(ALGORITHM_ECDH)

        keyAgreement.init(privateKey)
        keyAgreement.doPhase(publicKey, true)

        val sharedSecret = keyAgreement.generateSecret()
        val secretKeySpec = SecretKeySpec(sharedSecret, HASH_HMACSHA256)
        val hmac = Mac.getInstance(HASH_HMACSHA256)
        hmac.init(secretKeySpec)
        return hmac.doFinal(CONVERSION_STRING_INPUT.toByteArray(Charsets.UTF_8))
    }

    internal fun decodeDecrypt(key: ByteArray, encodedEncryptedInput: String): String {
        val rawEncryptedCertificate = Base64.decode(encodedEncryptedInput, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(AES_GCM_CIPHER_TYPE)
        val iv: ByteArray = rawEncryptedCertificate.copyOfRange(0, AES_GCM_IV_LENGTH)
        val ivSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH_IN_BITS, iv)
        val secretKey = SecretKeySpec(key, ALGORITHM_AES)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val rawDecryptedCertificate = cipher.doFinal(
            rawEncryptedCertificate,
            AES_GCM_IV_LENGTH,
            rawEncryptedCertificate.size - AES_GCM_IV_LENGTH
        )

        return rawDecryptedCertificate.toString(Charsets.UTF_8)
    }

    internal fun encryptEncode(key: ByteArray, clearInput: String): String {
        val secretKey = SecretKeySpec(key, HASH_HMACSHA256)
        val bos = ByteArrayOutputStream()

        val cipher = Cipher.getInstance(AES_GCM_CIPHER_TYPE)
        val iv: ByteArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            cipher.iv
        } else {
            val iv = ByteArray(AES_GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            iv
        }

        bos.write(iv)

        CipherOutputStream(bos, cipher).use { cos ->
            clearInput
                .byteInputStream(Charsets.UTF_8)
                .use { input ->
                    input.copyTo(cos)
                }
        }

        val encryptedData = bos.toByteArray()

        return Base64.encodeToString(encryptedData, Base64.NO_WRAP)
    }

    internal fun generateEcdhKeyPair(): KeyPair {
        val ecSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec(NAMED_CURVE_SPEC)
        val bouncyCastleProvider = BouncyCastleProvider()
        val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_ECDH, bouncyCastleProvider)
        keyPairGenerator.initialize(ecSpec, SecureRandom())
        return keyPairGenerator.genKeyPair()
    }
}