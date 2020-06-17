/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/09/06 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class LocalCryptoManagerTest {

    private lateinit var localCryptoManager: LocalCryptoManager

    @Before
    fun init() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        localCryptoManager = LocalCryptoManager(context)
    }

    @Test
    fun encrypt_decrypt_shortByteArray() {
        val passphrase = Random.nextBytes(Random.nextInt(0, 4096))

        val encrypted = localCryptoManager.encrypt(passphrase.copyOf())
        val decrypted = localCryptoManager.decrypt(encrypted)

        assertThat(encrypted).isNotEqualTo(passphrase)
        assertThat(decrypted).isEqualTo(passphrase)
    }

    @Test
    fun encrypt_decrypt_longByteArray() {
        val passphrase = Random.nextBytes(Random.nextInt(4096, 16384))

        val encrypted = localCryptoManager.encrypt(passphrase.copyOf())
        val decrypted = localCryptoManager.decrypt(encrypted)

        assertThat(encrypted).isNotEqualTo(passphrase)
        assertThat(decrypted).isEqualTo(passphrase)
    }
}