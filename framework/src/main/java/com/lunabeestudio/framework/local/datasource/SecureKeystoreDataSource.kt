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
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.domain.model.DeviceParameterCorrection
import com.lunabeestudio.domain.model.FormEntry
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import java.lang.reflect.Type

class SecureKeystoreDataSource(context: Context, private val cryptoManager: LocalCryptoManager) : LocalKeystoreDataSource {

    private var cache: HashMap<String, Any?> = hashMapOf()
    private val gson: Gson = Gson()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)

    override var shouldReloadBleSettings: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS, value)

    override var kA: ByteArray?
        get() = getEncryptedValue(SHARED_PREF_KEY_KA, ByteArray::class.java, useCache = false)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_KA, value, useCache = false)

    override var kEA: ByteArray?
        get() = getEncryptedValue(SHARED_PREF_KEY_KEA, ByteArray::class.java, useCache = false)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_KEA, value, useCache = false)

    override var timeStart: Long?
        get() = getAndMigrateOldUnencryptedLong(SHARED_PREF_KEY_TIME_START, SHARED_PREF_KEY_TIME_START_ENCRYPTED)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_TIME_START_ENCRYPTED, value)

    override var isWarningAtRisk: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_IS_WARNING_AT_RISK, Boolean::class.java, useCache = false)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_IS_WARNING_AT_RISK, value, useCache = false)

    override var atRiskLastRefresh: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_AT_RISK_LAST_REFRESH, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_AT_RISK_LAST_REFRESH, value)

    override var atRiskLastError: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_AT_RISK_LAST_ERROR, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_AT_RISK_LAST_ERROR, value)

    override var atRiskMinHourContactNotif: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_AT_RISK_MIN_HOUR_CONTACT_NOTIF, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_AT_RISK_MIN_HOUR_CONTACT_NOTIF, value)

    override var atRiskMaxHourContactNotif: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_AT_RISK_MAX_HOUR_CONTACT_NOTIF, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_AT_RISK_MAX_HOUR_CONTACT_NOTIF, value)

    override var lastRiskReceivedDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_LAST_RISK_RECEIVED_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_LAST_RISK_RECEIVED_DATE, value)

    override var lastExposureTimeframe: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME, Int::class.java, useCache = false)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME, value, useCache = false)

    override var proximityActive: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_PROXIMITY_ACTIVE, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_PROXIMITY_ACTIVE, value)

    override var isSick: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_IS_SICK, Boolean::class.java, useCache = false)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_IS_SICK, value, useCache = false)

    override var calibration: List<DeviceParameterCorrection>?
        get() = getEncryptedValue(SHARED_PREF_KEY_CALIBRATION, object : TypeToken<List<DeviceParameterCorrection>>() {}.type)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_CALIBRATION, value)

    override var filteringConfig: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_FILTERING_CONFIG, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_FILTERING_CONFIG, value)

    override var filteringMode: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_FILTERING_MODE, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_FILTERING_MODE, value)

    override var serviceUUID: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_SERVICE_UUID, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_SERVICE_UUID, value)

    override var characteristicUUID: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_CHARACTERISTIC_UUID, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_CHARACTERISTIC_UUID, value)

    override var backgroundServiceManufacturerData: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_BACKGROUND_SERVICE_MANUFACTURER_DATA, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_BACKGROUND_SERVICE_MANUFACTURER_DATA, value)

    override var dataRetentionPeriod: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_DATA_RETENTION_PERIOD, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_DATA_RETENTION_PERIOD, value)

    override var venuesRetentionPeriod: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_VENUES_RETENTION_PERIOD, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_VENUES_RETENTION_PERIOD, value)

    override var quarantinePeriod: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_QUARANTINE_PERIOD, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_QUARANTINE_PERIOD, value)

    override var checkStatusFrequency: Float?
        get() = getEncryptedValue(SHARED_PREF_KEY_CHECK_STATUS_FREQUENCY, Float::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_CHECK_STATUS_FREQUENCY, value)

    override var minStatusRetryDuraction: Float?
        get() = getEncryptedValue(SHARED_PREF_KEY_MIN_STATUS_RETRY_DURATION, Float::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_MIN_STATUS_RETRY_DURATION, value)

    override var randomStatusHour: Float?
        get() = getEncryptedValue(SHARED_PREF_KEY_RANDOM_STATUS_HOUR, Float::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_RANDOM_STATUS_HOUR, value)

    override var preSymptomsSpan: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_PRE_SYMPTOMS_SPAN, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_PRE_SYMPTOMS_SPAN, value)

    override var positiveSampleSpan: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_POSITIVE_SAMPLE_SPAN, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_POSITIVE_SAMPLE_SPAN, value)

    override var appAvailability: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_APP_AVAILABILITY, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_APP_AVAILABILITY, value)

    override var displayDepartmentLevel: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_DISPLAY_DEPARTMENT_LEVEL, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_DISPLAY_DEPARTMENT_LEVEL, value)

    override var apiVersion: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_API_VERSION, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_API_VERSION, value)

    override var warningApiVersion: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_WARNING_API_VERSION, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_WARNING_API_VERSION, value)

    override var displayAttestation: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_DISPLAY_ATTESTATION, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_DISPLAY_ATTESTATION, value)

    override var displayRecordVenues: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_DISPLAY_RECORD_VENUES, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_DISPLAY_RECORD_VENUES, value)

    override var displayPrivateEvent: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_DISPLAY_PRIVATE_EVENT, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_DISPLAY_PRIVATE_EVENT, value)

    override var qrCodeDeletionHours: Float?
        get() = getEncryptedValue(SHARED_PREF_KEY_QR_CODE_DELETION_HOURS, Float::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_QR_CODE_DELETION_HOURS, value)

    override var qrCodeExpiredHours: Float?
        get() = getEncryptedValue(SHARED_PREF_KEY_QR_CODE_EXPIRED_HOURS, Float::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_QR_CODE_EXPIRED_HOURS, value)

    override var qrCodeFormattedString: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING, value)

    override var qrCodeFormattedStringDisplayed: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING_DISPLAYED, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING_DISPLAYED, value)

    override var qrCodeFooterString: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_QR_CODE_FOOTER_STRING, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_QR_CODE_FOOTER_STRING, value)

    override var proximityReactivationReminderHours: List<Int>?
        get() = getEncryptedValue(SHARED_PREF_KEY_PROXIMITY_REACTIVATE_REMINDER_HOURS, object : TypeToken<List<Int>>() {}.type)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_PROXIMITY_REACTIVATE_REMINDER_HOURS, value)

    override var saveAttestationData: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_SAVE_ATTESTATION_DATA, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_SAVE_ATTESTATION_DATA, value)

    override var savedAttestationData: Map<String, FormEntry>?
        get() = getEncryptedValue(SHARED_PREF_KEY_SAVED_ATTESTATION_DATA, object : TypeToken<Map<String, FormEntry>>() {}.type)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_SAVED_ATTESTATION_DATA, value)

    private var _attestationsLiveData: MutableLiveData<List<Map<String, FormEntry>>?> = MutableLiveData(attestations)
    override val attestationsLiveData: LiveData<List<Map<String, FormEntry>>?>
        get() = _attestationsLiveData
    override var attestations: List<Map<String, FormEntry>>?
        get() = getEncryptedValue(SHARED_PREF_KEY_ATTESTATIONS, object : TypeToken<List<Map<String, FormEntry>>>() {}.type)
        set(value) {
            setEncryptedValue(SHARED_PREF_KEY_ATTESTATIONS, value)
            _attestationsLiveData.postValue(value)
        }

    override var configVersion: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_CONFIG_VERSION, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_CONFIG_VERSION, value)

    override var venuesTimestampRoundingInterval: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_VENUES_TIMESTAMP_ROUNDING_INTERVAL, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_VENUES_TIMESTAMP_ROUNDING_INTERVAL, value)

    override var reportDate: Long?
        get() = getAndMigrateOldUnencryptedLong(SHARED_PREF_KEY_REPORT_DATE, SHARED_PREF_KEY_REPORT_DATE_ENCRYPTED)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_DATE_ENCRYPTED, value)

    override var reportValidationToken: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_VALIDATION_TOKEN, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_VALIDATION_TOKEN, value)

    override var venuesQrCode: List<VenueQrCode>?
        get() = getEncryptedValue(SHARED_PREF_KEY_SAVE_DATA_VENUES_QR_CODE, object : TypeToken<List<VenueQrCode>>() {}.type)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_SAVE_DATA_VENUES_QR_CODE, value)

    override var privateEventVenueType: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_PRIVATE_EVENT_VENUE_TYPE, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_PRIVATE_EVENT_VENUE_TYPE, value)

    override var reportToSendTime: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_TO_SEND_TIME, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_TO_SEND_TIME, value)

    override var displayIsolation: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_DISPLAY_ISOLATION, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_DISPLAY_ISOLATION, value)

    override var reportPositiveTestDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_POSITIVE_TEST_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_POSITIVE_TEST_DATE, value)

    override var reportSymptomsStartDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_SYMPTOMS_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_SYMPTOMS_DATE, value)

    override var lastWarningReceivedDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_WARNING_RECEIVED_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_WARNING_RECEIVED_DATE, value)

    // Isolation vars
    override var isolationFormState: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_FORM_STATE, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_FORM_STATE, value)

    override var isolationLastContactDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_LAST_CONTACT_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_LAST_CONTACT_DATE, value)

    override var isolationIsKnownIndexAtHome: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_KNOWN_INDEX_AT_HOME, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_KNOWN_INDEX_AT_HOME, value)

    override var isolationKnowsIndexSymptomsEndDate: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_KNOWS_SYMPTOMS_END_DATE, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_KNOWS_SYMPTOMS_END_DATE, value)

    override var isolationIndexSymptomsEndDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_INDEX_SYMPTOMS_END_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_INDEX_SYMPTOMS_END_DATE, value)

    override var isolationLastFormValidationDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_LAST_FORM_VALIDATION_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_LAST_FORM_VALIDATION_DATE, value)

    override var isolationIsTestNegative: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_TEST_NEGATIVE, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_TEST_NEGATIVE, value)

    override var isolationPositiveTestingDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_POSITIVE_TESTING_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_POSITIVE_TESTING_DATE, value)

    override var isolationIsHavingSymptoms: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_HAVING_SYMPTOMS, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_HAVING_SYMPTOMS, value)

    override var isolationSymptomsStartDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_SYMPTOMS_START_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_SYMPTOMS_START_DATE, value)

    override var isolationIsStillHavingFever: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_STILL_HAVING_FEVER, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_STILL_HAVING_FEVER, value)

    override var isolationIsFeverReminderScheduled: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_FEVER_REMINDER_SCHEDULES, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_IS_FEVER_REMINDER_SCHEDULES, value)

    override var isolationDuration: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_ISOLATION_DURATION, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_ISOLATION_DURATION, value)

    override var postIsolationDuration: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_POST_ISOLATION_DURATION, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_POST_ISOLATION_DURATION, value)

    // GENERIC METHODS
    private fun <T> getEncryptedValue(key: String, type: Type, useCache: Boolean = true): T? {
        @Suppress("UNCHECKED_CAST")
        return (cache[key] as? T?).takeIf { useCache } ?: run {
            val encryptedText = sharedPreferences.getString(key, null)
            return if (encryptedText != null) {
                val result = kotlin.runCatching {
                    if (type == ByteArray::class.java) {
                        cryptoManager.decrypt(encryptedText) as? T
                    } else {
                        val decryptedString = cryptoManager.decryptToString(encryptedText)
                        gson.fromJson<T>(decryptedString, type)
                    }
                }.getOrNull()
                if (useCache) {
                    cache[key] = result
                }
                result
            } else {
                null
            }
        }
    }

    private fun setEncryptedValue(key: String, value: Any?, useCache: Boolean = true) {
        if (useCache) {
            cache[key] = when (value) {
                is List<*> -> value.toList()
                is Map<*, *> -> value.toMap()
                else -> value
            }
        }
        if (value != null) {
            sharedPreferences.edit()
                .putString(
                    key,
                    if (value is ByteArray) {
                        cryptoManager.encryptToString(value)
                    } else {
                        cryptoManager.encryptToString(gson.toJson(value))
                    }
                )
                .apply()
        } else {
            sharedPreferences.edit().remove(key).apply()
        }
    }

    private fun getAndMigrateOldUnencryptedLong(oldKey: String, newKey: String): Long? {
        return if (sharedPreferences.contains(oldKey)) {
            val prevLong = sharedPreferences.getLong(oldKey, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
            setEncryptedValue(newKey, prevLong)
            sharedPreferences.edit { remove(oldKey) }
            prevLong
        } else {
            getEncryptedValue(newKey, Long::class.java)
        }
    }

    companion object {
        private const val SHARED_PREF_NAME = "robert_prefs"
        private const val SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS = "shared.pref.should_reload_ble_settings"
        private const val SHARED_PREF_KEY_KA = "shared.pref.ka"
        private const val SHARED_PREF_KEY_KEA = "shared.pref.kea"
        private const val SHARED_PREF_KEY_TIME_START = "shared.pref.time_start"
        private const val SHARED_PREF_KEY_TIME_START_ENCRYPTED = "shared.pref.time_start_encrypted"
        private const val SHARED_PREF_KEY_IS_WARNING_AT_RISK = "shared.pref.is_warning_at_risk"
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
        private const val SHARED_PREF_KEY_VENUES_RETENTION_PERIOD = "shared.pref.venues_retention_period"
        private const val SHARED_PREF_KEY_QUARANTINE_PERIOD = "shared.pref.quarantine_period"
        private const val SHARED_PREF_KEY_CHECK_STATUS_FREQUENCY = "shared.pref.check_status_frequency"
        private const val SHARED_PREF_KEY_MIN_STATUS_RETRY_DURATION = "shared.pref.min_status_retry_duration"
        private const val SHARED_PREF_KEY_RANDOM_STATUS_HOUR = "shared.pref.random_status_hour"
        private const val SHARED_PREF_KEY_PRE_SYMPTOMS_SPAN = "shared.pref.pre_symptoms_span"
        private const val SHARED_PREF_KEY_POSITIVE_SAMPLE_SPAN = "shared.pref.positive_sample_span"
        private const val SHARED_PREF_KEY_APP_AVAILABILITY = "shared.pref.app_availability"
        private const val SHARED_PREF_KEY_API_VERSION = "shared.pref.api_version"
        private const val SHARED_PREF_KEY_WARNING_API_VERSION = "shared.pref.warning_api_version"
        private const val SHARED_PREF_KEY_DISPLAY_ATTESTATION = "shared.pref.display_attestation"
        private const val SHARED_PREF_KEY_DISPLAY_RECORD_VENUES = "shared.pref.display_record_venues"
        private const val SHARED_PREF_KEY_DISPLAY_PRIVATE_EVENT = "shared.pref.display_private_event"
        private const val SHARED_PREF_KEY_QR_CODE_DELETION_HOURS = "shared.pref.qr_code_deletion_hours"
        private const val SHARED_PREF_KEY_QR_CODE_EXPIRED_HOURS = "shared.pref.qr_code_expired_hours"
        private const val SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING = "shared.pref.qr_code_formatted_string"
        private const val SHARED_PREF_KEY_QR_CODE_FORMATTED_STRING_DISPLAYED = "shared.pref.qr_code_formatted_string_displayed"
        private const val SHARED_PREF_KEY_QR_CODE_FOOTER_STRING = "shared.pref.qr_code_footer_string"
        private const val SHARED_PREF_KEY_VENUES_TIMESTAMP_ROUNDING_INTERVAL = "shared.pref.venuesTimestampRoundingInterval"
        private const val SHARED_PREF_KEY_PROXIMITY_REACTIVATE_REMINDER_HOURS = "shared.pref.proximity_reactivate_reminder_hours"
        private const val SHARED_PREF_KEY_SAVE_ATTESTATION_DATA = "shared.pref.save_attestation_data"
        private const val SHARED_PREF_KEY_SAVED_ATTESTATION_DATA = "shared.pref.saved_attestation_data"
        private const val SHARED_PREF_KEY_ATTESTATIONS = "shared.pref.attestations"
        private const val SHARED_PREF_KEY_CONFIG_VERSION = "shared.pref.config_version"
        private const val SHARED_PREF_KEY_DISPLAY_DEPARTMENT_LEVEL = "shared.pref.display_department_level"
        private const val SHARED_PREF_KEY_REPORT_DATE = "shared.pref.report_date"
        private const val SHARED_PREF_KEY_REPORT_DATE_ENCRYPTED = "shared.pref.report_date_encrypted"
        private const val SHARED_PREF_KEY_SAVE_DATA_VENUES_QR_CODE = "shared.pref.venues_qr_code"
        private const val SHARED_PREF_KEY_REPORT_VALIDATION_TOKEN = "shared.pref.report_validation_token"
        private const val SHARED_PREF_KEY_PRIVATE_EVENT_VENUE_TYPE = "shared.pref.private_event_venue_type"
        private const val SHARED_PREF_KEY_REPORT_TO_SEND_TIME = "shared.pref.report_to_send_time"

        // Add on to ROBERT for isolation
        private const val SHARED_PREF_KEY_REPORT_SYMPTOMS_DATE = "shared.pref.reportSymptomsDate"
        private const val SHARED_PREF_KEY_REPORT_POSITIVE_TEST_DATE = "shared.pref.reportPositiveTestDate"
        private const val SHARED_PREF_KEY_WARNING_RECEIVED_DATE = "shared.pref.warningReceivedDate"

        // Isolation keys
        private const val SHARED_PREF_KEY_DISPLAY_ISOLATION = "shared.pref.displayIsolation"
        private const val SHARED_PREF_KEY_ISOLATION_FORM_STATE = "shared.pref.isolationFormState"
        private const val SHARED_PREF_KEY_ISOLATION_LAST_CONTACT_DATE = "shared.pref.isolationLastContactDate"
        private const val SHARED_PREF_KEY_ISOLATION_IS_KNOWN_INDEX_AT_HOME = "shared.pref.isolationIsKnownIndexAtHome"
        private const val SHARED_PREF_KEY_ISOLATION_KNOWS_SYMPTOMS_END_DATE = "shared.pref.isolationKnowsIndexSymptomsEndDate"
        private const val SHARED_PREF_KEY_ISOLATION_INDEX_SYMPTOMS_END_DATE = "shared.pref.isolationIndexSymptomsEndDate"
        private const val SHARED_PREF_KEY_ISOLATION_LAST_FORM_VALIDATION_DATE = "shared.pref.isolationLastFormValidationDate"
        private const val SHARED_PREF_KEY_ISOLATION_IS_TEST_NEGATIVE = "shared.pref.isolationIsTestNegative"
        private const val SHARED_PREF_KEY_ISOLATION_POSITIVE_TESTING_DATE = "shared.pref.isolationPositiveTestingDate"
        private const val SHARED_PREF_KEY_ISOLATION_IS_HAVING_SYMPTOMS = "shared.pref.isolationIsHavingSymptoms"
        private const val SHARED_PREF_KEY_ISOLATION_SYMPTOMS_START_DATE = "shared.pref.isolationSymptomsStartDate"
        private const val SHARED_PREF_KEY_ISOLATION_IS_STILL_HAVING_FEVER = "shared.pref.isolationIsStillHavingFever"
        private const val SHARED_PREF_KEY_ISOLATION_IS_FEVER_REMINDER_SCHEDULES = "shared.pref.isolationIsFeverReminderScheduled"
        private const val SHARED_PREF_KEY_ISOLATION_DURATION = "shared.pref.isolation_duration"
        private const val SHARED_PREF_KEY_POST_ISOLATION_DURATION = "shared.pref.post_isolation_duration"
    }
}
