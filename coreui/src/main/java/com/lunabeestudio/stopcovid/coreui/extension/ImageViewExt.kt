/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.extension

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import java.io.File

fun ImageView.setImageResourceOrHide(@DrawableRes resId: Int?) {
    resId?.let(this::setImageResource)
    isVisible = resId != null
}

fun ImageView.setImageFileIfValid(file: File): Boolean {
    val fileDrawable = Drawable.createFromPath(file.path)
    return if (fileDrawable != null) {
        setImageDrawable(fileDrawable)
        true
    } else {
        false
    }
}