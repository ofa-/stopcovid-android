package com.lunabeestudio.stopcovid.extension

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLocale
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

fun Duration.getRelativeDateTimeString(context: Context, localizedStrings: LocalizedStrings): String? {
    val appLocale = context.getApplicationLocale()
    val now = System.currentTimeMillis().milliseconds
    val daysSinceThis = ChronoUnit.DAYS.between(LocalDate.ofEpochDay(inWholeDays), LocalDate.now()).toInt()

    return when {
        now - this <= 1.minutes -> localizedStrings["common.justNow"]
        now - this <= 1.hours -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                RelativeDateTimeFormatter.getInstance(appLocale).format(
                    (now - this).inWholeMinutes.toDouble(),
                    RelativeDateTimeFormatter.Direction.LAST,
                    RelativeDateTimeFormatter.RelativeUnit.MINUTES,
                )
            } else {
                val timeString = SimpleDateFormat.getTimeInstance(DateFormat.SHORT, appLocale)
                    .format(Date(this.inWholeMilliseconds))
                "${localizedStrings["common.today"]}, $timeString"
            }
        }
        daysSinceThis <= 7 -> {
            val timeString = SimpleDateFormat.getTimeInstance(DateFormat.SHORT, appLocale)
                .format(Date(this.inWholeMilliseconds))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val dateTimeFormatter = RelativeDateTimeFormatter.getInstance(appLocale)
                val dateString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dateTimeFormatter.format(
                        daysSinceThis * -1.0,
                        RelativeDateTimeFormatter.RelativeDateTimeUnit.DAY,
                    ).titleCaseFirstChar()
                } else {
                    when {
                        daysSinceThis <= 1 -> getRelativeString(daysSinceThis, localizedStrings)
                        else -> {
                            dateTimeFormatter.format(
                                daysSinceThis.toDouble(),
                                RelativeDateTimeFormatter.Direction.LAST,
                                RelativeDateTimeFormatter.RelativeUnit.DAYS,
                            ).titleCaseFirstChar()
                        }
                    }
                }

                dateTimeFormatter.combineDateAndTime(dateString, timeString).fixQuoteInString()
            } else {
                val dateString = getRelativeString(daysSinceThis, localizedStrings)
                "$dateString, $timeString"
            }
        }
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val dateTimeFormatter = RelativeDateTimeFormatter.getInstance(appLocale)
                val dateString = SimpleDateFormat.getDateInstance(DateFormat.SHORT, appLocale)
                    .format(Date(this.inWholeMilliseconds))
                val timeString = SimpleDateFormat.getTimeInstance(DateFormat.SHORT, appLocale)
                    .format(Date(this.inWholeMilliseconds))

                dateTimeFormatter.combineDateAndTime(dateString, timeString).fixQuoteInString()
            } else {
                SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, appLocale)
                    .format(Date(this.inWholeMilliseconds))
            }
        }
    }
}

private fun getRelativeString(daysSince: Int, localizedStrings: LocalizedStrings): String? {
    return when (daysSince) {
        0 -> localizedStrings["common.today"]
        1 -> localizedStrings["common.yesterday"]
        else -> localizedStrings.stringsFormat("common.daysAgo", daysSince)
    }
}

fun Duration.getRelativeDateShortString(context: Context): String {
    return SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, context.getApplicationLocale()).format(Date(this.inWholeMilliseconds))
}