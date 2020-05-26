/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.ErrorCode

fun CovidException.getString(strings: Map<String, String>): String = when (this.errorCode) {
    ErrorCode.NO_INTERNET -> strings["common.error.internet"] ?: message
    ErrorCode.UNAUTHORIZED -> message
    ErrorCode.UNKNOWN -> message
    ErrorCode.BACKEND -> strings["common.error.server"] ?: message
    ErrorCode.NEED_REGISTER -> strings["common.error.needRegister"] ?: message
    ErrorCode.PROXIMITY_UNKNOWN -> message
    ErrorCode.ROBERT_UNKNOWN -> message
    ErrorCode.ROBERT_NO_EBID -> message
    ErrorCode.KEYSTORE_NO_KEY -> message
    ErrorCode.KEYSTORE_DECRYPT -> message
}