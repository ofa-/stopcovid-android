package com.lunabeestudio.stopcovid.extension

import android.graphics.Color.parseColor
import android.graphics.drawable.GradientDrawable
import com.lunabeestudio.stopcovid.model.RisksUILevel

fun RisksUILevel.getGradientBackground(): GradientDrawable? {
    val colors = IntArray(2)
    colors[0] = parseColor(color.from)
    colors[1] = parseColor(color.to)
    return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
}
