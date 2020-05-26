/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/12/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.local.datasource

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lunabeestudio.domain.model.Hello
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.framework.utils.CryptoManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.random.Random

class SecureFileLocalProximityDataSourceTest {

    lateinit var secureFileLocalProximityDataSource: SecureFileLocalProximityDataSource
    lateinit var localStorage: File

    @Before
    fun createDataSource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        localStorage = File(context.filesDir, "local_proximity_test")
        localStorage.deleteRecursively()
        secureFileLocalProximityDataSource = SecureFileLocalProximityDataSource(localStorage, CryptoManager(context))
    }

    @After
    fun clearStorage() {
        File(ApplicationProvider.getApplicationContext<Context>().filesDir, "local_proximity_test").deleteRecursively()
    }

    @Test
    fun save_all_get_all() {
        val list = generateLocalProximity(10L)
        runBlocking {
            secureFileLocalProximityDataSource.saveAll(*list.toTypedArray())
            delay(250)
        }

        val getList = secureFileLocalProximityDataSource.getAll()

        assertThat(getList).isEqualTo(list)
    }

    @Test
    fun save_all_remove_all_after_dump() {
        val list = generateLocalProximity(10L)
        runBlocking {
            secureFileLocalProximityDataSource.saveAll(*list.toTypedArray())
            delay(250)
        }

        secureFileLocalProximityDataSource.removeAll()

        val removedList = secureFileLocalProximityDataSource.getAll()
        assertThat(removedList).isEmpty()
    }

    @Test
    fun save_all_remove_all_during_dump() {
        val list = generateLocalProximity(10L)
        runBlocking {
            secureFileLocalProximityDataSource.saveAll(*list.toTypedArray())
            secureFileLocalProximityDataSource.removeAll() // remove all before dump finished
            delay(250)
        }

        val getList = secureFileLocalProximityDataSource.getAll()

        assertThat(getList).isEmpty()
    }

    @Test
    fun remove_until() {
        (0 until 10).map { dirName ->
            val dir = File(localStorage, dirName.toString())
            dir.mkdirs()
            (0 until 10).map { fileName ->
                File(dir, "test-$fileName").writeText("test")
            }
        }

        assertThat(localStorage.walkBottomUp().filter { it.isFile }.toList().size).isEqualTo(10 * 10)

        secureFileLocalProximityDataSource.removeUntilTime(5 * 60 * 60 * 24)

        assertThat(localStorage.listFiles()).hasLength(5)
        assertThat(localStorage.walkBottomUp().filter { it.isFile }.toList().size).isEqualTo(5 * 10)
    }

    private fun generateLocalProximity(count: Long): List<LocalProximity> {
        return (0 until count).map {
            LocalProximity(Hello(Random.nextBytes(16)), it, 0, 0)
        }
    }
}