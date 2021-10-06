/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.local.datasource

import android.content.Context
import androidx.core.content.edit
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

        val keystore = KeyStore.getInstance(LocalCryptoManager.ANDROID_KEY_STORE_PROVIDER)
        keystore.load(null)
        keystore.deleteEntry("aes_local_protection")
        keystore.deleteEntry("rsa_wrap_local_protection")
        context.getSharedPreferences(SecureKeystoreDataSource.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit {
            remove("shared.pref.shared_key")
            remove("shared.pref.time_start")
            remove("shared.pref.db_key")
        }
        context.getSharedPreferences(LocalCryptoManager.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit {
            remove("aes_wrapped_local_protection")
            remove("secret_key_generated")
        }

        keystoreDataSource = SecureKeystoreDataSource(context, LocalCryptoManager(context), hashMapOf())
    }

    @After
    fun clear() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val keystore = KeyStore.getInstance(LocalCryptoManager.ANDROID_KEY_STORE_PROVIDER)
        keystore.load(null)
        keystore.deleteEntry("aes_local_protection")
        keystore.deleteEntry("rsa_wrap_local_protection")
        context.getSharedPreferences(SecureKeystoreDataSource.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit {
            remove("shared.pref.shared_key")
            remove("shared.pref.time_start")
            remove("shared.pref.db_key")
        }
        context.getSharedPreferences(LocalCryptoManager.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit {
            remove("aes_wrapped_local_protection")
            remove("secret_key_generated")
        }
    }

    @Test
    fun saveKA_and_getKA() {
        val key = Random.nextBytes(16)
        keystoreDataSource.kA = key.copyOf()

        val storedString = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(SecureKeystoreDataSource.SHARED_PREF_NAME, Context.MODE_PRIVATE)
            .getString("shared.pref.ka", null)

        assertThat(storedString).isNotNull()
        assertThat(storedString).isNotEqualTo(key)

        val decryptedKey = keystoreDataSource.kA

        assertThat(decryptedKey).isNotNull()
        assert(key.contentEquals(decryptedKey!!))
    }

    @Test
    fun saveLongString_and_getLongString() {
        val key = Random.nextBytes(8732)
        keystoreDataSource.kA = key.copyOf()

        val storedString = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(SecureKeystoreDataSource.SHARED_PREF_NAME, Context.MODE_PRIVATE)
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
            .getSharedPreferences(SecureKeystoreDataSource.SHARED_PREF_NAME, Context.MODE_PRIVATE)
            .getString("shared.pref.ka", null)

        assertThat(storedString).isNotNull()

        keystoreDataSource.kA = null

        storedString = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(SecureKeystoreDataSource.SHARED_PREF_NAME, Context.MODE_PRIVATE)
            .getString("shared.pref.ka", null)

        assertThat(storedString).isNull()
    }
}