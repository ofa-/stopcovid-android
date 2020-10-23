package com.lunabeestudio.stopcovid.extension

import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible

fun ImageView.setImageResourceOrHide(@DrawableRes resId: Int?) {
    resId?.let(this::setImageResource)
    isVisible = resId != null
}