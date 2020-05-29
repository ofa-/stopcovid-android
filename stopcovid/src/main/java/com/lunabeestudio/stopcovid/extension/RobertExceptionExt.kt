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

import com.lunabeestudio.robert.model.ErrorCode
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.stopcovid.model.BLEAdvertiserException
import com.lunabeestudio.stopcovid.model.BLEProximityNotificationException
import com.lunabeestudio.stopcovid.model.BLEScannerException
import com.lunabeestudio.stopcovid.model.BackendException
import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.NoEphemeralBluetoothIdentifierFound
import com.lunabeestudio.stopcovid.model.NoEphemeralBluetoothIdentifierFoundForEpoch
import com.lunabeestudio.stopcovid.model.NoInternetException
import com.lunabeestudio.stopcovid.model.NoKeyException
import com.lunabeestudio.stopcovid.model.ProximityException
import com.lunabeestudio.stopcovid.model.RobertUnknownException
import com.lunabeestudio.stopcovid.model.ServerDecryptException
import com.lunabeestudio.stopcovid.model.UnauthorizedException
import com.lunabeestudio.stopcovid.model.UnknownException

fun RobertException?.toCovidException(): CovidException = if (this != null) {
    when (this.errorCode) {
        ErrorCode.UNKNOWN -> UnknownException(message)
        ErrorCode.UNAUTHORIZED -> UnauthorizedException(message)
        ErrorCode.BACKEND -> BackendException(message)
        ErrorCode.NO_INTERNET -> NoInternetException(message)
        ErrorCode.PROXIMITY_UNKNOWN -> ProximityException((this as? com.lunabeestudio.robert.model.ProximityException)?.throwable, message)
        ErrorCode.KEYSTORE_NO_KEY -> NoKeyException(message)
        ErrorCode.ROBERT_NO_EBID_FOR_EPOCH -> NoEphemeralBluetoothIdentifierFoundForEpoch(message)
        ErrorCode.ROBERT_NO_EBID -> NoEphemeralBluetoothIdentifierFound(message)
        ErrorCode.ROBERT_UNKNOWN -> RobertUnknownException(message)
        ErrorCode.DECRYPT_FAIL -> ServerDecryptException(message)
        ErrorCode.BLE_ADVERTISER -> BLEAdvertiserException(message)
        ErrorCode.BLE_SCANNER -> BLEScannerException(message)
        ErrorCode.BLE_PROXIMITY_NOTIFICATION -> BLEProximityNotificationException(message)
    }
} else {
    UnknownException()
}