/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/22/6 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote.datasource

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AnalyticsServiceName
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.framework.crypto.ServerKeyAgreementHelper
import com.lunabeestudio.framework.remote.RetrofitClient
import com.lunabeestudio.framework.remote.extension.remoteToRobertException
import com.lunabeestudio.framework.remote.model.ApiConversionErrorRS
import com.lunabeestudio.framework.remote.model.ApiConversionRQ
import com.lunabeestudio.framework.remote.server.InGroupeApi
import com.lunabeestudio.robert.datasource.RemoteConversionDataSource
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
import com.lunabeestudio.robert.model.BackendException
import com.lunabeestudio.robert.model.NoInternetException
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.model.UnknownException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber

class InGroupeDatasource(
    context: Context,
    sharedCryptoDataSource: SharedCryptoDataSource,
    baseUrl: String,
    private val analyticsManager: AnalyticsManager,
) : RemoteConversionDataSource {

    private val okHttpClient: OkHttpClient = RetrofitClient.getDefaultOKHttpClient(context, null)
    private val serverKeyAgreementHelper = ServerKeyAgreementHelper(sharedCryptoDataSource)

    private val api: InGroupeApi = RetrofitClient.getService(baseUrl, InGroupeApi::class.java, okHttpClient)
    private val gson = Gson()

    override suspend fun convertCertificateV1(
        encodedCertificate: String,
        from: WalletCertificateType.Format,
        to: WalletCertificateType.Format
    ): RobertResultData<String> {

        val apiConversionRq = ApiConversionRQ(chainEncoded = encodedCertificate, destination = to.toApiKey(), source = from.toApiKey())

        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            try {
                val response = api.convertV1(apiConversionRq)
                val bodyRs = response.body()?.string()

                if (response.isSuccessful && bodyRs != null) {
                    RobertResultData.Success(bodyRs)
                } else {
                    var analyticsErrorDesc: String? = null
                    val httpCode = response.code()
                    try {
                        val error = gson.fromJson(bodyRs, ApiConversionErrorRS::class.java)
                        analyticsErrorDesc = "${error.msgError} (${error.codeError})"
                        RobertResultData.Failure(BackendException("${error.msgError} (${error.codeError})", httpCode))
                    } catch (e: JsonSyntaxException) {
                        RobertResultData.Failure(BackendException("Unable to parse body result: $bodyRs", httpCode))
                    } finally {
                        analyticsManager.reportWSError(
                            AnalyticsServiceName.CERTIFICATE_CONVERSION,
                            "0",
                            httpCode,
                            analyticsErrorDesc,
                        )
                    }
                }
            } catch (e: Exception) {
                val robertException = e.remoteToRobertException()
                if (robertException !is NoInternetException) {
                    analyticsManager.reportWSError(
                        AnalyticsServiceName.CERTIFICATE_CONVERSION,
                        "0",
                        0,
                        e.message,
                    )
                }
                RobertResultData.Failure(robertException)
            }
        }
    }

    override suspend fun convertCertificateV2(
        serverKeyConfig: Pair<String, String>,
        encodedCertificate: String,
        from: WalletCertificateType.Format,
        to: WalletCertificateType.Format
    ): RobertResultData<String> {

        val serverKeyAgreementData = try {
            serverKeyAgreementHelper.encryptRequestData(serverKeyConfig.second, encodedCertificate)
        } catch (e: Exception) {
            Timber.e(e)
            return RobertResultData.Failure(UnknownException("Failed to encrypt certificate for conversion\n${e.message}"))
        }

        val apiConversionRq = ApiConversionRQ(
            chainEncoded = serverKeyAgreementData.encryptedData,
            destination = to.toApiKey(),
            source = from.toApiKey(),
        )

        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            try {
                val response = api.convertV2(serverKeyAgreementData.encodedLocalPublicKey, serverKeyConfig.first, apiConversionRq)

                if (response.isSuccessful) {
                    response.body()?.string()?.let {
                        val certificate = serverKeyAgreementHelper.decryptResponse(it)
                        RobertResultData.Success(certificate)
                    } ?: RobertResultData.Failure(BackendException("Response successful but body is empty", response.code()))
                } else {
                    var analyticsErrorDesc: String? = null
                    val bodyError = response.errorBody()?.string()
                    val httpCode = response.code()
                    try {
                        val error = gson.fromJson(bodyError, ApiConversionErrorRS::class.java)
                        analyticsErrorDesc = "${error.msgError} (${error.codeError})"
                        RobertResultData.Failure(BackendException("${error.msgError} (${error.codeError})", httpCode))
                    } catch (e: JsonSyntaxException) {
                        RobertResultData.Failure(BackendException("Unable to parse body error: $bodyError", httpCode))
                    } finally {
                        analyticsManager.reportWSError(
                            AnalyticsServiceName.CERTIFICATE_CONVERSION,
                            "0",
                            httpCode,
                            analyticsErrorDesc,
                        )
                    }
                }
            } catch (e: Exception) {
                val robertException = e.remoteToRobertException()
                if (robertException !is NoInternetException) {
                    analyticsManager.reportWSError(
                        AnalyticsServiceName.CERTIFICATE_CONVERSION,
                        "0",
                        0,
                        e.message,
                    )
                }
                RobertResultData.Failure(robertException)
            } finally {
                okHttpClient.connectionPool.evictAll()
            }
        }
    }

    fun WalletCertificateType.Format.toApiKey(): String {
        return when (this) {
            WalletCertificateType.Format.WALLET_2D -> "DEUX_D_DOC"
            WalletCertificateType.Format.WALLET_DCC -> "DGCA"
        }
    }
}
