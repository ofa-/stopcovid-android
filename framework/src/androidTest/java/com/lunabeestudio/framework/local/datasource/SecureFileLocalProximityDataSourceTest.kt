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
import com.lunabeestudio.framework.extension.toProto
import com.lunabeestudio.framework.local.LocalCryptoManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.random.Random

class SecureFileLocalProximityDataSourceTest {

    private lateinit var secureFileLocalProximityDataSource: SecureFileLocalProximityDataSource
    private lateinit var localCryptoManager: LocalCryptoManager
    private lateinit var localStorage: File

    @Before
    fun createDataSource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        localStorage = File(context.filesDir, "local_proximity_test")
        localStorage.deleteRecursively()
        localCryptoManager = LocalCryptoManager(context)
        secureFileLocalProximityDataSource = SecureFileLocalProximityDataSource(localStorage,
            localCryptoManager)
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

        val getList = secureFileLocalProximityDataSource.getUntilTime(0)

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

        val removedList = secureFileLocalProximityDataSource.getUntilTime(0)
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

        val getList = secureFileLocalProximityDataSource.getUntilTime(0)

        assertThat(getList).isEmpty()
    }

    @Test
    fun get_until() {
        val proximityNumber = 10L
        val dayNumber = 10
        val sessionNumber = 10
        val proto = generateLocalProximity(proximityNumber).toProto()
        (0 until dayNumber).map { dirName ->
            val dir = File(localStorage, dirName.toString())
            dir.mkdirs()
            (0 until sessionNumber).map { fileName ->
                localCryptoManager.createCipherOutputStream(File(dir, "$dirName-$fileName").outputStream()).use {
                    proto.writeTo(it)
                }
            }
        }

        assertThat(secureFileLocalProximityDataSource.getUntilTime(5 * 60 * 60 * 24)).hasSize(5 * dayNumber * sessionNumber)
        assertThat(secureFileLocalProximityDataSource.getUntilTime(9 * 60 * 60 * 24)).hasSize(dayNumber * sessionNumber)
        assertThat(secureFileLocalProximityDataSource.getUntilTime(10 * 60 * 60 * 24)).hasSize(0)
        assertThat(secureFileLocalProximityDataSource.getUntilTime(0L)).hasSize((proximityNumber * dayNumber * sessionNumber).toInt())
    }

    @Test
    fun remove_until() {
        (0 until 10).map { dirName ->
            val dir = File(localStorage, dirName.toString())
            dir.mkdirs()
            (0 until 10).map { fileName ->
                File(dir, "$dirName-$fileName").writeText("test")
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