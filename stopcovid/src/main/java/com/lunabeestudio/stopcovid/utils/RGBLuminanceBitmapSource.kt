/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/6/12 - for the TOUS-ANTI-COVID project
 */
package com.lunabeestudio.stopcovid.utils

import android.graphics.Bitmap
import com.google.zxing.LuminanceSource

/**
 * This class is used to help decode images from files which arrive as RGB data from
 * a [Bitmap]. It does not support rotation.
 *
 * Inspired from https://github.com/zxing/zxing/blob/zxing-3.4.1/core/src/main/java/com/google/zxing/RGBLuminanceSource.java
 */
class RGBLuminanceBitmapSource : LuminanceSource {
    private val luminances: ByteArray
    private val dataWidth: Int
    private val dataHeight: Int
    private val left: Int
    private val top: Int

    constructor(bitmap: Bitmap) : super(bitmap.width, bitmap.height) {
        dataWidth = bitmap.width
        dataHeight = bitmap.height
        left = 0
        top = 0

        // In order to measure pure decoding speed, we convert the entire image to a greyscale array
        // up front, which is the same as the Y channel of the YUVLuminanceSource in the real app.
        //
        // Total number of pixels suffices, can ignore shape
        val size = dataWidth * dataHeight
        luminances = ByteArray(size)
        val pixelsRows = IntArray(dataWidth * ROW_AMOUNT)
        var y = 0
        while (y < dataHeight) {
            val rowsToRead = if (y + ROW_AMOUNT > dataHeight) dataHeight - y else ROW_AMOUNT
            bitmap.getPixels(pixelsRows, 0, dataWidth, 0, y, dataWidth, rowsToRead)
            for (offset in 0 until dataWidth * rowsToRead) {
                val pixel = pixelsRows[offset]
                val r = pixel shr 16 and 0xff // red
                val g2 = pixel shr 7 and 0x1fe // 2 * green
                val b = pixel and 0xff // blue
                // Calculate green-favouring average cheaply
                luminances[y * dataWidth + offset] = ((r + g2 + b) / 4).toByte()
            }
            y += ROW_AMOUNT
        }
    }

    private constructor(
        pixels: ByteArray,
        dataWidth: Int,
        dataHeight: Int,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ) : super(width, height) {
        require(!(left + width > dataWidth || top + height > dataHeight)) { "Crop rectangle does not fit within image data." }
        luminances = pixels
        this.dataWidth = dataWidth
        this.dataHeight = dataHeight
        this.left = left
        this.top = top
    }

    override fun getRow(y: Int, row: ByteArray): ByteArray {
        var retRow: ByteArray? = row
        require(!(y < 0 || y >= height)) { "Requested row is outside the image: $y" }
        val width = width
        if (retRow == null || retRow.size < width) {
            retRow = ByteArray(width)
        }
        val offset = (y + top) * dataWidth + left
        System.arraycopy(luminances, offset, retRow, 0, width)
        return retRow
    }

    override fun getMatrix(): ByteArray {
        val width = width
        val height = height

        // If the caller asks for the entire underlying image, save the copy and give them the
        // original data. The docs specifically warn that result.length must be ignored.
        if (width == dataWidth && height == dataHeight) {
            return luminances
        }
        val area = width * height
        val matrix = ByteArray(area)
        var inputOffset = top * dataWidth + left

        // If the width matches the full width of the underlying data, perform a single copy.
        if (width == dataWidth) {
            System.arraycopy(luminances, inputOffset, matrix, 0, area)
            return matrix
        }

        // Otherwise copy one cropped row at a time.
        for (y in 0 until height) {
            val outputOffset = y * width
            System.arraycopy(luminances, inputOffset, matrix, outputOffset, width)
            inputOffset += dataWidth
        }
        return matrix
    }

    override fun isCropSupported(): Boolean {
        return true
    }

    override fun crop(left: Int, top: Int, width: Int, height: Int): LuminanceSource {
        return RGBLuminanceBitmapSource(
            luminances,
            dataWidth,
            dataHeight,
            this.left + left,
            this.top + top,
            width,
            height
        )
    }

    companion object {
        private const val ROW_AMOUNT = 100
    }
}