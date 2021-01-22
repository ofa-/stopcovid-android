package com.lunabeestudio.stopcovid.extension

import android.icu.text.CompactDecimalFormat
import java.util.Locale
import kotlin.math.floor

fun Float.formatCompact(): String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        // Hack because the CompactDecimalFormat throws exception with negative values
        if (this <= 0) {
            "0"
        } else {
            CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT).apply {
                maximumSignificantDigits = 3
            }.format(this.toDouble())
        }
    } else {
        floor(this.toDouble()).toString()
    }
}
