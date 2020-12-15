/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.util.MalformedJsonException
import com.lunabeestudio.robert.model.BackendException
import com.lunabeestudio.robert.model.ForbiddenException
import com.lunabeestudio.robert.model.NoInternetException
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.robert.model.UnauthorizedException
import com.lunabeestudio.robert.model.UnknownException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

internal fun Exception.remoteToRobertException(): RobertException = when (this) {
    is MalformedJsonException -> BackendException()
    is SSLException -> BackendException()
    is SocketTimeoutException,
    is IOException,
    -> NoInternetException()
    is HttpException -> {
        when (code()) {
            401 -> UnauthorizedException()
            403 -> ForbiddenException()
            else -> BackendException()
        }
    }
    else -> UnknownException()
}
