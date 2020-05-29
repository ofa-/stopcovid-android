/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.local.datasource

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lunabeestudio.framework.local.LocalCryptoManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.security.KeyStore
import kotlin.random.Random

class KeystoreDataSourceTest {
    private lateinit var keystoreDataSource: SecureKeystoreDataSource

    @Before
    fun createDataSource() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val keystore = KeyStore.getInstance("AndroidKeyStore")
        keystore.load(null)
        keystore.deleteEntry("aes_local_protection")
        keystore.deleteEntry("rsa_wrap_local_protection")
        context.getSharedPreferences("robert_prefs", Context.MODE_PRIVATE).edit().remove("shared.pref.shared_key").commit()
        context.getSharedPreferences("robert_prefs", Context.MODE_PRIVATE).edit().remove("shared.pref.time_start").commit()
        context.getSharedPreferences("robert_prefs", Context.MODE_PRIVATE).edit().remove("shared.pref.db_key").commit()
        context.getSharedPreferences("crypto_prefs", Context.MODE_PRIVATE).edit().remove("aes_wrapped_local_protection").commit()

        keystoreDataSource = SecureKeystoreDataSource(context, LocalCryptoManager(context))
    }

    @After
    fun clear() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val keystore = KeyStore.getInstance("AndroidKeyStore")
        keystore.load(null)
        keystore.deleteEntry("aes_local_protection")
        keystore.deleteEntry("rsa_wrap_local_protection")
        context.getSharedPreferences("robert_prefs", Context.MODE_PRIVATE).edit().remove("shared.pref.shared_key").commit()
        context.getSharedPreferences("robert_prefs", Context.MODE_PRIVATE).edit().remove("shared.pref.time_start").commit()
        context.getSharedPreferences("robert_prefs", Context.MODE_PRIVATE).edit().remove("shared.pref.db_key").commit()
        context.getSharedPreferences("crypto_prefs", Context.MODE_PRIVATE).edit().remove("aes_wrapped_local_protection").commit()
    }

    @Test
    fun saveKA_and_getKA() {
        val key = Random.nextBytes(16)
        keystoreDataSource.kA = key

        val storedString = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("robert_prefs", Context.MODE_PRIVATE)
            .getString("shared.pref.ka", null)

        assertThat(storedString).isNotNull()
        assertThat(storedString).isNotEqualTo(key)

        val decryptedKey = keystoreDataSource.kA

        assertThat(decryptedKey).isNotNull()
        assert(key.contentEquals(decryptedKey!!))
    }

    @Test
    fun saveKA_and_removeKA() {
        val key = Random.nextBytes(16)
        keystoreDataSource.kA = key

        var storedString = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("robert_prefs", Context.MODE_PRIVATE)
            .getString("shared.pref.ka", null)

        assertThat(storedString).isNotNull()

        keystoreDataSource.kA = null

        storedString = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("robert_prefs", Context.MODE_PRIVATE)
            .getString("shared.pref.ka", null)

        assertThat(storedString).isNull()
    }
}