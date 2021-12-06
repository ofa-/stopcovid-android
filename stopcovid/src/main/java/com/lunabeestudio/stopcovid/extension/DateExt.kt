package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import java.util.Calendar
import java.util.Date
import kotlin.time.Duration.Companion.days

fun Date.roundedTimeIntervalSince1900(interval: Long): Long {
    val timeInterval = this.time.unixTimeMsToNtpTimeS()
    return timeInterval + interval / 2 - (timeInterval + interval / 2) % interval
}

fun midnightDate(): Date {
    return midnightCalendar().time
}

private fun midnightCalendar(): Calendar =
    Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

fun Date.daysTo(toDate: Date): Float {
    return (toDate.time - time).toFloat() / 1.days.inWholeMilliseconds.toFloat()
}

fun Date.yearsOld(): Int {
    val today: Calendar = midnightCalendar()
    val birthdate: Calendar = Calendar.getInstance().apply {
        time = this@yearsOld
    }
    var diff = today[Calendar.YEAR] - birthdate[Calendar.YEAR]
    if (today[Calendar.DAY_OF_YEAR] > birthdate[Calendar.DAY_OF_YEAR]) {
        diff--
    }
    return diff
}

fun Date.future(): Boolean {
    return after(midnightDate())
}