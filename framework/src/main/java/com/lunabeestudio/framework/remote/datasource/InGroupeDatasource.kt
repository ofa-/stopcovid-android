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
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AnalyticsServiceName
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.framework.remote.RetrofitClient
import com.lunabeestudio.framework.remote.extension.remoteToRobertException
import com.lunabeestudio.framework.remote.model.ApiConversionErrorRS
import com.lunabeestudio.framework.remote.model.ApiConversionRQ
import com.lunabeestudio.framework.remote.server.InGroupeApi
import com.lunabeestudio.robert.RobertConstant
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.datasource.RemoteCertificateDataSource
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
import com.lunabeestudio.robert.model.BackendException
import com.lunabeestudio.robert.model.NoInternetException
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.model.UnknownException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class InGroupeDatasource(
    private val context: Context,
    private val sharedCryptoDataSource: SharedCryptoDataSource,
    baseUrl: String,
) : RemoteCertificateDataSource {

    private val api: InGroupeApi = RetrofitClient.getService(context, baseUrl, InGroupeApi::class.java, null)
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
                    try {
                        val error = gson.fromJson(bodyRs, ApiConversionErrorRS::class.java)
                        analyticsErrorDesc = "${error.msgError} (${error.codeError})"
                        RobertResultData.Failure(BackendException("${error.msgError} (${error.codeError})"))
                    } catch (e: JsonSyntaxException) {
                        RobertResultData.Failure(BackendException("Unable to parse body result: $bodyRs"))
                    } finally {
                        AnalyticsManager.reportWSError(
                            context,
                            context.filesDir,
                            AnalyticsServiceName.CERTIFICATE_CONVERSION,
                            "0",
                            response.code(),
                            analyticsErrorDesc,
                        )
                    }
                }
            } catch (e: Exception) {
                val robertException = e.remoteToRobertException()
                if (robertException !is NoInternetException) {
                    AnalyticsManager.reportWSError(
                        context,
                        context.filesDir,
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
        robertManager: RobertManager,
        encodedCertificate: String,
        from: WalletCertificateType.Format,
        to: WalletCertificateType.Format
    ): RobertResultData<String> {

        val serverKeyConfig = robertManager.configuration.conversionPublicKey.toList().firstOrNull()
            ?: return RobertResultData.Failure(UnknownException("No server conversion key found for conversion v2"))

        val publicKey64: String
        val apiConversionRq: ApiConversionRQ
        val key: ByteArray
        try {
            val rawServerKey = Base64.decode(serverKeyConfig.second, Base64.NO_WRAP)

            val keyPair = sharedCryptoDataSource.createECDHKeyPair()
            publicKey64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)

            key = sharedCryptoDataSource.getEncryptionKeys(
                rawServerPublicKey = rawServerKey,
                rawLocalPrivateKey = keyPair.private.encoded,
                derivationDataArray = listOf(
                    RobertConstant.CONVERSION_STRING_INPUT.toByteArray(Charsets.UTF_8),
                )
            ).first()

            val rawEncryptedCertificate = sharedCryptoDataSource.encrypt(key, encodedCertificate.toByteArray(Charsets.UTF_8))

            apiConversionRq = ApiConversionRQ(
                chainEncoded = Base64.encodeToString(rawEncryptedCertificate, Base64.NO_WRAP),
                destination = to.toApiKey(),
                source = from.toApiKey(),
            )
        } catch (e: Exception) {
            Timber.e(e)
            return RobertResultData.Failure(UnknownException("Failed to encrypt certificate for conversion\n${e.message}"))
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            try {
                val response = api.convertV2(publicKey64, serverKeyConfig.first, apiConversionRq)

                if (response.isSuccessful) {
                    val data = Base64.decode(response.body()?.string(), Base64.NO_WRAP)
                    val certificate = sharedCryptoDataSource.decrypt(key, data).toString(Charsets.UTF_8)
                    RobertResultData.Success(certificate)
                } else {
                    var analyticsErrorDesc: String? = null
                    val bodyError = response.errorBody()?.string()
                    try {
                        val error = gson.fromJson(bodyError, ApiConversionErrorRS::class.java)
                        analyticsErrorDesc = "${error.msgError} (${error.codeError})"
                        RobertResultData.Failure(BackendException("${error.msgError} (${error.codeError})"))
                    } catch (e: JsonSyntaxException) {
                        RobertResultData.Failure(BackendException("Unable to parse body error: $bodyError"))
                    } finally {
                        AnalyticsManager.reportWSError(
                            context,
                            context.filesDir,
                            AnalyticsServiceName.CERTIFICATE_CONVERSION,
                            "0",
                            response.code(),
                            analyticsErrorDesc,
                        )
                    }
                }
            } catch (e: Exception) {
                val robertException = e.remoteToRobertException()
                if (robertException !is NoInternetException) {
                    AnalyticsManager.reportWSError(
                        context,
                        context.filesDir,
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

    fun WalletCertificateType.Format.toApiKey(): String {
        return when (this) {
            WalletCertificateType.Format.WALLET_2D -> "DEUX_D_DOC"
            WalletCertificateType.Format.WALLET_DCC -> "DGCA"
        }
    }
}