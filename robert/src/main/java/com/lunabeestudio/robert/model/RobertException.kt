/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.model

sealed class RobertException(
    val errorCode: ErrorCode,
    override val message: String,
) : Exception(message)

enum class ErrorCode {
    UNKNOWN,
    UNAUTHORIZED,
    FORBIDDEN,
    NO_INTERNET,
    BACKEND,
    PROXIMITY_UNKNOWN,
    ROBERT_UNKNOWN,
    ROBERT_INVALID_EBID_FOR_EPOCH,
    ROBERT_NO_EBID_FOR_EPOCH,
    ROBERT_NO_EBID,
    DECRYPT_FAIL,
    KEYSTORE_NO_KEY,
    BLE_ADVERTISER,
    BLE_SCANNER,
    BLE_GATT,
    BLE_PROXIMITY_NOTIFICATION,
    TIME_NOT_ALIGNED,
    REPORT_DELAY,
    SECRET_KEY_ALREADY_GENERATED,
    ROBERT_RESET_INACTIVITY,
    ROBERT_NOT_REGISTERED,
}

class UnknownException(message: String = "Unknown error occurred") :
    RobertException(ErrorCode.UNKNOWN, message)

class UnauthorizedException(message: String = "Not authorized to call this endpoint") :
    RobertException(ErrorCode.UNAUTHORIZED, message)

class ForbiddenException(message: String = "Forbidden to call this endpoint") :
    RobertException(ErrorCode.FORBIDDEN, message)

class BackendException(message: String = "An error occurs. Our team is working to fix it!", val httpCode: Int?) :
    RobertException(ErrorCode.BACKEND, message)

class NoInternetException(message: String = "No internet") :
    RobertException(ErrorCode.NO_INTERNET, message)

class ProximityException(
    val throwable: Throwable? = null,
    message: String = "An error occurs in BLE proximity",
) :
    RobertException(ErrorCode.PROXIMITY_UNKNOWN, message)

class ServerDecryptException(message: String = "Server data decrypt fail") :
    RobertException(ErrorCode.DECRYPT_FAIL, message)

class NoKeyException(message: String = "No key found") :
    RobertException(ErrorCode.KEYSTORE_NO_KEY, message)

class NoEphemeralBluetoothIdentifierFoundForEpoch(message: String = "No EphemeralBluetoothIdentifier found for the requested time") :
    RobertException(ErrorCode.ROBERT_NO_EBID_FOR_EPOCH, message)

class InvalidEphemeralBluetoothIdentifierForEpoch(message: String? = null) :
    RobertException(ErrorCode.ROBERT_INVALID_EBID_FOR_EPOCH, message ?: "EphemeralBluetoothIdentifier is not valid for the requested time")

class NoEphemeralBluetoothIdentifierFound(message: String? = null) :
    RobertException(ErrorCode.ROBERT_NO_EBID, message ?: "No EphemeralBluetoothIdentifier found")

class RobertUnknownException(message: String = "Unknown error occurred") :
    RobertException(ErrorCode.ROBERT_UNKNOWN, message)

class BLEAdvertiserException(message: String = "An error occurs in BLE advertiser") :
    RobertException(ErrorCode.BLE_ADVERTISER, message)

class BLEScannerException(message: String = "An error occurs in BLE scanner") :
    RobertException(ErrorCode.BLE_SCANNER, message)

class BLEGattException(message: String = "An error occurs in BLE gatt") :
    RobertException(ErrorCode.BLE_GATT, message)

class BLEProximityNotificationException(message: String = "An error occurs in BLE Proximity Notification") :
    RobertException(ErrorCode.BLE_PROXIMITY_NOTIFICATION, message)

class TimeNotAlignedException(message: String = "Phone time not aligned with server time") :
    RobertException(ErrorCode.TIME_NOT_ALIGNED, message)

class ReportDelayException(message: String = "You need to wait before you can use proximity again") :
    RobertException(ErrorCode.REPORT_DELAY, message)

class SecretKeyAlreadyGeneratedException(message: String = "Secret key was already generated but can't be found in the KeyStore") :
    RobertException(ErrorCode.SECRET_KEY_ALREADY_GENERATED, message)

class RequireRobertResetException(message: String = "Robert needs to be reset due to inactivity") :
    RobertException(ErrorCode.ROBERT_RESET_INACTIVITY, message)

class RequireRobertRegisterException(message: String = "Robert needs to register") :
    RobertException(ErrorCode.ROBERT_NOT_REGISTERED, message)