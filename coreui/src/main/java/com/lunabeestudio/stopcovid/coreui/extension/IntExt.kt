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
import android.content.res.ColorStateList
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.Dimension

/**
 * Return the color corresponding to theme of android from the receiver id.
 *
 * @param context context where the theme is.
 * @return A color Int of the color.
 */
@ColorInt
fun @receiver:AttrRes Int.fetchSystemColor(context: Context): Int {
    val typedValue = TypedValue()
    val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(this))
    val color = a.getColor(0, 0)

    a.recycle()

    return color
}

/**
 * Return the state aware colors corresponding to theme of android from the receiver id.
 *
 * @param context context where the theme is.
 * @return The ColorStateList.
 */
fun @receiver:AttrRes Int.fetchSystemColorStateList(context: Context): ColorStateList {
    val typedValue = TypedValue()
    val a = context.obtainStyledAttributes(typedValue.data, intArrayOf(this))
    val colorStateList: ColorStateList = a.getColorStateList(0)!!
    a.recycle()
    return colorStateList
}

/**
 * Return the dimension corresponding to the current theme of the receiver id.
 *
 * @param context context where the theme is.
 * @return Resource dimension value multiplied by the appropriate metric.
 */
@Dimension
fun @receiver:DimenRes Int.toDimensSize(context: Context): Float = context.resources.getDimension(this)