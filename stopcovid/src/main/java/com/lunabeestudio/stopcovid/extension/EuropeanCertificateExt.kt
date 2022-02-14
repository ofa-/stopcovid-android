/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/01/12 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import java.text.DateFormat
import java.util.Date

fun EuropeanCertificate.smartWalletProfileId(): String {
    return ((firstName ?: name).orEmpty() + greenCertificate.dateOfBirth).uppercase()
}

fun EuropeanCertificate.multipassProfileId(): String {
    return greenCertificate.person.standardisedGivenName.orEmpty().trimEnd('<') +
        greenCertificate.person.standardisedFamilyName.trimEnd('<') +
        greenCertificate.dateOfBirth
}

// Convert date from it's timeZone to the current timeZone at midnight
// For example, if done in January, Sat Aug 28 00:43:07 GMT+02:00 2021 will give 2021-08-28
// And then it will be converted to Sat Aug 28 01:00:00 GMT+02:00 2021
// if dateFormatCurrentTime only was used it would have given Fri Aug 27 01:00:00 GMT+02:00 2021
// if dateFormatLocalTime only was used it would have given Sat Aug 28 00:00:00 GMT+02:00 2021
// As we compare multiple date at midnight shifted with days, it's important to always have only one timeZone as a reference
fun Date.midnightInCurrentTimeZone(dateFormatLocalTime: DateFormat, dateFormatCurrentTime: DateFormat): Date? {
    return dateFormatCurrentTime.parse(dateFormatLocalTime.format(this))
}
