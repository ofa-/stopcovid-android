/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.model

abstract class CovidException(val errorCode: ErrorCode, override val message: String) : Exception(message)

enum class ErrorCode {
    UNKNOWN,
    UNAUTHORIZED,
    NO_INTERNET,
    BACKEND,
    NEED_REGISTER,
    PROXIMITY_UNKNOWN,
    ROBERT_UNKNOWN,
    ROBERT_NO_EBID_FOR_EPOCH,
    ROBERT_NO_EBID,
    DECRYPT_FAIL,
    KEYSTORE_NO_KEY,
    BLE_ADVERTISER,
    BLE_SCANNER,
    BLE_PROXIMITY_NOTIFICATION
}

class UnknownException(message: String = "Unknown error occurred") :
    CovidException(ErrorCode.UNKNOWN, message)

class UnauthorizedException(message: String = "Not authorized to call this endpoint") :
    CovidException(ErrorCode.UNAUTHORIZED, message)

class BackendException(message: String = "An error occurs. Our team is working to fix it!") :
    CovidException(ErrorCode.BACKEND, message)

class NoInternetException(message: String = "No internet") :
    CovidException(ErrorCode.NO_INTERNET, message)

class NeedRegisterException(message: String = "You need to be registered to execute this action") :
    CovidException(ErrorCode.NEED_REGISTER, message)

class ProximityException(val throwable: Throwable? = null,
    message: String = throwable?.localizedMessage ?: "An error occurs in BLE proximity") :
    CovidException(ErrorCode.PROXIMITY_UNKNOWN, message)

class ServerDecryptException(message: String = "Server data decrypt fail") :
    CovidException(ErrorCode.DECRYPT_FAIL, message)

class NoKeyException(message: String = "No key found") :
    CovidException(ErrorCode.KEYSTORE_NO_KEY, message)

class NoEphemeralBluetoothIdentifierFoundForEpoch(message: String = "No EphemeralBluetoothIdentifier found the requested time") :
    CovidException(ErrorCode.ROBERT_NO_EBID_FOR_EPOCH, message)

class NoEphemeralBluetoothIdentifierFound(message: String? = null) :
    CovidException(ErrorCode.ROBERT_NO_EBID, message ?: "No EphemeralBluetoothIdentifier found")

class RobertUnknownException(message: String = "Unknown error occurred") :
    CovidException(ErrorCode.ROBERT_UNKNOWN, message)

class BLEAdvertiserException(message: String = "An error occurs in BLE advertiser") :
    CovidException(ErrorCode.BLE_ADVERTISER, message)

class BLEScannerException(message: String = "An error occurs in BLE scanner") :
    CovidException(ErrorCode.BLE_SCANNER, message)

class BLEProximityNotificationException(message: String = "An error occurs in BLE proximity notification") :
    CovidException(ErrorCode.BLE_PROXIMITY_NOTIFICATION, message)