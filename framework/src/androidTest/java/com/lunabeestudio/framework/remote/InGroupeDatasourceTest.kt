/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/8/7 - for the TOUS-ANTI-COVID project
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/8/7 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote

import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.framework.crypto.BouncyCastleCryptoDataSource
import com.lunabeestudio.framework.remote.datasource.InGroupeDatasource
import com.lunabeestudio.framework.remote.model.ApiConversionRQ
import com.lunabeestudio.framework.testutils.CryptoUtils
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.BackendException
import com.lunabeestudio.robert.model.RobertResultData
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.security.KeyPair

class InGroupeDatasourceTest {
    private val server = MockWebServer()
    private val serverKeyPair: KeyPair = CryptoUtils.generateEcdhKeyPair()

    private lateinit var robertManager: RobertManager
    private lateinit var configuration: Configuration

    @MockK(relaxed = true)
    private lateinit var analyticsManager: AnalyticsManager

    @Before
    fun init() {
        MockKAnnotations.init(this)
        robertManager = mockk(relaxed = true)
        configuration = mockk(relaxed = true)
        every { robertManager.configuration } returns configuration

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (request.path?.startsWith(CONVERSION_PATH) == true) {
                    try {
                        val apiConversionRq = Gson().fromJson(request.body.readString(Charsets.UTF_8), ApiConversionRQ::class.java)
                        val sharedKey = CryptoUtils.generateKey(
                            serverKeyPair.private,
                            request.requestUrl!!.queryParameter("publicKey")!!,
                        )
                        val clearInput = CryptoUtils.decodeDecrypt(sharedKey, apiConversionRq.chainEncoded)
                        val encryptedOutput = CryptoUtils.encryptEncode(sharedKey, clearInput.toOutput())

                        MockResponse()
                            .setResponseCode(200)
                            .setBody(encryptedOutput)
                    } catch (e: Exception) {
                        val errorBody = "{ \"codeError\":\"$ERROR_CODE\", \"msgError\":\"${e.localizedMessage}\" }"
                        MockResponse()
                            .setResponseCode(400)
                            .setBody(errorBody)
                    }
                } else {
                    MockResponse()
                        .setResponseCode(404)
                }
            }
        }

        server.start()
    }

    @Test
    fun convert_certificate_success() {
        val inputString = "Is NSA watching?"
        val expectedOutput = inputString.toOutput()

        val inGroupeDatasource = InGroupeDatasource(
            InstrumentationRegistry.getInstrumentation().context,
            BouncyCastleCryptoDataSource(),
            server.url("/").toString(),
            analyticsManager,
        )

        every { configuration.conversionPublicKey } returns hashMapOf(
            Pair("good_key", Base64.encodeToString(serverKeyPair.public.encoded, Base64.NO_WRAP))
        )

        val convertResult = runBlocking {
            inGroupeDatasource.convertCertificateV2(
                robertManager,
                inputString,
                WalletCertificateType.Format.WALLET_2D,
                WalletCertificateType.Format.WALLET_DCC,
            )
        }

        assert(convertResult is RobertResultData.Success)
        assert((convertResult as RobertResultData.Success).data == expectedOutput) {
            Timber.e(convertResult.data)
        }
    }

    @Test
    fun convert_certificate_bad_key() {
        val inputString = "Is NSA watching?"

        val badServerKey = CryptoUtils.generateEcdhKeyPair().public.encoded

        every { configuration.conversionPublicKey } returns hashMapOf(
            Pair("bad_key", Base64.encodeToString(badServerKey, Base64.NO_WRAP))
        )

        val inGroupeDatasource = InGroupeDatasource(
            InstrumentationRegistry.getInstrumentation().context,
            BouncyCastleCryptoDataSource(),
            server.url("/").toString(),
            analyticsManager,
        )

        val convertResult = runBlocking {
            inGroupeDatasource.convertCertificateV2(
                robertManager,
                inputString,
                WalletCertificateType.Format.WALLET_2D,
                WalletCertificateType.Format.WALLET_DCC,
            )
        }

        assert(convertResult is RobertResultData.Failure)
        assert(((convertResult as RobertResultData.Failure).error as BackendException).message.endsWith("($ERROR_CODE)"))
    }

    private fun String.toOutput(): String = "$this Yes."

    companion object {
        private const val CONVERSION_PATH: String = "/api/v2/client/convertor/decode/decodeDocument"
        private const val ERROR_CODE: String = "SERVER_ERROR"
    }
}