/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.util.AtomicFile
import androidx.emoji.text.EmojiCompat
import com.lunabeestudio.domain.model.CacheConfig
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.network.OkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun String.saveTo(context: Context, file: File): Boolean {
    return withContext(Dispatchers.IO) {
        val cacheConfig = CacheConfig(File(context.cacheDir, "http_cache"), 30L * 1024L * 1024L)
        val okHttpClient = OkHttpClient.getDefaultOKHttpClient(context, this@saveTo, ConfigConstant.SERVER_CERTIFICATE_SHA256, cacheConfig)
        val request: Request = Request.Builder().apply {
            cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
            url(this@saveTo)
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
            true
        } else {
            throw HttpException(Response.error<Any>(body!!, response))
        }
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun String.saveTo(context: Context, atomicFile: AtomicFile, validData: suspend (data: ByteArray) -> Boolean): Boolean {
    return withContext(Dispatchers.IO) {
        val cacheConfig = CacheConfig(File(context.cacheDir, "http_cache"), 30L * 1024L * 1024L)
        val okHttpClient = OkHttpClient.getDefaultOKHttpClient(context, this@saveTo, ConfigConstant.SERVER_CERTIFICATE_SHA256, cacheConfig)
        val request: Request = Request.Builder().apply {
            cacheControl(CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build())
            url(this@saveTo)
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

fun String.callPhone(context: Context) {
    val callIntent = Intent(Intent.ACTION_DIAL)
    callIntent.data = Uri.parse("tel:$this")
    context.startActivity(callIntent)
}

fun String?.safeEmojiSpanify(): CharSequence? {
    return try {
        EmojiCompat.get().process(this ?: "")
    } catch (e: IllegalStateException) {
        this
    }
}

fun String.fixFormatter(): String = this
    .replace("%@", "%s")
    .replace(Regex("%\\d\\$@")) { matchResult ->
        matchResult.value.replace('@', 's')
    }

fun String?.formatWithSameValue(value: Any?): String? {
    return this?.let {
        val placeholder = "%s"
        val cleanedString = it.replace(placeholder, "")
        val count = (this.count() - cleanedString.count()) / placeholder.count()
        val values = Array(count) { value }
        return String.format(it, *values)
    }
}
