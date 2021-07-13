/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote

import android.content.Context
import android.os.Build
import com.lunabeestudio.domain.model.CacheConfig
import com.lunabeestudio.framework.BuildConfig
import okhttp3.Cache
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.tls.HandshakeCertificates
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

object RetrofitClient {

    internal fun <T> getService(
        context: Context,
        baseUrl: String,
        clazz: Class<T>,
        cacheConfig: CacheConfig?,
        onProgressUpdate: ((Float) -> Unit)? = null,
    ): T {
        return Retrofit.Builder()
            .baseUrl(baseUrl.toHttpUrl())
            .addConverterFactory(MoshiConverterFactory.create())
            .client(getDefaultOKHttpClient(context, cacheConfig, onProgressUpdate))
            .build().create(clazz)
    }

    internal fun <T> getFileService(context: Context, baseUrl: String, clazz: Class<T>): T {
        return Retrofit.Builder()
            .baseUrl(baseUrl.toHttpUrl())
            .client(getFileOKHttpClient(context))
            .build().create(clazz)
    }

    fun getDefaultOKHttpClient(
        context: Context,
        cacheConfig: CacheConfig?,
        onProgressUpdate: ((Float) -> Unit)? = null,
    ): OkHttpClient {
        val requireTls12 = ConnectionSpec.Builder(ConnectionSpec.RESTRICTED_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .build()
        return OkHttpClient.Builder().apply {
            if (!BuildConfig.DEBUG) {
                connectionSpecs(listOf(requireTls12))
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                val certificates: HandshakeCertificates = HandshakeCertificates.Builder()
                    .addTrustedCertificate(certificateFromString(context, "certigna_services"))
                    .addTrustedCertificate(certificateFromString(context, "r3"))
                    .addTrustedCertificate(certificateFromString(context, "l1k"))
                    .build()
                sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
            }
            addInterceptor(getDefaultHeaderInterceptor())
            addInterceptor(getLogInterceptor())
            addNetworkInterceptor(getProgressInterceptor(onProgressUpdate))
            callTimeout(1L, TimeUnit.MINUTES)
            connectTimeout(1L, TimeUnit.MINUTES)
            readTimeout(1L, TimeUnit.MINUTES)
            writeTimeout(1L, TimeUnit.MINUTES)
            if (cacheConfig != null) {
                cache(Cache(cacheConfig.cacheDir, cacheConfig.cacheSize))
            }
        }.build()
    }

    private fun getFileOKHttpClient(context: Context): OkHttpClient {
        val requireTls12 = ConnectionSpec.Builder(ConnectionSpec.RESTRICTED_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .build()
        return OkHttpClient.Builder().apply {
            if (!BuildConfig.DEBUG) {
                connectionSpecs(listOf(requireTls12))
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                val certificates: HandshakeCertificates = HandshakeCertificates.Builder()
                    .addTrustedCertificate(certificateFromString(context, "certigna_services"))
                    .build()
                sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
            }
            addInterceptor(getFileHeaderInterceptor())
            callTimeout(30L, TimeUnit.SECONDS)
            connectTimeout(30L, TimeUnit.SECONDS)
            readTimeout(30L, TimeUnit.SECONDS)
            writeTimeout(30L, TimeUnit.SECONDS)
        }.build()
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

    private fun getDefaultHeaderInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()
            .newBuilder().apply {
                addHeader("Accept", "application/json")
                addHeader("Content-Type", "application/json")
            }.build()
        chain.proceed(request)
    }

    private fun getFileHeaderInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()
            .newBuilder().apply {
                addHeader("Content-Type", "application/json")
            }.build()
        chain.proceed(request)
    }

    private fun getLogInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor { message -> Timber.v(message) }.apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private fun getProgressInterceptor(onProgressUpdate: ((Float) -> Unit)?): Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        val requestBody = originalRequest.body
        if (requestBody == null) {
            chain.proceed(originalRequest)
        } else {
            val progressRequest = originalRequest.newBuilder()
                .method(
                    originalRequest.method,
                    UploadProgressRequestBody(
                        requestBody,
                        object : UploadProgressRequestBody.ProgressListener {
                            override fun update(bytesWritten: Long, contentLength: Long) {
                                onProgressUpdate?.invoke(bytesWritten.toFloat() / contentLength.toFloat())
                            }
                        }
                    )
                )
                .build()

            chain.proceed(progressRequest)
        }
    }
}
