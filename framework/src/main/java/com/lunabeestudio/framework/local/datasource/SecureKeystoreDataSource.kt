/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.local.datasource

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.domain.model.DeviceParameterCorrection
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource

class SecureKeystoreDataSource(context: Context, private val cryptoManager: LocalCryptoManager) : LocalKeystoreDataSource {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)

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

    override var atRisk: Boolean?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_AT_RISK, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toBoolean()
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_AT_RISK, cryptoManager.encryptToString(value.toString()))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_AT_RISK).apply()
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

    override var checkStatusFrequency: Int?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_CHECK_STATUS_FREQUENCY, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toIntOrNull()
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

    override var randomStatusHour: Int?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_RANDOM_STATUS_HOUR, null)
            return if (encryptedText != null) {
                cryptoManager.decryptToString(encryptedText).toIntOrNull()
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

    companion object {
        private const val SHARED_PREF_NAME = "robert_prefs"
        private const val SHARED_PREF_KEY_KA = "shared.pref.ka"
        private const val SHARED_PREF_KEY_KEA = "shared.pref.kea"
        private const val SHARED_PREF_KEY_TIME_START = "shared.pref.time_start"
        private const val SHARED_PREF_KEY_AT_RISK = "shared.pref.at_risk"
        private const val SHARED_PREF_KEY_AT_RISK_LAST_REFRESH = "shared.pref.at_risk_last_refresh"
        private const val SHARED_PREF_KEY_AT_RISK_MIN_HOUR_CONTACT_NOTIF = "shared.pref.at_risk_min_hour_contact_notif"
        private const val SHARED_PREF_KEY_AT_RISK_MAX_HOUR_CONTACT_NOTIF = "shared.pref.at_risk_max_hour_contact_notif"
        private const val SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME = "shared.pref.last_exposure_timeframe"
        private const val SHARED_PREF_KEY_PROXIMITY_ACTIVE = "shared.pref.proximity_active"
        private const val SHARED_PREF_KEY_IS_SICK = "shared.pref.is_sick"
        private const val SHARED_PREF_KEY_CALIBRATION = "shared.pref.calibration"
        private const val SHARED_PREF_KEY_SERVICE_UUID = "shared.pref.service_uuid"
        private const val SHARED_PREF_KEY_CHARACTERISTIC_UUID = "shared.pref.characteristic_uuid"
        private const val SHARED_PREF_KEY_BACKGROUND_SERVICE_MANUFACTURER_DATA = "shared.pref.background_service_manufacturer_data"
        private const val SHARED_PREF_KEY_DATA_RETENTION_PERIOD = "shared.pref.data_retention_period"
        private const val SHARED_PREF_KEY_QUARANTINE_PERIOD = "shared.pref.quarantine_period"
        private const val SHARED_PREF_KEY_CHECK_STATUS_FREQUENCY = "shared.pref.check_status_frequency"
        private const val SHARED_PREF_KEY_RANDOM_STATUS_HOUR = "shared.pref.random_status_hour"
        private const val SHARED_PREF_KEY_PRE_SYMPTOMS_SPAN = "shared.pref.pre_symptoms_span"
        private const val SHARED_PREF_KEY_APP_AVAILABILITY = "shared.pref.app_availability"
    }
}
