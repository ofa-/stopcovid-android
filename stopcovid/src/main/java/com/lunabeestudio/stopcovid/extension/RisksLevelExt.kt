package com.lunabeestudio.stopcovid.extension

import android.graphics.Color.parseColor
import android.graphics.drawable.GradientDrawable
import android.view.View
import com.lunabeestudio.stopcovid.model.LinkType
import com.lunabeestudio.stopcovid.model.RisksUILevel
import com.lunabeestudio.stopcovid.model.RisksUILevelSectionLink

fun RisksUILevel.getGradientBackground(): GradientDrawable? {
    val colors = IntArray(2)
    colors[0] = parseColor(color.from)
    colors[1] = parseColor(color.to)
    return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
}


