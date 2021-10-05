package com.lunabeestudio.stopcovid.extension

import android.icu.text.CompactDecimalFormat
import com.lunabeestudio.stopcovid.Constants
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow

fun Float.formatCompact(): String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        // Hack because the CompactDecimalFormat throws exception with negative values
        if (this <= 0) {
            "0"
        } else {
            CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT).apply {
                maximumSignificantDigits = Constants.Chart.SIGNIFICANT_DIGIT_MAX
            }.format(this.toDouble())
        }
    } else {
        this.formatCompactLowApi()
    }
}

fun Float.formatCompactLowApi(): String {
    val suffixChars = "kMGTPE"
    val formatter = DecimalFormat.getInstance(Locale.getDefault())

    return when {
        this < 1 -> {
            formatter.maximumFractionDigits = Constants.Chart.SIGNIFICANT_DIGIT_MAX - 1
            formatter.format(this)
        }
        this < 1000 -> {
            formatter.maximumFractionDigits = Constants.Chart.SIGNIFICANT_DIGIT_MAX - 1 - log10(this).toInt()
            formatter.format(this)
        }
        else -> {
            val exp = (ln(this) / ln(1000.0)).toInt()
            val logValue = this / 1000.0.pow(exp.toDouble())
            formatter.maximumFractionDigits = Constants.Chart.SIGNIFICANT_DIGIT_MAX - 1 - log10(logValue).toInt()
            formatter.format(logValue) + suffixChars[exp - 1]
        }
    }
}