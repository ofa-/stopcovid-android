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

import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.stopcovid.model.BackendException
import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.KeyDecryptionFailed
import com.lunabeestudio.stopcovid.model.NoEphemeralBluetoothIdentifierFoundForEpoch
import com.lunabeestudio.stopcovid.model.NoInternetException
import com.lunabeestudio.stopcovid.model.NoSharedKeyException
import com.lunabeestudio.stopcovid.model.ProximityException
import com.lunabeestudio.stopcovid.model.RobertUnknownException
import com.lunabeestudio.stopcovid.model.UnauthorizedException
import com.lunabeestudio.stopcovid.model.UnknownException

fun RobertException?.toCovidException(): CovidException = when (this) {
    is com.lunabeestudio.robert.model.UnknownException -> UnknownException(message)
    is com.lunabeestudio.robert.model.UnauthorizedException -> UnauthorizedException(message)
    is com.lunabeestudio.robert.model.BackendException -> BackendException(message)
    is com.lunabeestudio.robert.model.NoInternetException -> NoInternetException(message)
    is com.lunabeestudio.robert.model.ProximityException -> ProximityException(throwable, message)
    is com.lunabeestudio.robert.model.NoSharedKeyException -> NoSharedKeyException(message)
    is com.lunabeestudio.robert.model.NoEphemeralBluetoothIdentifierFoundForEpoch -> NoEphemeralBluetoothIdentifierFoundForEpoch(message)
    is com.lunabeestudio.robert.model.RobertUnknownException -> RobertUnknownException(message)
    is com.lunabeestudio.robert.model.KeyDecryptionFailed -> KeyDecryptionFailed(throwable, message)
    else -> UnknownException()
}