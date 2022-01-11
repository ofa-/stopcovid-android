package com.lunabeestudio.stopcovid.extension

import android.content.Context
import android.icu.text.CompactDecimalFormat
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLocale
import java.text.DecimalFormat
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow

fun Float.formatCompact(context: Context): String {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        // Hack because the CompactDecimalFormat throws exception with negative values
        if (this <= 0) {
            "0"
        } else {
            CompactDecimalFormat.getInstance(context.getApplicationLocale(), CompactDecimalFormat.CompactStyle.SHORT).apply {
                maximumSignificantDigits = Constants.Chart.SIGNIFICANT_DIGIT_MAX
            }.format(this.toDouble())
        }
    } else {
        this.formatCompactLowApi(context)
    }
}

fun Float.formatCompactLowApi(context: Context): String {
    val suffixChars = "kMGTPE"
    val formatter = DecimalFormat.getInstance(context.getApplicationLocale())

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