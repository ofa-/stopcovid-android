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

import android.util.Base64
import android.util.Log
import com.google.common.truth.Truth.assertThat
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

class BouncyCastleCryptoDataSourceTest() {
    private lateinit var bcCryptoDataSource: BouncyCastleCryptoDataSource
    private lateinit var serverPublicKey: PublicKey
    private lateinit var localPrivateKey: PrivateKey

    @Before
    fun createDataSource() {
        bcCryptoDataSource = BouncyCastleCryptoDataSource()

        val bouncyCastleProvider = BouncyCastleProvider()
        val keyFactory = KeyFactory.getInstance("ECDH", bouncyCastleProvider)
        val serverKeySpec = X509EncodedKeySpec(Base64.decode(MOCK_SERVER_PUB_KEY, Base64.NO_WRAP))
        serverPublicKey = keyFactory.generatePublic(serverKeySpec)
        val localKeySpec = PKCS8EncodedKeySpec(Base64.decode(MOCK_LOCAL_KEY, Base64.NO_WRAP))
        localPrivateKey = keyFactory.generatePrivate(localKeySpec)
    }

    @Test
    fun create_ECDH_keyPair() {
        // Test no throws
        val keyPair = bcCryptoDataSource.createECDHKeyPair()
        assertThat(keyPair).isNotNull()
    }

    @Test
    fun get_encryption_keys() {
        val keys = bcCryptoDataSource.getEncryptionKeys(serverPublicKey.encoded,
            localPrivateKey.encoded,
            "test".toByteArray(),
            "test2".toByteArray())

        val kA = Base64.encodeToString(keys.first, Base64.NO_WRAP)
        val kEA = Base64.encodeToString(keys.second, Base64.NO_WRAP)

        assertThat(kA).isEqualTo("mwuOwJO0qxPG7JuZibow6RzByIwDcvzEEx3jbW84t8k=")
        assertThat(kEA).isEqualTo("Xl0TXEspgkuTBniuUEcNFPqQvoHM006/tpyyE4NRFyY=")
    }

    companion object {
        private const val MOCK_SERVER_PUB_KEY: String = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEIk7OAGcqyGpnmeTQiEDU0Uih9h3wMhwGmv6lqYuupR6I9aqLTBGSQvi6YIA+r7ZvxilaRBxzdxIuMXlTUTDxhw=="
        private const val MOCK_LOCAL_KEY: String = "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg8Ss533Vz+z0GG/l2sxYBtA2vD0NR1WW3tgNRJ/uq67uhRANCAATkgJaihoP8jim8eAOfswWt9LcKE0iKKqc0ItWDmJrI6LxU+oa4qgI/CDEbRBQIAAYwvCCPLLNH8TJBCjf9kBfX"
    }
}
