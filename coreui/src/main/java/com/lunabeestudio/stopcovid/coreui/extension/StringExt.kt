/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.emoji.text.EmojiCompat
import com.lunabeestudio.stopcovid.coreui.BuildConfig
import com.lunabeestudio.stopcovid.coreui.network.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.File

@WorkerThread
fun String.download(context: Context): okhttp3.Response {
    val okHttpClient = OkHttpClient.getDefaultOKHttpClient(context, this, BuildConfig.SERVER_CERTIFICATE_SHA256)
    val request: Request = Request.Builder()
        .url(this)
        .build()
    val response = okHttpClient.newCall(request).execute()
    return if (response.isSuccessful && response.body != null) {
        response
    } else {
        Timber.d(response.body?.string())
        throw HttpException(Response.error<Any>(response.body!!, response))
    }
}

@WorkerThread
fun String.saveTo(context: Context, file: File) {
    val okHttpClient = OkHttpClient.getDefaultOKHttpClient(context, this, BuildConfig.SERVER_CERTIFICATE_SHA256)
    val request: Request = Request.Builder()
        .url(this)
        .build()
    val response = okHttpClient.newCall(request).execute()
    if (response.isSuccessful && response.body != null) {
        response.body!!.string().byteInputStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output, 4 * 1024)
            }
        }
    } else {
        Timber.d(response.body?.string())
        throw HttpException(Response.error<Any>(response.body!!, response))
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

fun String.fixFormatter() = this.replace("%@", "%s")