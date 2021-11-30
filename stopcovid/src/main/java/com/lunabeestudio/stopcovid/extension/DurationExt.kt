package com.lunabeestudio.stopcovid.extension

import android.content.Context
import android.text.format.DateUtils
import java.util.Calendar
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

fun Duration.getRelativeDateTimeString(context: Context, nowString: String?): String? {
    val now = System.currentTimeMillis().milliseconds

    return when {
        now - this <= 1.minutes -> nowString
        now - this <= 1.days -> DateUtils.getRelativeTimeSpanString(
            this.coerceAtMost(now - 1.minutes).inWholeMilliseconds,
            now.inWholeMilliseconds,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH,
        )
            .toString()
            .fixQuoteInString()
        else -> DateUtils.getRelativeDateTimeString(
            context,
            this.inWholeMilliseconds,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH,
        )
            .toString()
            .fixQuoteInString()
    }
}

fun Duration.getRelativeDateShortString(context: Context): String {
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)

    val dataDateMs = this@getRelativeDateShortString.inWholeMilliseconds
    calendar.timeInMillis = dataDateMs
    val dataYear = calendar.get(Calendar.YEAR)

    val formatYear = if (currentYear == dataYear) {
        DateUtils.FORMAT_NO_YEAR
    } else {
        DateUtils.FORMAT_SHOW_YEAR
    }

    return DateUtils.formatDateTime(
        context,
        dataDateMs,
        DateUtils.FORMAT_SHOW_DATE or formatYear or DateUtils.FORMAT_ABBREV_MONTH
    )
}
