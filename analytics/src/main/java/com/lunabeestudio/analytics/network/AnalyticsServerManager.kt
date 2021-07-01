/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/01/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.analytics.network

import android.content.Context
import android.os.Build
import com.lunabeestudio.analytics.BuildConfig
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AnalyticsResult
import com.lunabeestudio.analytics.model.AnalyticsServiceName
import com.lunabeestudio.analytics.network.model.SendAnalyticsRQ
import com.lunabeestudio.analytics.network.model.SendAppAnalyticsRQ
import com.lunabeestudio.analytics.network.model.SendHealthAnalyticsRQ
import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.tls.HandshakeCertificates
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

internal object AnalyticsServerManager {

    private fun getRetrofit(context: Context, baseUrl: String, certificateSha256: String, token: String): AnalyticsApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(
                OkHttpClient.Builder().apply {
                    if (!BuildConfig.DEBUG) {
                        val requireTls12 = ConnectionSpec.Builder(ConnectionSpec.RESTRICTED_TLS)
                            .tlsVersions(TlsVersion.TLS_1_2)
                            .build()
                        connectionSpecs(listOf(requireTls12))
                    }
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                        certificatePinner(
                            CertificatePinner.Builder()
                                .add(
                                    baseUrl.toHttpUrl().host,
                                    certificateSha256
                                )
                                .build()
                        )
                        val certificates: HandshakeCertificates = HandshakeCertificates.Builder()
                            .addTrustedCertificate(certificateFromString(context, "analytics_api_tousanticovid_gouv_fr"))
                            .build()
                        sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
                    }
                    addInterceptor(getDefaultHeaderInterceptor(token))
                    addInterceptor(getLogInterceptor())
                    callTimeout(30L, TimeUnit.SECONDS)
                    connectTimeout(30L, TimeUnit.SECONDS)
                    readTimeout(30L, TimeUnit.SECONDS)
                    writeTimeout(30L, TimeUnit.SECONDS)
                }.build()
            )
            .build()
            .create(AnalyticsApi::class.java)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun sendAnalytics(
        context: Context,
        baseUrl: String,
        certificateSha256: String,
        apiVersion: String,
        token: String,
        sendAnalyticsRQ: SendAnalyticsRQ
    ): AnalyticsResult {
        return try {
            val result = when (sendAnalyticsRQ) {
                is SendAppAnalyticsRQ -> getRetrofit(context, baseUrl, certificateSha256, token).sendAppAnalytics(
                    apiVersion,
                    sendAnalyticsRQ
                )
                is SendHealthAnalyticsRQ -> getRetrofit(context, baseUrl, certificateSha256, token).sendHealthAnalytics(
                    apiVersion,
                    sendAnalyticsRQ
                )
            }
            if (result.isSuccessful) {
                AnalyticsResult.Success()
            } else {
                AnalyticsResult.Failure(HttpException(result))
            }
        } catch (e: Exception) {
            AnalyticsResult.Failure(e)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun deleteAnalytics(
        context: Context,
        baseUrl: String,
        certificateSha256: String,
        apiVersion: String,
        token: String,
        installationUuid: String,
    ): AnalyticsResult {
        return try {
            val result = getRetrofit(context, baseUrl, certificateSha256, token).deleteAnalytics(apiVersion, installationUuid)
            if (result.isSuccessful) {
                AnalyticsResult.Success()
            } else {
                AnalyticsManager.reportWSError(
                    context,
                    context.filesDir,
                    AnalyticsServiceName.ANALYTICS,
                    apiVersion,
                    result.code(),
                    result.message()
                )
                AnalyticsResult.Failure(HttpException(result))
            }
        } catch (e: Exception) {
            AnalyticsManager.reportWSError(
                context,
                context.filesDir,
                AnalyticsServiceName.ANALYTICS,
                apiVersion,
                (e as? HttpException)?.code() ?: 0,
                e.message
            )
            AnalyticsResult.Failure(e)
        }
    }

    private fun certificateFromString(context: Context, fileName: String): X509Certificate {
        return CertificateFactory.getInstance("X.509").generateCertificate(
            context.resources.openRawResource(
                context.resources.getIdentifier(
                    fileName,
                    "raw", context.packageName
                )
            )
        ) as X509Certificate
    }

    private fun getDefaultHeaderInterceptor(token: String): Interceptor = Interceptor { chain ->
        val request = chain.request()
            .newBuilder().apply {
                addHeader("Accept", "application/json")
                addHeader("Content-Type", "application/json")
                addHeader("Authorization", "Bearer $token")
            }.build()
        chain.proceed(request)
    }

    private fun getLogInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor { message -> Timber.v(message) }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
}
