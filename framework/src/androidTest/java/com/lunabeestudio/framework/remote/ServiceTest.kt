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
import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.framework.remote.datasource.ServiceDataSource
import com.lunabeestudio.framework.testutils.ResourcesHelper
import com.lunabeestudio.robert.model.BackendException
import com.lunabeestudio.robert.model.ErrorCode
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.model.UnauthorizedException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class ServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var dataSource: ServiceDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        dataSource = ServiceDataSource(
            ApplicationProvider.getApplicationContext(),
            server.url("/api/v1.0/").toString(),
            server.url("/api/v1.0/").toString(),
            "",
            "",
            null,
        )
    }

    @Test
    fun captcha() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(ResourcesHelper.readTestFileAsString("captchaSuccess"))
        )
        val result = runBlocking {
            dataSource.generateCaptcha("", "", "")
        }
        assertThat(result).isInstanceOf(RobertResultData.Success::class.java)
        result as RobertResultData.Success
        assertThat(result.data).isEqualTo("228482eb770547059425e58ca6652c8a")

        testDataErrors {
            dataSource.generateCaptcha("", "", "")
        }
    }

    @Test
    fun registerV2Test() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(ResourcesHelper.readTestFileAsString("registerV2Success"))
        )
        val result = runBlocking {
            dataSource.registerV2("", "", "", "")
        }
        assertThat(result).isInstanceOf(RobertResultData.Success::class.java)
        result as RobertResultData.Success
        assertThat(result.data.message).isEqualTo("The application did register successfully")
        assertThat(result.data.timeStart).isEqualTo(3799958400L)
        assertThat(result.data.tuples).isEqualTo("test")

        testDataErrors {
            dataSource.registerV2("", "", "", "")
        }
    }

    @Test
    fun statusTest() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(ResourcesHelper.readTestFileAsString("statusSuccess"))
        )
        val result = runBlocking {
            dataSource.status("", ServerStatusUpdate("", 0L, "", ""))
        }
        assertThat(result).isInstanceOf(RobertResultData.Success::class.java)
        assertThat((result as RobertResultData.Success).data.riskLevel).isEqualTo(0)
        assertThat(result.data.ntpLastContactS).isEqualTo(3814601612L)
        assertThat(result.data.ntpLastRiskScoringS).isEqualTo(3814601613L)
        assertThat(result.data.message).isEqualTo("message")
        assertThat(result.data.tuples).isEqualTo("tuples")
        assertThat(result.data.declarationToken).isEqualTo("declarationToken")
        assertThat(result.data.analyticsToken).isEqualTo("analyticsToken")

        testDataErrors {
            dataSource.status("", ServerStatusUpdate("", 0L, "", ""))
        }
    }

    @Test
    fun wstatusTest() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(ResourcesHelper.readTestFileAsString("wstatusSuccess"))
        )
        val result = runBlocking {
            dataSource.wstatus("", emptyList())
        }
        assertThat(result).isInstanceOf(RobertResultData.Success::class.java)
        assertThat((result as RobertResultData.Success).data.riskLevel).isEqualTo(0)
        assertThat(result.data.ntpLastContactS).isEqualTo(3814601613L)

        testDataErrors {
            dataSource.wstatus("", emptyList())
        }
    }

    @Test
    fun reportTest() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(ResourcesHelper.readTestFileAsString("reportSuccess"))
        )
        var result = runBlocking {
            dataSource.report("", "", emptyList())
        }
        assertThat(result).isInstanceOf(RobertResultData.Success::class.java)
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(ResourcesHelper.readTestFileAsString("reportWithTokenSuccess"))
        )
        result = runBlocking {
            dataSource.report("", "", emptyList())
        }
        assertThat(result).isInstanceOf(RobertResultData.Success::class.java)
        assertThat((result as RobertResultData.Success).data.reportValidationToken).isEqualTo("token")


        testDataErrors {
            dataSource.report("", "", emptyList())
        }
    }

    @Test
    fun wreportTest() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(ResourcesHelper.readTestFileAsString("wreportSuccess"))
        )
        val result = runBlocking {
            dataSource.wreport("", "", emptyList())
        }
        assertThat(result).isInstanceOf(RobertResult.Success::class.java)

        testErrors {
            dataSource.wreport("", "", emptyList())
        }
    }

    @Test
    fun unregisterTest() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(ResourcesHelper.readTestFileAsString("unregisterSuccess"))
        )
        val result = runBlocking {
            dataSource.unregister("", ServerStatusUpdate("", 0L, "", ""))
        }
        assertThat(result).isInstanceOf(RobertResult.Success::class.java)

        testErrors {
            dataSource.unregister("", ServerStatusUpdate("", 0L, "", ""))
        }
    }

    @Test
    fun deleteExposureHistory() {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody(ResourcesHelper.readTestFileAsString("deleteExposureHistorySuccess"))
        )
        val result = runBlocking {
            dataSource.deleteExposureHistory("", ServerStatusUpdate("", 0L, "", ""))
        }
        assertThat(result).isInstanceOf(RobertResult.Success::class.java)

        testErrors {
            dataSource.deleteExposureHistory("", ServerStatusUpdate("", 0L, "", ""))
        }
    }

    private fun testDataErrors(wsCall: suspend () -> Any) {
        server.enqueue(MockResponse().setResponseCode(401))
        val unauthorizedResult = runBlocking {
            wsCall()
        }
        assertThat(unauthorizedResult).isInstanceOf(RobertResultData.Failure::class.java)
        assertThat((unauthorizedResult as RobertResultData.Failure<*>).error).isInstanceOf(UnauthorizedException::class.java)
        assertThat(unauthorizedResult.error?.errorCode).isEqualTo(ErrorCode.UNAUTHORIZED)

        server.enqueue(MockResponse().setResponseCode(500))
        val backendResult = runBlocking {
            wsCall()
        }
        assertThat(backendResult).isInstanceOf(RobertResultData.Failure::class.java)
        assertThat((backendResult as RobertResultData.Failure<*>).error).isInstanceOf(BackendException::class.java)
        assertThat(backendResult.error?.errorCode).isEqualTo(ErrorCode.BACKEND)
        assertThat(backendResult.error?.message).isEqualTo(BackendException().message)
    }

    private fun testErrors(wsCall: suspend () -> Any) {
        server.enqueue(MockResponse().setResponseCode(401))
        val unauthorizedResult = runBlocking {
            wsCall()
        }
        assertThat(unauthorizedResult).isInstanceOf(RobertResult.Failure::class.java)
        assertThat((unauthorizedResult as RobertResult.Failure).error).isInstanceOf(UnauthorizedException::class.java)
        assertThat(unauthorizedResult.error?.errorCode).isEqualTo(ErrorCode.UNAUTHORIZED)

        server.enqueue(MockResponse().setResponseCode(500))
        val backendResult = runBlocking {
            wsCall()
        }
        assertThat(backendResult).isInstanceOf(RobertResult.Failure::class.java)
        assertThat((backendResult as RobertResult.Failure).error).isInstanceOf(BackendException::class.java)
        assertThat(backendResult.error?.errorCode).isEqualTo(ErrorCode.BACKEND)
        assertThat(backendResult.error?.message).isEqualTo(BackendException().message)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
}
