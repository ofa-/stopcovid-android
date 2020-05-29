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
    ErrorCode.UNAUTHORIZED -> listOfNotNull(strings["common.error.unauthorized"], message).joinToString("\n")
    ErrorCode.UNKNOWN -> listOfNotNull(strings["common.error.unknown"], message).joinToString("\n")
    ErrorCode.BACKEND -> strings["common.error.server"] ?: message
    ErrorCode.NEED_REGISTER -> strings["common.error.needRegister"] ?: message
    ErrorCode.PROXIMITY_UNKNOWN -> listOfNotNull(strings["common.error.proximityUnknown"], message).joinToString("\n")
    ErrorCode.ROBERT_UNKNOWN -> listOfNotNull(strings["common.error.robertUnknown"], message).joinToString("\n")
    ErrorCode.ROBERT_NO_EBID_FOR_EPOCH -> listOfNotNull(strings["common.error.robertNoEbidForEpoch"], message).joinToString("\n")
    ErrorCode.ROBERT_NO_EBID -> listOfNotNull(strings["common.error.robertNoEbid"], message).joinToString("\n")
    ErrorCode.KEYSTORE_NO_KEY -> listOfNotNull(strings["common.error.keystoreNoKey"], message).joinToString("\n")
    ErrorCode.DECRYPT_FAIL -> listOfNotNull(strings["common.error.decryptFail"], message).joinToString("\n")
    ErrorCode.BLE_ADVERTISER -> listOfNotNull(strings["common.error.bleAdvertiser"], message).joinToString(" ")
    ErrorCode.BLE_SCANNER -> listOfNotNull(strings["common.error.bleScanner"], message).joinToString(" ")
    ErrorCode.BLE_PROXIMITY_NOTIFICATION -> listOfNotNull(strings["common.error.proximityNotification"], message).joinToString(" ")
}