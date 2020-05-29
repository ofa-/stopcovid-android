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
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.annotation.WorkerThread
import androidx.emoji.text.EmojiCompat
import com.lunabeestudio.stopcovid.coreui.BuildConfig
import com.lunabeestudio.stopcovid.coreui.network.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.text.Normalizer
import java.util.Locale

@WorkerThread
fun String.download(context: Context): String {
    val okHttpClient = OkHttpClient.getDefaultOKHttpClient(context, this, BuildConfig.SERVER_CERTIFICATE_SHA256)
    val request: Request = Request.Builder()
        .url(this)
        .build()
    val response = okHttpClient.newCall(request).execute()
    return if (response.isSuccessful && response.body != null) {
        response.body!!.string()
    } else {
        Timber.d(response.body?.string())
        throw Exception()
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
                input.copyTo(output)
            }
        }
    } else {
        Timber.d(response.body?.string())
        throw Exception()
    }
}

fun String.callPhone(context: Context) {
    val callIntent = Intent(Intent.ACTION_DIAL)
    callIntent.data = Uri.parse("tel:$this")
    context.startActivity(callIntent)
}

/**
 * Remove all the diacritics from a string.
 *
 * @return A diacritic free String.
 */
fun String.removeAccents(): String = Normalizer.normalize(this,
    Normalizer.Form.NFD).replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

/**
 * Remove the diacritics from a string and lowercase the result.
 *
 * @return A lowercase diacritic free String.
 */
fun String.transformForSearch(): String = this.removeAccents().toLowerCase(Locale.getDefault())

fun String.boldSubstringForSearch(treatedString: String,
    treatedSearchString: String): SpannableString {
    val spannable = SpannableString(this)
    if (treatedSearchString.isNotEmpty() && treatedString.contains(treatedSearchString)) {
        spannable.setSpan(StyleSpan(Typeface.NORMAL), 0, this.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(StyleSpan(Typeface.BOLD),
            treatedString.indexOf(treatedSearchString),
            treatedString.indexOf(treatedSearchString) + treatedSearchString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannable
}

fun String?.safeEmojiSpanify(): CharSequence? {
    return try {
        EmojiCompat.get().process(this ?: "")
    } catch (_: IllegalStateException) {
        this
    }
}