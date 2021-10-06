/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/27/8 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote.server

import android.content.Context
import android.os.Build
import androidx.core.util.AtomicFile
import com.lunabeestudio.framework.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.ConnectionPool
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.tls.HandshakeCertificates
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.net.HttpURLConnection
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

class ServerManager(context: Context) {
    val okHttpClient: OkHttpClient = getDefaultOKHttpClient(context)

    private fun getDefaultOKHttpClient(context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(getLogInterceptor())
            .callTimeout(30L, TimeUnit.SECONDS)
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .writeTimeout(30L, TimeUnit.SECONDS)
            .connectionPool(getDefaultConnectionPool())
            .cache(Cache(File(context.cacheDir, OKHTTP_CACHE_FILENAME), OKHTTP_MAX_CACHE_SIZE_BYTES))

        val requireTls12 = ConnectionSpec.Builder(ConnectionSpec.RESTRICTED_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .build()
        builder.connectionSpecs(listOf(requireTls12))

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            val certificates: HandshakeCertificates = HandshakeCertificates.Builder()
                .addTrustedCertificate(certificateFromString(context, "certigna_services"))
                .build()
            builder.sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
        }

        return builder.build()
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

    private fun getLogInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor { message -> Timber.v(message) }.apply {
        level = when {
            BuildConfig.DEBUG -> HttpLoggingInterceptor.Level.BODY
            else -> HttpLoggingInterceptor.Level.NONE
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun saveTo(url: String, file: File, acceptMimeType: String): Boolean {
        return withContext(Dispatchers.IO) {
            val request: Request = Request.Builder().apply {
                cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
                addHeader("Accept", acceptMimeType)
                url(url)
            }.build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body
            if (response.isSuccessful && body != null && response.networkResponse?.code != HttpURLConnection.HTTP_NOT_MODIFIED) {
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output, 4 * 1024)
                    }
                }
                true
            } else if (response.networkResponse?.code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                false
            } else {
                throw HttpException(Response.error<Any>(body!!, response))
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun saveTo(url: String, atomicFile: AtomicFile, validData: suspend (data: ByteArray) -> Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            val request: Request = Request.Builder().apply {
                cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
                url(url)
            }.build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body
            if (response.isSuccessful && body != null && response.networkResponse?.code != HttpURLConnection.HTTP_NOT_MODIFIED) {
                body.use {
                    val bodyBytes = body.bytes()
                    val isValid = validData(bodyBytes)
                    if (isValid) {
                        val fileOutputStream = atomicFile.startWrite()
                        try {
                            bodyBytes.inputStream().use { input ->
                                input.copyTo(fileOutputStream, 4 * 1024)
                            }
                            atomicFile.finishWrite(fileOutputStream)
                        } catch (e: Exception) {
                            atomicFile.failWrite(fileOutputStream)
                            throw e
                        }
                    }
                }
                true
            } else if (response.networkResponse?.code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                false
            } else {
                throw HttpException(Response.error<Any>(body!!, response))
            }
        }
    }

    companion object {
        const val OKHTTP_CACHE_FILENAME: String = "http_cache"
        const val OKHTTP_MAX_CACHE_SIZE_BYTES: Long = 30 * 1024 * 1024
        fun getDefaultConnectionPool(): ConnectionPool = ConnectionPool(5, 30, TimeUnit.SECONDS)
    }
}