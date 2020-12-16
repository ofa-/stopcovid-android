package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import java.util.Date

fun Date.roundedTimeIntervalSince1900(interval: Long): Long {
    val timeInterval = this.time.unixTimeMsToNtpTimeS()
    return timeInterval + interval / 2 - (timeInterval + interval / 2) % interval
}
