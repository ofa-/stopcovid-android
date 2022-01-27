/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/22/9 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote.datasource

import com.google.gson.Gson
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AnalyticsServiceName
import com.lunabeestudio.domain.model.DccLightData
import com.lunabeestudio.framework.crypto.ServerKeyAgreementHelper
import com.lunabeestudio.framework.remote.RetrofitClient
import com.lunabeestudio.framework.remote.extension.remoteToRobertException
import com.lunabeestudio.framework.remote.model.ApiAggregateCertificate
import com.lunabeestudio.framework.remote.model.ApiAggregateError
import com.lunabeestudio.framework.remote.model.ApiAggregateRQ
import com.lunabeestudio.framework.remote.model.ApiDccLightList
import com.lunabeestudio.framework.remote.model.ApiGenerateRQ
import com.lunabeestudio.framework.remote.server.DccLightApi
import com.lunabeestudio.robert.datasource.RemoteDccLightDataSource
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
import com.lunabeestudio.robert.model.AggregateBackendException
import com.lunabeestudio.robert.model.BackendException
import com.lunabeestudio.robert.model.NoInternetException
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.model.UnknownException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.Date

class DccLightDataSource(
    sharedCryptoDataSource: SharedCryptoDataSource,
    baseUrl: String,
    okHttpClient: OkHttpClient,
    private val analyticsManager: AnalyticsManager,
) : RemoteDccLightDataSource {

    private val api: DccLightApi = RetrofitClient.getService(baseUrl, DccLightApi::class.java, okHttpClient)
    private val serverKeyAgreementHelper = ServerKeyAgreementHelper(sharedCryptoDataSource)
    private val gson = Gson()

    override suspend fun generateActivityPass(
        serverPublicKey: String,
        encodedCertificate: String,
    ): RobertResultData<DccLightData> {
        val serverKeyAgreementData = try {
            serverKeyAgreementHelper.encryptRequestData(serverPublicKey, listOf(encodedCertificate))
        } catch (e: Exception) {
            Timber.e(e)
            return RobertResultData.Failure(UnknownException("Failed to encrypt certificate for dcc generation\n${e.message}"))
        }

        val apiGenerateRQ = ApiGenerateRQ(
            serverKeyAgreementData.encodedLocalPublicKey,
            serverKeyAgreementData.encryptedData.first(),
        )

        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            try {
                val response = api.generate(apiGenerateRQ)

                if (response.isSuccessful) {
                    response.body()?.let {
                        val clearResponse = serverKeyAgreementHelper.decryptResponse(it.response)
                        val dccLights = gson.fromJson(clearResponse, ApiDccLightList::class.java)
                        val dccLightData = DccLightData(
                            dccLights.complete,
                            dccLights.lightCertificates.associate { apiDccLight ->
                                Pair(Date(apiDccLight.exp * 1000), apiDccLight.dcc)
                            }
                        )

                        RobertResultData.Success(dccLightData)
                    } ?: RobertResultData.Failure(BackendException("Response successful but body is empty", response.code()))
                } else {
                    RobertResultData.Failure(BackendException(httpCode = response.code()))
                }
            } catch (e: Exception) {
                val robertException = e.remoteToRobertException()
                if (robertException !is NoInternetException) {
                    analyticsManager.reportWSError(
                        AnalyticsServiceName.DCC_LIGHT,
                        "0",
                        0,
                        e.message,
                    )
                }
                RobertResultData.Failure(robertException)
            }
        }
    }

    override suspend fun generateMultipass(serverPublicKey: String, encodedCertificateList: List<String>): RobertResultData<String> {
        val serverKeyAgreementData = try {
            serverKeyAgreementHelper.encryptRequestData(serverPublicKey, encodedCertificateList)
        } catch (e: Exception) {
            Timber.e(e)
            return RobertResultData.Failure(UnknownException("Failed to encrypt certificate for dcc aggregation\n${e.message}"))
        }

        val apiAggregateRQ = ApiAggregateRQ(
            serverKeyAgreementData.encodedLocalPublicKey,
            serverKeyAgreementData.encryptedData,
        )

        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            try {
                val response = api.aggregate(apiAggregateRQ)
                if (response.isSuccessful) {
                    response.body()?.let {
                        val multipassJson = serverKeyAgreementHelper.decryptResponse(it.response)
                        val multipass = gson.fromJson(multipassJson, ApiAggregateCertificate::class.java).certificate
                        RobertResultData.Success(multipass)
                    } ?: RobertResultData.Failure(BackendException("Response successful but body is empty", response.code()))
                } else {
                    val error = response.errorBody()?.string()?.let { errorBody ->
                        gson.fromJson(errorBody, ApiAggregateError::class.java)
                    }
                    if (error != null) {
                        RobertResultData.Failure(
                            AggregateBackendException(
                                message = error.message,
                                httpCode = response.code(),
                                errorCodes = error.errors.map { it.code },
                            )
                        )
                    } else {
                        RobertResultData.Failure(BackendException(httpCode = response.code()))
                    }
                }
            } catch (e: Exception) {
                val robertException = e.remoteToRobertException()
                if (robertException !is NoInternetException) {
                    analyticsManager.reportWSError(
                        AnalyticsServiceName.DCC_LIGHT_AGGREGATE,
                        "0",
                        0,
                        e.message,
                    )
                }
                RobertResultData.Failure(robertException)
            }
        }
    }
}
