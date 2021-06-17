package com.lunabeestudio.stopcovid.extension

import android.content.Context
import android.text.format.DateUtils
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.days
import kotlin.time.milliseconds
import kotlin.time.minutes

@OptIn(ExperimentalTime::class)
fun Duration.getRelativeDateTimeString(context: Context, nowString: String?): String? {
    val now = System.currentTimeMillis().milliseconds

    return when {
        now - this <= 1.minutes -> nowString
        now - this <= Duration.days(1) -> DateUtils.getRelativeTimeSpanString(
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

@OptIn(ExperimentalTime::class)
fun Duration.getRelativeDateString(): String? {
    return DateUtils.getRelativeTimeSpanString(
        this.inWholeMilliseconds,
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
    )
        .toString()
}

@OptIn(ExperimentalTime::class)
fun Duration.getRelativeDateShortString(context: Context): String? {
    return DateUtils.formatDateTime(
        context,
        this.inWholeMilliseconds,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH
    )
}
