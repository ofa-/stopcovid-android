package com.lunabeestudio.stopcovid.extension

import java.util.Calendar

fun Long?.roundTimestampToStartOfDay(): Long? {
    return this?.let {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = it
        calendar.set(Calendar.HOUR, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }
}


