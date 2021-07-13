package com.lunabeestudio.stopcovid.extension

import android.content.Context
import android.text.format.DateUtils
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun Duration.getRelativeDateTimeString(context: Context, nowString: String?): String? {
    val now = Duration.milliseconds(System.currentTimeMillis())

    return when {
        now - this <= Duration.minutes(1) -> nowString
        now - this <= Duration.days(1) -> DateUtils.getRelativeTimeSpanString(
            this.coerceAtMost(now - Duration.minutes(1)).inWholeMilliseconds,
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
fun Duration.getRelativeDateString(): String {
    return DateUtils.getRelativeTimeSpanString(
        this.inWholeMilliseconds,
        System.currentTimeMillis(),
        DateUtils.DAY_IN_MILLIS,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
    )
        .toString()
}

@OptIn(ExperimentalTime::class)
fun Duration.getRelativeDateShortString(context: Context): String {
    return DateUtils.formatDateTime(
        context,
        this.inWholeMilliseconds,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH
    )
}
