/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.model

abstract class RobertException(
    val errorCode: ErrorCode,
    override val message: String
) : Exception(message)

enum class ErrorCode {
    UNKNOWN,
    UNAUTHORIZED,
    NO_INTERNET,
    BACKEND,
    PROXIMITY_UNKNOWN,
    ROBERT_UNKNOWN,
    ROBERT_NO_EBID_FOR_EPOCH,
    ROBERT_NO_EBID,
    KEYSTORE_NO_KEY,
    KEYSTORE_DECRYPT
}

class UnknownException(message: String = "Unknown error occurred") :
    RobertException(ErrorCode.UNKNOWN, message)

class UnauthorizedException(message: String = "Not authorized to call this endpoint") :
    RobertException(ErrorCode.UNAUTHORIZED, message)

class BackendException(message: String = "An error occurs. Our team is working to fix it!") :
    RobertException(ErrorCode.BACKEND, message)

class NoInternetException(message: String = "No internet") :
    RobertException(ErrorCode.NO_INTERNET, message)

class ProximityException(val throwable: Throwable? = null,
    message: String = "An error occurs in BLE proximity") :
    RobertException(ErrorCode.PROXIMITY_UNKNOWN, message)

class NoSharedKeyException(message: String = "No shared key found") :
    RobertException(ErrorCode.KEYSTORE_NO_KEY, message)

class NoEphemeralBluetoothIdentifierFoundForEpoch(message: String = "No EphemeralBluetoothIdentifier found for the requested time") :
    RobertException(ErrorCode.ROBERT_NO_EBID_FOR_EPOCH, message)

class NoEphemeralBluetoothIdentifierFound(message: String? = null) :
    RobertException(ErrorCode.ROBERT_NO_EBID, message ?: "No EphemeralBluetoothIdentifier found")

class RobertUnknownException(message: String = "Unknown error occurred") :
    RobertException(ErrorCode.ROBERT_UNKNOWN, message)

class KeyDecryptionFailed(val throwable: Throwable? = null,
    message: String = throwable?.localizedMessage ?: "The stored key cannot be decrypt") :
    RobertException(ErrorCode.KEYSTORE_DECRYPT, message)