/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/27/01 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureCardBinding
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureChartCardBinding
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun ViewBinding.getBitmapForItem(): Bitmap {
    val bitmap = Bitmap.createBitmap(root.measuredWidth, root.measuredHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    suspendCoroutine<Unit> { continuation ->
        root.post {
            root.draw(canvas)
            continuation.resume(Unit)
        }
    }

    return bitmap
}

suspend fun ItemKeyFigureCardBinding.getBitmapForItemKeyFigureCardBinding(): Bitmap {
    val savedShareVisibility = shareButton.isVisible
    shareButton.isVisible = false
    val savedDescriptionVisibility = descriptionTextView.isVisible
    descriptionTextView.isVisible = false
    root.measure(
        View.MeasureSpec.makeMeasureSpec(root.width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
    )

    val bitmap = getBitmapForItem()

    shareButton.isVisible = savedShareVisibility
    descriptionTextView.isVisible = savedDescriptionVisibility
    root.measure(
        View.MeasureSpec.makeMeasureSpec(root.width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
    )

    return bitmap
}

suspend fun ItemKeyFigureChartCardBinding.getBitmapForItemKeyFigureChartCardBinding(): Bitmap {
    val savedShareVisibility = shareButton.isVisible
    shareButton.isVisible = false
    root.measure(
        View.MeasureSpec.makeMeasureSpec(root.width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
    )

    val bitmap = getBitmapForItem()

    shareButton.isVisible = savedShareVisibility
    root.measure(
        View.MeasureSpec.makeMeasureSpec(root.width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
    )

    return bitmap
}