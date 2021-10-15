/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/01/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import java.io.FileNotFoundException
import java.io.InputStream

abstract class QRCodeDccFragment : QRCodeFragment() {

    private val pickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { activityResult -> onPickerResult(activityResult) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.import_qr_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.item_import).title = strings["universalQrScanController.rightBarButton.title"]
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.item_import) {
            openGallery()
            true
        } else {
            false
        }
    }

    private fun openGallery() {
        val intent = Intent()
            .setType("image/*")
            .setAction(Intent.ACTION_GET_CONTENT)

        pickerLauncher.launch(intent)
    }

    private fun onPickerResult(activityResult: ActivityResult) {
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val uri = activityResult.data?.data

            val openInputStream = try {
                if (uri != null) {
                    context?.contentResolver?.openInputStream(uri)
                } else {
                    return
                }
            } catch (e: FileNotFoundException) {
                return
            }

            openInputStream?.let {
                scanImageFile(it)
            }
        }
    }

    fun onScanResult(result: Result?) {
        if (result != null) {
            onCodeScanned(result.text)
        } else {
            strings["universalQrScanController.error.noCodeFound"]?.let { str ->
                (activity as MainActivity).showErrorSnackBar(str)
            }
        }
    }

    fun scanImageFile(inputStream: InputStream) {
        onScanResult(
            scanBitmap(BitmapFactory.decodeStream(inputStream))
        )
    }

    fun scanBitmap(bitmap: Bitmap): Result? {
        val width: Int = bitmap.width
        val height: Int = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val bBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        bitmap.recycle()
        return try {
            reader.decode(bBitmap)
        } catch (e: NotFoundException) {
            null
        }
    }
}