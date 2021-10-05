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
import com.lunabeestudio.domain.model.WalletCertificateError
import com.lunabeestudio.robert.model.BackendException
import com.lunabeestudio.robert.model.ForbiddenException
import com.lunabeestudio.robert.model.NoInternetException
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.robert.model.UnauthorizedException
import com.lunabeestudio.robert.model.UnknownException
import com.lunabeestudio.stopcovid.model.WalletCertificateInvalidSignatureException
import com.lunabeestudio.stopcovid.model.WalletCertificateMalformedException
import com.lunabeestudio.stopcovid.model.WalletCertificateNoKeyException
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

internal fun Exception.remoteToRobertException(): RobertException = when (this) {
    is MalformedJsonException -> BackendException(httpCode = null)
    is SSLException -> BackendException(httpCode = null)
    is SocketTimeoutException,
    is IOException,
    -> NoInternetException()
    is HttpException -> {
        val httpCode = code()
        when (httpCode) {
            401 -> UnauthorizedException()
            403 -> ForbiddenException()
            else -> BackendException(httpCode = httpCode)
        }
    }
    else -> UnknownException()
}

internal fun Exception.walletCertificateError(): WalletCertificateError? {
    Timber.e(this)
    return when (this) {
        is WalletCertificateInvalidSignatureException -> WalletCertificateError.INVALID_CERTIFICATE_SIGNATURE
        is WalletCertificateMalformedException -> WalletCertificateError.MALFORMED_CERTIFICATE
        is WalletCertificateNoKeyException -> WalletCertificateError.INVALID_CERTIFICATE_SIGNATURE
        else -> null
    }
}