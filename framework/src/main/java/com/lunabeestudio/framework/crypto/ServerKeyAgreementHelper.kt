/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/22/9 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.crypto

import android.util.Base64
import com.lunabeestudio.robert.RobertConstant
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
import com.lunabeestudio.robert.extension.use

internal class ServerKeyAgreementHelper(
    private val sharedCryptoDataSource: SharedCryptoDataSource,
) {

    private var localPrivateKey: ByteArray? = null

    fun encryptRequestData(
        serverPublicKey: String,
        encodedDataList: List<String>,
    ): ServerKeyAgreementData {
        val rawServerKey = Base64.decode(serverPublicKey, Base64.NO_WRAP)

        val keyPair = sharedCryptoDataSource.createECDHKeyPair()

        val encodedLocalPublicKey = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        val serverKeyAgreementData = sharedCryptoDataSource.getEncryptionKeys(
            rawServerPublicKey = rawServerKey,
            rawLocalPrivateKey = keyPair.private.encoded,
            derivationDataArray = listOf(
                RobertConstant.CONVERSION_STRING_INPUT.toByteArray(Charsets.UTF_8),
            )
        ).first().let {
            localPrivateKey = it
            val encryptedDataList = encodedDataList.map { encodedData ->
                sharedCryptoDataSource.encrypt(it, encodedData.toByteArray(Charsets.UTF_8))
            }
            val encodedEncryptedData = encryptedDataList.map { encryptedData ->
                Base64.encodeToString(encryptedData, Base64.NO_WRAP)
            }
            ServerKeyAgreementData(encodedLocalPublicKey, encodedEncryptedData)
        }

        return serverKeyAgreementData
    }

    @Throws(IllegalStateException::class)
    fun decryptResponse(encryptedData: String): String {
        val data = Base64.decode(encryptedData, Base64.NO_WRAP)
        return try {
            (localPrivateKey ?: throw IllegalStateException("localPrivateKey is null. getKeyAgreementData must be call first")).use {
                sharedCryptoDataSource.decrypt(it, data).toString(Charsets.UTF_8)
            }
        } finally {
            localPrivateKey = null
        }
    }

    class ServerKeyAgreementData(
        val encodedLocalPublicKey: String,
        val encryptedData: List<String>,
    )
}