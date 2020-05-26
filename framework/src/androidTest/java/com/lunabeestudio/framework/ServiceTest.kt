/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.framework.remote.datasource.ServiceDataSource
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
import java.nio.ByteBuffer

class ServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var dataSource: ServiceDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        dataSource = ServiceDataSource(ApplicationProvider.getApplicationContext(), server.url("/api/v1.0/").toString())
    }

    @Test
    fun registerTest() {
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(readTestFile("registerSuccess")))
        val result = runBlocking {
            dataSource.register("")
        }
        assertThat(result).isInstanceOf(RobertResultData.Success::class.java)
        assertThat((result as RobertResultData.Success).data.key).isEqualTo("Y2VjaWVzdHVuZWNsZWRldGVzdA==")
        assertThat(result.data.message).isEqualTo("The application did register successfully")
        assertThat(result.data.ephemeralBluetoothIdentifierList.size).isEqualTo(4)
        assertThat(result.data.ephemeralBluetoothIdentifierList[0].ecc).isEqualTo("102".toByteArray())
        assertThat(result.data.ephemeralBluetoothIdentifierList[0].ebid).isEqualTo("97.98.99.100.101.102.103.104".toByteArray())
        assertThat(result.data.timeStart).isEqualTo(3796669679L)
        assertThat(result.data.filterings?.size).isEqualTo(1)
        assertThat(result.data.filterings?.get(0)?.name).isEqualTo("distance")
        assertThat(result.data.filterings?.get(0)?.value).isEqualTo(12.0)

        testDataErrors {
            dataSource.register("")
        }
    }

    @Test
    fun statusTest() {
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(readTestFile("statusSuccess")))
        val result = runBlocking {
            dataSource.status(ServerStatusUpdate("", "", ""), 0)
        }
        assertThat(result).isInstanceOf(RobertResultData.Success::class.java)
        assertThat((result as RobertResultData.Success).data.atRisk)
        assertThat(result.data.lastExposureTimeframe).isEqualTo(0)
        assertThat(result.data.message)
            .isEqualTo("Votre test COVID-19 est positif. Merci de respecter la période de quatorzaine. Prenez soin de vous et de vos proches.")
        assertThat(result.data.ephemeralBluetoothIdentifierList.size).isEqualTo(1)
        assertThat(result.data.ephemeralBluetoothIdentifierList[0].ecc).isEqualTo("73.-87.109.121.-41.-76".toByteArray())
        assertThat(result.data.ephemeralBluetoothIdentifierList[0].ebid).isEqualTo("73.-87.109.121.-47.-96.12.29.90.91.83".toByteArray())
        assertThat(result.data.filterings.size).isEqualTo(1)
        assertThat(result.data.filterings[0].name).isEqualTo("distance")
        assertThat(result.data.filterings[0].value).isEqualTo(12.0)

        testDataErrors {
            dataSource.status(ServerStatusUpdate("", "", ""), 0)
        }
    }

    @Test
    fun reportTest() {
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(readTestFile("reportSuccess")))
        val result = runBlocking {
            dataSource.report("", emptyList())
        }
        assertThat(result).isInstanceOf(RobertResult.Success::class.java)

        testErrors {
            dataSource.report("", emptyList())
        }
    }

    @Test
    fun unregisterTest() {
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(readTestFile("unregisterSuccess")))
        val result = runBlocking {
            dataSource.unregister(ServerStatusUpdate("", "", ""))
        }
        assertThat(result).isInstanceOf(RobertResult.Success::class.java)

        testErrors {
            dataSource.unregister(ServerStatusUpdate("", "", ""))
        }
    }

    @Test
    fun deleteExposureHistory() {
        server.enqueue(MockResponse().setResponseCode(200)
            .setBody(readTestFile("deleteExposureHistorySuccess")))
        val result = runBlocking {
            dataSource.deleteExposureHistory(ServerStatusUpdate("", "", ""))
        }
        assertThat(result).isInstanceOf(RobertResult.Success::class.java)

        testErrors {
            dataSource.deleteExposureHistory(ServerStatusUpdate("", "", ""))
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

    private fun String.toByteArray(): ByteArray {
        val split = split('.')
        val byteBuffer = ByteBuffer.allocate(split.size)
        split.forEach {
            byteBuffer.put(it.toByte())
        }
        return byteBuffer.array()
    }

    private fun readTestFile(filename: String): String =
        this.javaClass.classLoader!!.getResourceAsStream(filename).use {
            it.bufferedReader().readText()
        }
}