/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/14/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.ErrorEventName
import com.lunabeestudio.stopcovid.coreui.utils.SingleLiveEvent
import com.lunabeestudio.stopcovid.utils.RGBLuminanceBitmapSource
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.InputStream

class ImportQrBottomViewModel(
    private val handle: SavedStateHandle,
    private val analyticsManager: AnalyticsManager,
    private val dispatcherIO: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    var currentPdfUri: Uri? = handle[CURRENT_PDF_URI_KEY]
        set(value) {
            handle[CURRENT_PDF_URI_KEY] = value
            field = value
        }
    val loading: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val scanResult: SingleLiveEvent<Result?> = SingleLiveEvent()
    val passwordFailure: SingleLiveEvent<Boolean> = SingleLiveEvent()

    fun scanImageFile(context: Context, uri: Uri?) {
        uri.inputStream(context)?.use { inputStream ->
            scanResult.postValue(scanBitmap(BitmapFactory.decodeStream(inputStream)))
        }
    }

    fun scanPdfFile(context: Context, uri: Uri?) {
        currentPdfUri = uri
        getBitmapFromProtectedPDF(context, null)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun getBitmapFromProtectedPDF(context: Context, password: String?) {
        viewModelScope.launch(dispatcherIO) {
            loading.postValue(true)
            try {
                PDFBoxResourceLoader.init(context)
                currentPdfUri.inputStream(context).use { inputStream ->
                    PDDocument.load(inputStream, password).use { pdDocument ->
                        val numberOfPages = pdDocument.numberOfPages
                        val renderer = PDFRenderer(pdDocument)
                        var result: Result? = null
                        var pageNum = 0
                        while (pageNum != numberOfPages) {
                            // Render the image to an RGB Bitmap
                            val bitmap = renderer.renderImage(pageNum, 4f, ImageType.RGB)
                            result = scanBitmap(bitmap)
                            if (result == null) {
                                pageNum++
                            } else {
                                break
                            }
                        }
                        scanResult.postValue(result)
                    }
                }
            } catch (e: InvalidPasswordException) {
                passwordFailure.postValue(!password.isNullOrEmpty())
            } catch (e: Exception) {
                handlePdfImportError(e)
            } catch (e: OutOfMemoryError) {
                handlePdfImportError(e)
            }
            loading.postValue(false)
        }
    }

    private fun handlePdfImportError(e: Throwable) {
        Timber.e(e)
        scanResult.postValue(null)
        analyticsManager.reportErrorEvent(ErrorEventName.ERR_WALLET_PDF_IMPORT)
    }

    private fun Uri?.inputStream(context: Context): InputStream? {
        return try {
            if (this != null) {
                context.contentResolver?.openInputStream(this)
            } else {
                null
            }
        } catch (e: FileNotFoundException) {
            null
        }
    }

    private fun scanBitmap(bitmap: Bitmap): Result? {
        val luminanceSource = RGBLuminanceBitmapSource(bitmap)
        val bBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))
        val reader = MultiFormatReader()
        bitmap.recycle()
        return try {
            reader.decode(bBitmap)
        } catch (e: NotFoundException) {
            null
        }
    }

    companion object {
        private const val CURRENT_PDF_URI_KEY: String = "CURRENT_PDF_URI_KEY"
    }
}

class ImportQrBottomViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val analyticsManager: AnalyticsManager,
) : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        @Suppress("UNCHECKED_CAST")
        return ImportQrBottomViewModel(handle, analyticsManager) as T
    }
}