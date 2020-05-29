/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/06/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.local.datasource

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import com.lunabeestudio.framework.local.LocalCryptoManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class SecureFileEphemeralBluetoothIdentifierDataSourceTest {

    lateinit var secureFileEphemeralBluetoothIdentifierDataSource: SecureFileEphemeralBluetoothIdentifierDataSource

    @Before
    fun createDataSource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        secureFileEphemeralBluetoothIdentifierDataSource = SecureFileEphemeralBluetoothIdentifierDataSource(context,
            LocalCryptoManager(context))
        File(context.filesDir, "epochs").delete()
    }

    @After
    fun closeDb() {
        File(ApplicationProvider.getApplicationContext<Context>().filesDir, "epochs").delete()
    }

    @Test
    fun write_and_read() {
        secureFileEphemeralBluetoothIdentifierDataSource.saveAll(
            getEphemeralBluetoothIdentifier(0L, 500L),
            getEphemeralBluetoothIdentifier(500L, 1000L)
        )

        val getAll = secureFileEphemeralBluetoothIdentifierDataSource.getAll()
        val get250 = secureFileEphemeralBluetoothIdentifierDataSource.getForTime(250L)
        val get1000 = secureFileEphemeralBluetoothIdentifierDataSource.getForTime(1000L)

        assertThat(getAll).hasSize(2)
        assertThat(get250).isNotNull()
        assertThat(get1000).isNull()
    }

    @Test
    fun remove_until_time() {
        secureFileEphemeralBluetoothIdentifierDataSource.saveAll(
            getEphemeralBluetoothIdentifier(0L, 500L),
            getEphemeralBluetoothIdentifier(1000L, 1500L),
            getEphemeralBluetoothIdentifier(500L, 1000L)
        )

        secureFileEphemeralBluetoothIdentifierDataSource.removeUntilTimeKeepLast(750L)
        var getAll = secureFileEphemeralBluetoothIdentifierDataSource.getAll()

        assertThat(getAll).hasSize(2)

        secureFileEphemeralBluetoothIdentifierDataSource.removeUntilTimeKeepLast(2000L)
        getAll = secureFileEphemeralBluetoothIdentifierDataSource.getAll()

        assertThat(getAll).hasSize(1)
        assertThat(getAll.last().ntpEndTimeS).isEqualTo(1500L)
    }

    @Test
    fun remove_all() {
        secureFileEphemeralBluetoothIdentifierDataSource.saveAll(
            getEphemeralBluetoothIdentifier(0L, 500L),
            getEphemeralBluetoothIdentifier(500L, 1000L)
        )

        secureFileEphemeralBluetoothIdentifierDataSource.removeAll()
        val getAll = secureFileEphemeralBluetoothIdentifierDataSource.getAll()

        assertThat(getAll).isEmpty()
    }

    private fun getEphemeralBluetoothIdentifier(start: Long, end: Long): EphemeralBluetoothIdentifier {
        return EphemeralBluetoothIdentifier(
            epochId = 0,
            ntpStartTimeS = start,
            ntpEndTimeS = end,
            ecc = byteArrayOf(1),
            ebid = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        )
    }
}