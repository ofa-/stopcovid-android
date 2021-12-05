/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/17/6 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.OffsetTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun DateFormat.parseOrNull(source: String): Date? =
    try {
        parse(source)
    } catch (e: ParseException) {
        null
    }

// In France the default TimeZone is “Europe/Paris”
// which means that importing “2021-07-03" will result in a date with time set to midnight in GMT+2
// and that importing “2021-12-03” will result in a date with time set to midnight in GMT+1
// To avoid this tricky situation, this function force the timeZone to be the current GMT
fun yearMonthDayUsParser(): SimpleDateFormat {
    val offset = OffsetTime.ofInstant(midnightDate().toInstant(), ZoneId.of(TimeZone.getDefault().id)).offset
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT$offset")
    }
}
