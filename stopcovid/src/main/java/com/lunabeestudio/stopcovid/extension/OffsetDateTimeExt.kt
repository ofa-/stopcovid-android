/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/30/6 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import timber.log.Timber
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

fun parseToOffsetDateTimeOrNull(text: CharSequence): OffsetDateTime? {
    return try {
        OffsetDateTime.parse(text)
    } catch (e: DateTimeParseException) {
        Timber.e(e)
        null
    }
}