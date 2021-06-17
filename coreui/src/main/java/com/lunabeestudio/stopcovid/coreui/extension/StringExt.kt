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
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun String.saveTo(context: Context, file: File): Boolean {
    return withContext(Dispatchers.IO) {
        val cacheConfig = CacheConfig(File(context.cacheDir, "http_cache"), 30 * 1024 * 1024)
        val okHttpClient = OkHttpClient.getDefaultOKHttpClient(context, this@saveTo, ConfigConstant.SERVER_CERTIFICATE_SHA256, cacheConfig)
        val request: Request = Request.Builder().apply {
            url(this@saveTo)
        }.build()

        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful && response.body != null && response.networkResponse?.code != HttpURLConnection.HTTP_NOT_MODIFIED) {
            response.body!!.byteStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output, 4 * 1024)
                }
            }
            true
        } else if (response.networkResponse?.code == HttpURLConnection.HTTP_NOT_MODIFIED) {
            true
        } else {
            throw HttpException(Response.error<Any>(response.body!!, response))
        }
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun String.saveTo(context: Context, atomicFile: AtomicFile): FileOutputStream? {
    return withContext(Dispatchers.IO) {
        val cacheConfig = CacheConfig(File(context.cacheDir, "http_cache"), 30 * 1024 * 1024)
        val okHttpClient = OkHttpClient.getDefaultOKHttpClient(context, this@saveTo, ConfigConstant.SERVER_CERTIFICATE_SHA256, cacheConfig)
        val request: Request = Request.Builder().apply {
            url(this@saveTo)
        }.build()

        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful && response.body != null && response.networkResponse?.code != HttpURLConnection.HTTP_NOT_MODIFIED) {
            val fileOutputStream = atomicFile.startWrite()
            response.body!!.byteStream().use { input ->
                input.copyTo(fileOutputStream, 4 * 1024)
            }
            fileOutputStream
        } else if (response.networkResponse?.code == HttpURLConnection.HTTP_NOT_MODIFIED) {
            null
        } else {
            throw HttpException(Response.error<Any>(response.body!!, response))
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
