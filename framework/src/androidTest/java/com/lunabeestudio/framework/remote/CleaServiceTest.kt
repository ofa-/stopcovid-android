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
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.framework.remote.datasource.CleaDataSource
import com.lunabeestudio.framework.testutils.ResourcesHelper
import com.lunabeestudio.robert.model.RobertResultData
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class CleaServiceTest {

    private lateinit var dataSource: CleaDataSource
    private lateinit var server: MockWebServer

    @MockK(relaxed = true)
    private lateinit var analyticsManager: AnalyticsManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        server = MockWebServer()
        server.start()
        dataSource = CleaDataSource(
            ApplicationProvider.getApplicationContext(),
            server.url("/").toString(),
            server.url("/").toString(),
            analyticsManager,
        )
    }

    @Test
    fun getClusterIndexTestOK() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(ResourcesHelper.readTestFileAsString("clusterIndexSuccess"))
        )
        val result = runBlocking {
            dataSource.cleaClusterIndex("v1", null)
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
            dataSource.cleaClusterIndex("v1", null)
        }
        assertThat(result).isInstanceOf(RobertResultData.Failure::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
}