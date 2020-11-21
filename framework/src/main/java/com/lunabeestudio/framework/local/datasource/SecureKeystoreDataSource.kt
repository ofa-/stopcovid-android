/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.local.datasource

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.domain.model.DeviceParameterCorrection
import com.lunabeestudio.domain.model.FormEntry
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource

class SecureKeystoreDataSource(context: Context, private val cryptoManager: LocalCryptoManager) : LocalKeystoreDataSource {

    private val gson: Gson = Gson()

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)

    override var shouldReloadBleSettings: Boolean?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toBoolean()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS).apply()
            }
        }

    override var kA: ByteArray?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_KA, null)
            return if (encryptedText != null) {
                cryptoManager.decrypt(encryptedText)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_KA, cryptoManager.encryptToString(value))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_KA).apply()
            }
        }

    override var kEA: ByteArray?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_KEA, null)
            return if (encryptedText != null) {
                cryptoManager.decrypt(encryptedText)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_KEA, cryptoManager.encryptToString(value))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_KEA).apply()
            }
        }

    override var timeStart: Long?
        get() = sharedPreferences.getLong(SHARED_PREF_KEY_TIME_START, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putLong(SHARED_PREF_KEY_TIME_START, value)
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_TIME_START).apply()
            }
        }

    override var atRiskLastRefresh: Long?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_AT_RISK_LAST_REFRESH, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toLongOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_AT_RISK_LAST_REFRESH, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_AT_RISK_LAST_REFRESH).apply()
            }
        }

    override var atRiskLastError: Long?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_AT_RISK_LAST_ERROR, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toLongOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_AT_RISK_LAST_ERROR, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_AT_RISK_LAST_ERROR).apply()
            }
        }

    override var atRiskMinHourContactNotif: Int?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_AT_RISK_MIN_HOUR_CONTACT_NOTIF, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toIntOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_AT_RISK_MIN_HOUR_CONTACT_NOTIF, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_AT_RISK_MIN_HOUR_CONTACT_NOTIF).apply()
            }
        }

    override var atRiskMaxHourContactNotif: Int?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_AT_RISK_MAX_HOUR_CONTACT_NOTIF, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toIntOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_AT_RISK_MAX_HOUR_CONTACT_NOTIF, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_AT_RISK_MAX_HOUR_CONTACT_NOTIF).apply()
            }
        }

    override var lastRiskReceivedDate: Long?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_LAST_RISK_RECEIVED_DATE, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toLongOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_LAST_RISK_RECEIVED_DATE, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_LAST_RISK_RECEIVED_DATE).apply()
            }
        }

    override var lastExposureTimeframe: Int?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toIntOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME).apply()
            }
        }

    override var proximityActive: Boolean?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_PROXIMITY_ACTIVE, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toBoolean()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_PROXIMITY_ACTIVE, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_PROXIMITY_ACTIVE).apply()
            }
        }

    override var isSick: Boolean?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_IS_SICK, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toBoolean()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_IS_SICK, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_IS_SICK).apply()
            }
        }

    override var calibration: List<DeviceParameterCorrection>?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_CALIBRATION, null)
            return if (encryptedText != null) {
                val typeToken = object : TypeToken<List<DeviceParameterCorrection>>() {}.type
                Gson().fromJson(cryptoManager.decryptToString(encryptedText), typeToken)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_CALIBRATION, cryptoManager.encryptToString(Gson().toJson(value)))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_CALIBRATION).apply()
            }
        }

    override var filteringConfig: String?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_FILTERING_CONFIG, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_FILTERING_CONFIG, cryptoManager.encryptToString(value))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_FILTERING_CONFIG).apply()
            }
        }

    override var filteringMode: String?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_FILTERING_MODE, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_FILTERING_MODE, cryptoManager.encryptToString(value))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_FILTERING_MODE).apply()
            }
        }

    override var serviceUUID: String?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_SERVICE_UUID, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_SERVICE_UUID, cryptoManager.encryptToString(value))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_SERVICE_UUID).apply()
            }
        }

    override var characteristicUUID: String?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_CHARACTERISTIC_UUID, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_CHARACTERISTIC_UUID, cryptoManager.encryptToString(value))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_CHARACTERISTIC_UUID).apply()
            }
        }

    override var backgroundServiceManufacturerData: String?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_BACKGROUND_SERVICE_MANUFACTURER_DATA, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_BACKGROUND_SERVICE_MANUFACTURER_DATA, cryptoManager.encryptToString(value))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_BACKGROUND_SERVICE_MANUFACTURER_DATA).apply()
            }
        }

    override var dataRetentionPeriod: Int?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_DATA_RETENTION_PERIOD, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toIntOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_DATA_RETENTION_PERIOD, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_DATA_RETENTION_PERIOD).apply()
            }
        }

    override var quarantinePeriod: Int?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_QUARANTINE_PERIOD, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toIntOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_QUARANTINE_PERIOD, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_QUARANTINE_PERIOD).apply()
            }
        }

    override var checkStatusFrequency: Float?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_CHECK_STATUS_FREQUENCY, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toFloatOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_CHECK_STATUS_FREQUENCY, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_CHECK_STATUS_FREQUENCY).apply()
            }
        }

    override var minStatusRetryDuraction: Float?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_MIN_STATUS_RETRY_DURATION, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toFloatOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_MIN_STATUS_RETRY_DURATION, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_MIN_STATUS_RETRY_DURATION).apply()
            }
        }

    override var randomStatusHour: Float?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_RANDOM_STATUS_HOUR, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toFloatOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_RANDOM_STATUS_HOUR, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_RANDOM_STATUS_HOUR).apply()
            }
        }

    override var preSymptomsSpan: Int?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_PRE_SYMPTOMS_SPAN, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toIntOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_PRE_SYMPTOMS_SPAN, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_PRE_SYMPTOMS_SPAN).apply()
            }
        }

    override var positiveSampleSpan: Int?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_POSITIVE_SAMPLE_SPAN, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toIntOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_POSITIVE_SAMPLE_SPAN, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_POSITIVE_SAMPLE_SPAN).apply()
            }
        }

    override var appAvailability: Boolean?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_APP_AVAILABILITY, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toBoolean()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_APP_AVAILABILITY, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_APP_AVAILABILITY).apply()
            }
        }

    override var displayDepartmentLevel: Boolean?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_DISPLAY_DEPARTMENT_LEVEL, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toBoolean()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_DISPLAY_DEPARTMENT_LEVEL, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_DISPLAY_DEPARTMENT_LEVEL).apply()
            }
        }

    override var apiVersion: String?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_API_VERSION, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_API_VERSION, cryptoManager.encryptToString(value))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_API_VERSION).apply()
            }
        }

    override var displayAttestation: Boolean?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_DISPLAY_ATTESTATION, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toBoolean()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_DISPLAY_ATTESTATION, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_DISPLAY_ATTESTATION).apply()
            }
        }

    override var qrCodeDeletionHours: Float?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_QR_CODE_DELETION_HOURS, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toFloatOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_QR_CODE_DELETION_HOURS, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_QR_CODE_DELETION_HOURS).apply()
            }
        }

    override var qrCodeExpiredHours: Float?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_QR_CODE_EXPIRED_HOURS, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toFloatOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_QR_CODE_EXPIRED_HOURS, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_QR_CODE_EXPIRED_HOURS).apply()
            }
        }

    override var qrCodeFormattedString: String?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING, cryptoManager.encryptToString(value))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING).apply()
            }
        }

    override var qrCodeFormattedStringDisplayed: String?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING_DISPLAYED, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING_DISPLAYED, cryptoManager.encryptToString(value))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING_DISPLAYED).apply()
            }
        }

    override var qrCodeFooterString: String?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_QR_CODE_FOOTER_STRING, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_QR_CODE_FOOTER_STRING, cryptoManager.encryptToString(value))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_QR_CODE_FOOTER_STRING).apply()
            }
        }

    override var proximityReactivationReminderHours: List<Int>?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_PROXIMITY_REACTIVATE_REMINDER_HOURS, null)
            return if (encryptedText != null) {
                Gson().fromJson(cryptoManager.decryptToString(encryptedText), object : TypeToken<List<Int>>() {}.type)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_PROXIMITY_REACTIVATE_REMINDER_HOURS, cryptoManager.encryptToString(Gson().toJson(value)))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_PROXIMITY_REACTIVATE_REMINDER_HOURS).apply()
            }
        }

    override var saveAttestationData: Boolean?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_SAVE_ATTESTATION_DATA, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toBoolean()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_SAVE_ATTESTATION_DATA, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_SAVE_ATTESTATION_DATA).apply()
            }
        }

    override var savedAttestationData: Map<String, FormEntry>?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_SAVED_ATTESTATION_DATA, null)
            return if (encryptedText != null) {
                gson.fromJson(cryptoManager.decryptToString(encryptedText), object : TypeToken<Map<String, FormEntry>>() {}.type)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_SAVED_ATTESTATION_DATA, cryptoManager.encryptToString(gson.toJson(value)))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_SAVED_ATTESTATION_DATA).apply()
            }
        }

    private var _attestationsLiveData: MutableLiveData<List<Map<String, FormEntry>>?> = MutableLiveData(attestations)
    override val attestationsLiveData: LiveData<List<Map<String, FormEntry>>?>
        get() = _attestationsLiveData
    override var attestations: List<Map<String, FormEntry>>?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_ATTESTATIONS, null)
            return if (encryptedText != null) {
                gson.fromJson(cryptoManager.decryptToString(encryptedText), object : TypeToken<List<Map<String, FormEntry>>>() {}.type)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_ATTESTATIONS, cryptoManager.encryptToString(gson.toJson(value)))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_ATTESTATIONS).apply()
            }
            _attestationsLiveData.postValue(value)
        }

    override var configVersion: Int?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_CONFIG_VERSION, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toIntOrNull()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_CONFIG_VERSION, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_CONFIG_VERSION).apply()
            }
        }

    override var reportDate: Long?
        get() = sharedPreferences.getLong(SHARED_PREF_KEY_REPORT_DATE, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putLong(SHARED_PREF_KEY_REPORT_DATE, value)
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_REPORT_DATE).apply()
            }
        }

    companion object {
        private const val SHARED_PREF_NAME = "robert_prefs"
        private const val SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS = "shared.pref.should_reload_ble_settings"
        private const val SHARED_PREF_KEY_KA = "shared.pref.ka"
        private const val SHARED_PREF_KEY_KEA = "shared.pref.kea"
        private const val SHARED_PREF_KEY_TIME_START = "shared.pref.time_start"
        private const val SHARED_PREF_KEY_AT_RISK_LAST_REFRESH = "shared.pref.at_risk_last_refresh"
        private const val SHARED_PREF_KEY_AT_RISK_LAST_ERROR = "shared.pref.at_risk_last_error"
        private const val SHARED_PREF_KEY_LAST_RISK_RECEIVED_DATE = "shared.pref.last_risk_received_date"
        private const val SHARED_PREF_KEY_AT_RISK_MIN_HOUR_CONTACT_NOTIF = "shared.pref.at_risk_min_hour_contact_notif"
        private const val SHARED_PREF_KEY_AT_RISK_MAX_HOUR_CONTACT_NOTIF = "shared.pref.at_risk_max_hour_contact_notif"
        private const val SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME = "shared.pref.last_exposure_timeframe"
        private const val SHARED_PREF_KEY_PROXIMITY_ACTIVE = "shared.pref.proximity_active"
        private const val SHARED_PREF_KEY_IS_SICK = "shared.pref.is_sick"
        private const val SHARED_PREF_KEY_CALIBRATION = "shared.pref.calibration"
        private const val SHARED_PREF_KEY_FILTERING_CONFIG = "shared.pref.filtering_config"
        private const val SHARED_PREF_KEY_FILTERING_MODE = "shared.pref.filtering_mode"
        private const val SHARED_PREF_KEY_SERVICE_UUID = "shared.pref.service_uuid"
        private const val SHARED_PREF_KEY_CHARACTERISTIC_UUID = "shared.pref.characteristic_uuid"
        private const val SHARED_PREF_KEY_BACKGROUND_SERVICE_MANUFACTURER_DATA = "shared.pref.background_service_manufacturer_data"
        private const val SHARED_PREF_KEY_DATA_RETENTION_PERIOD = "shared.pref.data_retention_period"
        private const val SHARED_PREF_KEY_QUARANTINE_PERIOD = "shared.pref.quarantine_period"
        private const val SHARED_PREF_KEY_CHECK_STATUS_FREQUENCY = "shared.pref.check_status_frequency"
        private const val SHARED_PREF_KEY_MIN_STATUS_RETRY_DURATION = "shared.pref.min_status_retry_duration"
        private const val SHARED_PREF_KEY_RANDOM_STATUS_HOUR = "shared.pref.random_status_hour"
        private const val SHARED_PREF_KEY_PRE_SYMPTOMS_SPAN = "shared.pref.pre_symptoms_span"
        private const val SHARED_PREF_KEY_POSITIVE_SAMPLE_SPAN = "shared.pref.positive_sample_span"
        private const val SHARED_PREF_KEY_APP_AVAILABILITY = "shared.pref.app_availability"
        private const val SHARED_PREF_KEY_API_VERSION = "shared.pref.api_version"
        private const val SHARED_PREF_KEY_DISPLAY_ATTESTATION = "shared.pref.display_attestation"
        private const val SHARED_PREF_KEY_QR_CODE_DELETION_HOURS = "shared.pref.qr_code_deletion_hours"
        private const val SHARED_PREF_KEY_QR_CODE_EXPIRED_HOURS = "shared.pref.qr_code_expired_hours"
        private const val SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING = "shared.pref.qr_code_formatted_string"
        private const val SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING_DISPLAYED = "shared.pref.qr_code_formatted_string_displayed"
        private const val SHARED_PREF_KEY_QR_CODE_FOOTER_STRING = "shared.pref.qr_code_footer_string"
        private const val SHARED_PREF_KEY_PROXIMITY_REACTIVATE_REMINDER_HOURS = "shared.pref.proximity_reactivate_reminder_hours"
        private const val SHARED_PREF_KEY_SAVE_ATTESTATION_DATA = "shared.pref.save_attestation_data"
        private const val SHARED_PREF_KEY_SAVED_ATTESTATION_DATA = "shared.pref.saved_attestation_data"
        private const val SHARED_PREF_KEY_ATTESTATIONS = "shared.pref.attestations"
        private const val SHARED_PREF_KEY_CONFIG_VERSION = "shared.pref.config_version"
        private const val SHARED_PREF_KEY_DISPLAY_DEPARTMENT_LEVEL = "shared.pref.display_department_level"
        private const val SHARED_PREF_KEY_REPORT_DATE = "shared.pref.report_date"
    }
}
