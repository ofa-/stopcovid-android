package com.lunabeestudio.stopcovid.extension

import android.graphics.Color
import androidx.core.graphics.ColorUtils

fun Int.brighterColor(): Int {
    return ColorUtils.blendARGB(this, Color.WHITE, 0.4f)
}
