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
import androidx.emoji.text.EmojiCompat
import com.lunabeestudio.stopcovid.coreui.BuildConfig
import com.lunabeestudio.stopcovid.coreui.network.OkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.File

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun String.download(context: Context): okhttp3.Response {
    return withContext(Dispatchers.IO) {
        val okHttpClient = OkHttpClient.getDefaultOKHttpClient(context, this@download, BuildConfig.SERVER_CERTIFICATE_SHA256)
        val request: Request = Request.Builder()
            .url(this@download)
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful && response.body != null) {
            response
        } else {
            Timber.e(response.body?.string())
            throw HttpException(Response.error<Any>(response.body!!, response))
        }
    }
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun String.saveTo(context: Context, file: File) {
    withContext(Dispatchers.IO) {
        val okHttpClient = OkHttpClient.getDefaultOKHttpClient(context, this@saveTo, BuildConfig.SERVER_CERTIFICATE_SHA256)
        val request: Request = Request.Builder()
            .url(this@saveTo)
            .build()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful && response.body != null) {
            response.body!!.string().byteInputStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output, 4 * 1024)
                }
            }
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

fun String.fixFormatter() = this
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
