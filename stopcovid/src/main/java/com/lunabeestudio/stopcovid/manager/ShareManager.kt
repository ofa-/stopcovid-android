/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/30/10 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.lunabeestudio.stopcovid.BuildConfig
import com.lunabeestudio.stopcovid.extension.startTextIntent
import timber.log.Timber
import java.io.File

object ShareManager {

    fun getShareCaptureUriFromBitmap(context: Context, bitmap: Bitmap, filenameWithoutExt: String): Uri {
        val shareImagesDir = File("${context.cacheDir}/shared_images")
        shareImagesDir.mkdir()
        val shareImageFile = File(shareImagesDir, "$filenameWithoutExt.jpeg")
        shareImageFile.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        }
        return FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.share", shareImageFile)
    }

    fun shareImageAndText(context: Context, uri: Uri?, shareString: String?, onError: () -> Unit) {
        if (uri != null) {
            context.grantUriPermission(ANDROID_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareString?.let {
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareString)
            }
            shareIntent.type = MIME_TYPE_JPEG
            try {
                context.startActivity(Intent.createChooser(shareIntent, null))
            } catch (e: Exception) {
                Timber.e(e)
                onError.invoke()
            }
        } else {
            if (shareString != null) {
                context.startTextIntent(shareString)
            } else {
                onError.invoke()
            }
        }
    }

    private const val ANDROID_PACKAGE = "android"
    private const val MIME_TYPE_JPEG = "image/jpeg"
}