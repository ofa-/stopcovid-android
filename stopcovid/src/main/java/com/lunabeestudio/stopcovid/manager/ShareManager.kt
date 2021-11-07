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
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.lunabeestudio.stopcovid.BuildConfig
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureCardBinding
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureChartCardBinding
import com.lunabeestudio.stopcovid.extension.getBitmapForItem
import com.lunabeestudio.stopcovid.extension.getBitmapForItemKeyFigureCardBinding
import com.lunabeestudio.stopcovid.extension.getBitmapForItemKeyFigureChartCardBinding
import com.lunabeestudio.stopcovid.extension.startTextIntent
import com.lunabeestudio.stopcovid.fragment.CertificateSharingBottomSheetFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

object ShareManager {

    fun getShareCaptureUriFromBitmap(context: Context, bitmap: Bitmap, filenameWithoutExt: String): Uri {
        val shareImagesDir = File("${context.cacheDir}/shared_images")
        shareImagesDir.mkdirs()
        val shareImageFile = File(shareImagesDir, "$filenameWithoutExt.jpeg")
        shareImageFile.outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        }
        shareImageFile.deleteOnExit()
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

    suspend fun getShareCaptureUri(binding: ViewBinding, filenameWithoutExt: String): Uri {
        val bitmap = when (binding) {
            is ItemKeyFigureCardBinding -> binding.getBitmapForItemKeyFigureCardBinding()
            is ItemKeyFigureChartCardBinding -> binding.getBitmapForItemKeyFigureChartCardBinding()
            else -> binding.root.getBitmapForItem()
        }
        return getShareCaptureUriFromBitmap(binding.root.context, bitmap, filenameWithoutExt)
    }

    fun setupCertificateSharingBottomSheet(fragment: BaseFragment, text: String?, getCaptureUri: suspend () -> Uri?) {
        with(fragment) {
            setFragmentResultListener(CertificateSharingBottomSheetFragment.CERTIFICATE_SHARING_RESULT_KEY) { _, bundle ->
                if (bundle.getBoolean(CertificateSharingBottomSheetFragment.CERTIFICATE_SHARING_BUNDLE_KEY_CONFIRM, false)) {
                    viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
                        val uri = getCaptureUri()

                        withContext(Dispatchers.Main) {
                            shareImageAndText(requireContext(), uri, text) {
                                strings["common.error.unknown"]?.let { (fragment.activity as? MainActivity)?.showErrorSnackBar(it) }
                            }
                        }
                    }
                }
            }
        }
    }

    private const val ANDROID_PACKAGE = "android"
    private const val MIME_TYPE_JPEG = "image/jpeg"
    const val certificateScreenshotFilename: String = "certificate_screenshot"
}