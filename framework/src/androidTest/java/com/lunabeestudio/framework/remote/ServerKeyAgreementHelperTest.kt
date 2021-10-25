/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/8/7 - for the TOUS-ANTI-COVID project
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/8/7 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote

import android.util.Base64
import com.lunabeestudio.framework.crypto.BouncyCastleCryptoDataSource
import com.lunabeestudio.framework.crypto.ServerKeyAgreementHelper
import com.lunabeestudio.robert.RobertConstant
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.security.KeyPair
import javax.crypto.AEADBadTagException

class ServerKeyAgreementHelperTest {
    private val sharedCryptoDataSource: SharedCryptoDataSource = BouncyCastleCryptoDataSource()
    private val serverKeyAgreementHelper = ServerKeyAgreementHelper(sharedCryptoDataSource)
    private lateinit var serverKeyPair: KeyPair

    @Before
    fun init() {
        serverKeyPair = sharedCryptoDataSource.createECDHKeyPair()
    }

    @Test
    fun encrypt_decrypt_success() {
        val inputString = "Is NSA watching?"

        val keyAgreementData = serverKeyAgreementHelper.encryptRequestData(
            Base64.encodeToString(serverKeyPair.public.encoded, Base64.NO_WRAP),
            "",
        )

        // server side encryption
        val encryptionKey = sharedCryptoDataSource.getEncryptionKeys(
            Base64.decode(keyAgreementData.encodedLocalPublicKey, Base64.NO_WRAP),
            serverKeyPair.private.encoded,
            listOf(RobertConstant.CONVERSION_STRING_INPUT.toByteArray(Charsets.UTF_8)),
        ).first()
        val data = sharedCryptoDataSource.encrypt(encryptionKey, inputString.toByteArray(Charsets.UTF_8))

        val response = serverKeyAgreementHelper.decryptResponse(Base64.encodeToString(data, Base64.NO_WRAP))

        assert(inputString == response)
    }

    @Test
    fun decrypt_without_encryptRequestData() {
        val localKeyPair = sharedCryptoDataSource.createECDHKeyPair()

        val encryptionKey = sharedCryptoDataSource.getEncryptionKeys(
            serverKeyPair.public.encoded,
            localKeyPair.private.encoded,
            listOf(RobertConstant.CONVERSION_STRING_INPUT.toByteArray(Charsets.UTF_8)),
        ).first()
        val data = sharedCryptoDataSource.encrypt(encryptionKey, ByteArray(10))

        assertThrows(IllegalStateException::class.java) {
            serverKeyAgreementHelper.decryptResponse(Base64.encodeToString(data, Base64.NO_WRAP))
        }
    }

    @Test
    fun encrypt_decrypt_bad_key() {
        val inputString = "Is NSA watching?"

        val badKeyPair = sharedCryptoDataSource.createECDHKeyPair()

        val keyAgreementData = serverKeyAgreementHelper.encryptRequestData(
            Base64.encodeToString(badKeyPair.public.encoded, Base64.NO_WRAP),
            "",
        )

        // server side encryption
        val encryptionKey = sharedCryptoDataSource.getEncryptionKeys(
            Base64.decode(keyAgreementData.encodedLocalPublicKey, Base64.NO_WRAP),
            serverKeyPair.private.encoded,
            listOf(RobertConstant.CONVERSION_STRING_INPUT.toByteArray(Charsets.UTF_8)),
        ).first()
        val data = sharedCryptoDataSource.encrypt(encryptionKey, inputString.toByteArray(Charsets.UTF_8))

        assertThrows(AEADBadTagException::class.java) {
            serverKeyAgreementHelper.decryptResponse(Base64.encodeToString(data, Base64.NO_WRAP))
        }
    }
}