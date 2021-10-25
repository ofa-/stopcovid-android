/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

abstract class CovidException(
    val errorCode: ErrorCode,
    override val message: String,
    override val cause: Throwable?,
) : Exception(message, cause)

enum class ErrorCode {
    UNKNOWN,
    UNAUTHORIZED,
    FORBIDDEN,
    NO_INTERNET,
    BACKEND,
    NEED_REGISTER,
    PROXIMITY_UNKNOWN,
    ROBERT_UNKNOWN,
    ROBERT_INVALID_EBID_FOR_EPOCH,
    ROBERT_NO_EBID_FOR_EPOCH,
    ROBERT_NO_EBID,
    DECRYPT_FAIL,
    KEYSTORE_NO_KEY,
    BLE_ADVERTISER,
    BLE_SCANNER,
    BLE_PROXIMITY_NOTIFICATION,
    BLE_GATT,
    TIME_NOT_ALIGNED,
    REPORT_DELAY,
    SECRET_KEY_ALREADY_GENERATED,
    WALLET_CERTIFICATE_MALFORMED,
    WALLET_CERTIFICATE_INVALID_SIGNATURE,
    WALLET_CERTIFICATE_UNKNOWN_ERROR,
    VENUE_INVALID_FORMAT_EXCEPTION,
    VENUE_EXPIRED_EXCEPTION,
    KEY_FIGURES_NOT_AVAILABLE,
    ACTIVITY_PASS_NOT_GENERATED,
}

class UnknownException(message: String = "Unknown error occurred", cause: Throwable? = null) :
    CovidException(ErrorCode.UNKNOWN, message, cause)

class UnauthorizedException(message: String = "Not authorized to call this endpoint", cause: Throwable? = null) :
    CovidException(ErrorCode.UNAUTHORIZED, message, cause)

class ForbiddenException(message: String = "Forbidden to call this endpoint", cause: Throwable? = null) :
    CovidException(ErrorCode.FORBIDDEN, message, cause)

class BackendException(message: String = "An error occurs. Our team is working to fix it!", cause: Throwable? = null) :
    CovidException(ErrorCode.BACKEND, message, cause)

class NoInternetException(message: String = "No internet", cause: Throwable? = null) :
    CovidException(ErrorCode.NO_INTERNET, message, cause)

class NeedRegisterException(message: String = "You need to be registered to execute this action", cause: Throwable? = null) :
    CovidException(ErrorCode.NEED_REGISTER, message, cause)

class ProximityException(
    val throwable: Throwable? = null,
    message: String = throwable?.localizedMessage ?: "An error occurs in BLE proximity",
    cause: Throwable? = null
) :
    CovidException(ErrorCode.PROXIMITY_UNKNOWN, message, cause)

class ServerDecryptException(message: String = "Server data decrypt fail", cause: Throwable? = null) :
    CovidException(ErrorCode.DECRYPT_FAIL, message, cause)

class NoKeyException(message: String = "No key found", cause: Throwable? = null) :
    CovidException(ErrorCode.KEYSTORE_NO_KEY, message, cause)

class NoEphemeralBluetoothIdentifierFoundForEpoch(
    message: String = "No EphemeralBluetoothIdentifier found the requested time",
    cause: Throwable? = null
) :
    CovidException(ErrorCode.ROBERT_NO_EBID_FOR_EPOCH, message, cause)

class InvalidEphemeralBluetoothIdentifierForEpoch(message: String? = null, cause: Throwable? = null) :
    CovidException(
        ErrorCode.ROBERT_INVALID_EBID_FOR_EPOCH,
        message ?: "EphemeralBluetoothIdentifier is not valid for the requested time",
        cause
    )

class NoEphemeralBluetoothIdentifierFound(message: String? = null, cause: Throwable? = null) :
    CovidException(ErrorCode.ROBERT_NO_EBID, message ?: "No EphemeralBluetoothIdentifier found", cause)

class RobertUnknownException(message: String = "Unknown error occurred", cause: Throwable? = null) :
    CovidException(ErrorCode.ROBERT_UNKNOWN, message, cause)

class BLEAdvertiserException(message: String = "An error occurs in BLE advertiser", cause: Throwable? = null) :
    CovidException(ErrorCode.BLE_ADVERTISER, message, cause)

class BLEScannerException(message: String = "An error occurs in BLE scanner", cause: Throwable? = null) :
    CovidException(ErrorCode.BLE_SCANNER, message, cause)

class BLEGattException(message: String = "An error occurs in BLE GATT", cause: Throwable? = null) :
    CovidException(ErrorCode.BLE_GATT, message, cause)

class BLEProximityNotificationException(message: String = "An error occurs in BLE Proximity Notification", cause: Throwable? = null) :
    CovidException(ErrorCode.BLE_PROXIMITY_NOTIFICATION, message, cause)

class TimeNotAlignedException(message: String = "Phone time not aligned with server time", cause: Throwable? = null) :
    CovidException(ErrorCode.TIME_NOT_ALIGNED, message, cause)

class ReportDelayException(message: String = "You need to wait before you can use proximity again", cause: Throwable? = null) :
    CovidException(ErrorCode.REPORT_DELAY, message, cause)

class SecretKeyAlreadyGeneratedException(
    message: String = "Secret key was already generated but can't be found in the KeyStore",
    cause: Throwable? = null
) :
    CovidException(ErrorCode.SECRET_KEY_ALREADY_GENERATED, message, cause)

class WalletCertificateMalformedException(message: String = "Invalid certificate code", cause: Throwable? = null) :
    CovidException(ErrorCode.WALLET_CERTIFICATE_MALFORMED, message, cause)

class WalletCertificateInvalidSignatureException(message: String = "Invalid certificate signature", cause: Throwable? = null) :
    CovidException(ErrorCode.WALLET_CERTIFICATE_INVALID_SIGNATURE, message, cause)

class VenueInvalidFormatException(message: String = "Invalid venue format", cause: Throwable? = null) :
    CovidException(ErrorCode.VENUE_INVALID_FORMAT_EXCEPTION, message, cause)

class VenueExpiredException(message: String = "Venue expired", cause: Throwable? = null) :
    CovidException(ErrorCode.VENUE_EXPIRED_EXCEPTION, message, cause)

class WalletCertificateNoKeyException(message: String = "No key to verify the certificate authenticity", cause: Throwable? = null) :
    CovidException(ErrorCode.WALLET_CERTIFICATE_UNKNOWN_ERROR, message, cause)

class KeyFiguresNotAvailableException(message: String = "Failed to load key figures", cause: Throwable? = null) :
    CovidException(ErrorCode.KEY_FIGURES_NOT_AVAILABLE, message, cause)

class GenerateActivityPassException(message: String = "Failed to generate activity pass", cause: Throwable? = null) :
    CovidException(ErrorCode.ACTIVITY_PASS_NOT_GENERATED, message, cause)