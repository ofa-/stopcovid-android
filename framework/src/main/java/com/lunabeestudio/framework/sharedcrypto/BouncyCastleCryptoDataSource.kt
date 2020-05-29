/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/20/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.sharedcrypto

import com.lunabeestudio.domain.extension.safeUse
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
import com.lunabeestudio.robert.extension.use
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BouncyCastleCryptoDataSource : SharedCryptoDataSource {
    override fun createECDHKeyPair(): KeyPair {
        val ecSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec(NAMED_CURVE_SPEC)
        val bouncyCastleProvider = BouncyCastleProvider()
        val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_ECDH, bouncyCastleProvider)
        keyPairGenerator.initialize(ecSpec, SecureRandom())
        return keyPairGenerator.generateKeyPair()
    }

    override fun getEncryptionKeys(rawServerPublicKey: ByteArray,
        rawLocalPrivateKey: ByteArray,
        kADerivation: ByteArray,
        kEADerivation: ByteArray): Pair<ByteArray, ByteArray> {
        val bouncyCastleProvider = BouncyCastleProvider()
        val keyFactory = KeyFactory.getInstance(ALGORITHM_ECDH, bouncyCastleProvider)
        val serverPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(rawServerPublicKey))
        val localPrivateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(rawLocalPrivateKey))

        val keyAgreement = KeyAgreement.getInstance(ALGORITHM_ECDH)

        keyAgreement.init(localPrivateKey)
        keyAgreement.doPhase(serverPublicKey, true)

        return keyAgreement.generateSecret().use { sharedSecret ->
            SecretKeySpec(sharedSecret, HASH_HMACSHA256).safeUse<Pair<ByteArray, ByteArray>> { secretKeySpec ->
                val hmac = Mac.getInstance(HASH_HMACSHA256)
                hmac.init(secretKeySpec)

                val kA = hmac.doFinal(kADerivation)
                val kEA = hmac.doFinal(kEADerivation)

                Pair(kA, kEA)
            }
        }
    }

    override fun decrypt(key: ByteArray, encryptedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_CIPHER_TYPE)
        val iv: ByteArray = encryptedData.copyOfRange(0, AES_GCM_IV_LENGTH)
        val ivSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH_IN_BITS, iv)

        return SecretKeySpec(key, ALGORITHM_AES).safeUse { secretKey ->
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            cipher.doFinal(encryptedData, AES_GCM_IV_LENGTH, encryptedData.size - AES_GCM_IV_LENGTH)
        }
    }

    companion object {
        private const val HASH_HMACSHA256 = "HmacSHA256"
        private const val NAMED_CURVE_SPEC = "secp256r1"
        private const val ALGORITHM_ECDH = "ECDH"
        private const val ALGORITHM_AES = "AES"

        private const val AES_GCM_CIPHER_TYPE = "AES/GCM/NoPadding"
        private const val AES_GCM_IV_LENGTH = 12
        private const val AES_GCM_TAG_LENGTH_IN_BITS = 128
    }
}