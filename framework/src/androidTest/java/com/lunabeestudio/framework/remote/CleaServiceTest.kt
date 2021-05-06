/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/20/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lunabeestudio.framework.remote.datasource.CleaDataSource
import com.lunabeestudio.framework.testutils.ResourcesHelper
import com.lunabeestudio.robert.model.RobertResultData
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class CleaServiceTest {

    private lateinit var dataSource: CleaDataSource
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        dataSource = CleaDataSource(
            ApplicationProvider.getApplicationContext(),
            server.url("/").toString(),
            "sha256/tb6+ch/VeDZl6rHlWfL4fCAQHyCmkexJhYBa7drUmxY=",
            server.url("/").toString(),
            "sha256/tb6+ch/VeDZl6rHlWfL4fCAQHyCmkexJhYBa7drUmxY="
        )
    }

    @Test
    fun getClusterIndexTestOK() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(ResourcesHelper.readTestFileAsString("clusterIndexSuccess"))
        )
        val result = runBlocking {
            dataSource.cleaClusterIndex("v1")
        }
        assertThat(result).isInstanceOf(RobertResultData.Success::class.java)
    }

    @Test
    fun getClusterIndexTestKO() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(ResourcesHelper.readTestFileAsString("clusterIndexFailure"))
        )
        val result = runBlocking {
            dataSource.cleaClusterIndex("v1")
        }
        assertThat(result).isInstanceOf(RobertResultData.Failure::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
}