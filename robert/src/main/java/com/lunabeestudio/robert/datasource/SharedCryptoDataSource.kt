/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/20/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.datasource

import java.security.KeyPair

interface SharedCryptoDataSource {
    fun createECDHKeyPair(): KeyPair
    fun getEncryptionKeys(
        rawServerPublicKey: ByteArray,
        rawLocalPrivateKey: ByteArray,
        derivationDataArray: List<ByteArray>
    ): List<ByteArray>

    fun decrypt(key: ByteArray, encryptedData: ByteArray): ByteArray
    fun encrypt(key: ByteArray, clearData: ByteArray): ByteArray
}