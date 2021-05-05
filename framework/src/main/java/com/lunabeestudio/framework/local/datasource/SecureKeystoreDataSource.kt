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
import com.lunabeestudio.domain.model.Attestation
import com.lunabeestudio.domain.model.Calibration
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.FormEntry
import com.lunabeestudio.domain.model.RawWalletCertificate
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.domain.model.AtRiskStatus
import java.lang.reflect.Type

class SecureKeystoreDataSource(context: Context, private val cryptoManager: LocalCryptoManager) : LocalKeystoreDataSource {

    private var cache: HashMap<String, Any?> = hashMapOf()
    private val gson: Gson = Gson()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)

    override var configuration: Configuration?
        get() = getValue(SHARED_PREF_KEY_CONFIGURATION, Configuration::class.java)
        set(value) = setValue(SHARED_PREF_KEY_CONFIGURATION, value)

    override var calibration: Calibration?
        get() = getValue(SHARED_PREF_KEY_CALIBRATION, Calibration::class.java)
        set(value) = setValue(SHARED_PREF_KEY_CALIBRATION, value)

    override var shouldReloadBleSettings: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS, value)

    override var isRegistered: Boolean
        get() {
            val value: Boolean? = getEncryptedValue(SHARED_PREF_KEY_IS_REGISTERED, Boolean::class.java)
            return if (value == null) {
                isRegistered = kA != null && kEA != null
                false
            } else {
                value
            }
        }
        set(value) = setEncryptedValue(SHARED_PREF_KEY_IS_REGISTERED, value)

    override var kA: ByteArray?
        get() = getEncryptedValue(SHARED_PREF_KEY_KA, ByteArray::class.java, useCache = false)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_KA, value, useCache = false)

    override var kEA: ByteArray?
        get() = getEncryptedValue(SHARED_PREF_KEY_KEA, ByteArray::class.java, useCache = false)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_KEA, value, useCache = false)

    override var timeStart: Long?
        get() = getAndMigrateOldUnencryptedLong(SHARED_PREF_KEY_TIME_START, SHARED_PREF_KEY_TIME_START_ENCRYPTED)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_TIME_START_ENCRYPTED, value)

    override var atRiskLastRefresh: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_AT_RISK_LAST_REFRESH, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_AT_RISK_LAST_REFRESH, value)

    override var atRiskLastError: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_AT_RISK_LAST_ERROR, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_AT_RISK_LAST_ERROR, value)

    override var atRiskStatus: AtRiskStatus?
        get() = getEncryptedValue(SHARED_PREF_KEY_AT_RISK_STATUS, AtRiskStatus::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_AT_RISK_STATUS, value)

    override var currentRobertAtRiskStatus: AtRiskStatus?
        get() = getEncryptedValue(SHARED_PREF_KEY_CURRENT_ROBERT_AT_RISK_STATUS, AtRiskStatus::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_CURRENT_ROBERT_AT_RISK_STATUS, value)

    override var currentWarningAtRiskStatus: AtRiskStatus?
        get() = getEncryptedValue(SHARED_PREF_KEY_CURRENT_WARNING_AT_RISK_STATUS, AtRiskStatus::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_CURRENT_WARNING_AT_RISK_STATUS, value)

    override var cleaLastStatusIteration: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_CLEA_LAST_STATUS_ITERATION, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_CLEA_LAST_STATUS_ITERATION, value)

    override var atRiskModelVersion: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_AT_RISK_MODEL_VERSION, Int::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_AT_RISK_MODEL_VERSION, value)

    override var deprecatedLastRiskReceivedDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_LAST_RISK_RECEIVED_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_LAST_RISK_RECEIVED_DATE, value)

    override var deprecatedLastExposureTimeframe: Int?
        get() = getEncryptedValue(SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME, Int::class.java, useCache = false)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME, value, useCache = false)

    override var proximityActive: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_PROXIMITY_ACTIVE, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_PROXIMITY_ACTIVE, value)

    override var isSick: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_IS_SICK, Boolean::class.java, useCache = false)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_IS_SICK, value, useCache = false)

    override var saveAttestationData: Boolean?
        get() = getEncryptedValue(SHARED_PREF_KEY_SAVE_ATTESTATION_DATA, Boolean::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_SAVE_ATTESTATION_DATA, value)

    override var savedAttestationData: Map<String, FormEntry>?
        get() = getEncryptedValue(SHARED_PREF_KEY_SAVED_ATTESTATION_DATA, object : TypeToken<Map<String, FormEntry>>() {}.type)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_SAVED_ATTESTATION_DATA, value)

    private var _attestationsLiveData: MutableLiveData<List<Attestation>?> = MutableLiveData(attestations)
    override val attestationsLiveData: LiveData<List<Attestation>?>
        get() = _attestationsLiveData

    override var attestations: List<Attestation>?
        get() = getEncryptedValue(SHARED_PREF_KEY_ATTESTATIONS, object : TypeToken<List<Attestation>>() {}.type)
        set(value) {
            setEncryptedValue(SHARED_PREF_KEY_ATTESTATIONS, value)
            _attestationsLiveData.postValue(value)
        }

    override var rawWalletCertificates: List<RawWalletCertificate>?
        get() = getEncryptedValue(SHARED_PREF_KEY_WALLET_CERTIFICATES, object : TypeToken<List<RawWalletCertificate>>() {}.type)
        set(value) {
            setEncryptedValue(SHARED_PREF_KEY_WALLET_CERTIFICATES, value)
            _rawWalletCertificatesLiveData.postValue(value)
        }
    private var _rawWalletCertificatesLiveData: MutableLiveData<List<RawWalletCertificate>?> = MutableLiveData(rawWalletCertificates)
    override val rawWalletCertificatesLiveData: LiveData<List<RawWalletCertificate>?>
        get() = _rawWalletCertificatesLiveData

    @Deprecated(message = "Old model", replaceWith = ReplaceWith("attestations"), level = DeprecationLevel.WARNING)
    override var deprecatedAttestations: List<Map<String, FormEntry>>?
        get() = getEncryptedValue(SHARED_PREF_KEY_DEPRECTATED_ATTESTATIONS, object : TypeToken<List<Map<String, FormEntry>>>() {}.type)
        set(value) {
            setEncryptedValue(SHARED_PREF_KEY_DEPRECTATED_ATTESTATIONS, value)
        }

    override var reportDate: Long?
        get() = getAndMigrateOldUnencryptedLong(SHARED_PREF_KEY_REPORT_DATE, SHARED_PREF_KEY_REPORT_DATE_ENCRYPTED)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_DATE_ENCRYPTED, value)

    override var reportValidationToken: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_VALIDATION_TOKEN, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_VALIDATION_TOKEN, value)

    override var venuesQrCode: List<VenueQrCode>?
        get() = getEncryptedValue(SHARED_PREF_KEY_SAVE_DATA_VENUES_QR_CODE, object : TypeToken<List<VenueQrCode>>() {}.type)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_SAVE_DATA_VENUES_QR_CODE, value)

    override var reportToSendStartTime: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_TO_SEND_START_TIME, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_TO_SEND_START_TIME, value)

    override var reportToSendEndTime: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_TO_SEND_END_TIME, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_TO_SEND_END_TIME, value)

    override var reportPositiveTestDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_POSITIVE_TEST_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_POSITIVE_TEST_DATE, value)

    override var reportSymptomsStartDate: Long?
        get() = getEncryptedValue(SHARED_PREF_KEY_REPORT_SYMPTOMS_DATE, Long::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_REPORT_SYMPTOMS_DATE, value)

    override var declarationToken: String?
        get() = getEncryptedValue(SHARED_PREF_KEY_DECLARATION_TOKEN, String::class.java)
        set(value) = setEncryptedValue(SHARED_PREF_KEY_DECLARATION_TOKEN, value)

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

    // GENERIC METHODS
    private fun <T> getEncryptedValue(key: String, type: Type, useCache: Boolean = true): T? {
        @Suppress("UNCHECKED_CAST")
        val cachedValue = if (useCache) {
            cache[key] as? T?
        } else {
            null
        }
        return cachedValue ?: run {
            val encryptedText = sharedPreferences.getString(key, null)
            return if (encryptedText != null) {
                val result = kotlin.runCatching {
                    if (type == ByteArray::class.java) {
                        @Suppress("UNCHECKED_CAST")
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

    private fun <T> getValue(@Suppress("SameParameterValue") key: String, type: Type): T? {
        @Suppress("UNCHECKED_CAST")
        return (cache[key] as? T?) ?: run {
            val text = sharedPreferences.getString(key, null)
            return if (text != null) {
                val result = kotlin.runCatching {
                    gson.fromJson<T>(text, type)
                }.getOrNull()
                cache[key] = result
                result
            } else {
                null
            }
        }
    }

    private fun setValue(@Suppress("SameParameterValue") key: String, value: Any?) {
        cache[key] = when (value) {
            is List<*> -> value.toList()
            is Map<*, *> -> value.toMap()
            else -> value
        }
        if (value != null) {
            sharedPreferences.edit()
                .putString(
                    key,
                    gson.toJson(value)
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
        private const val SHARED_PREF_KEY_CONFIGURATION = "shared.pref.configuration"
        private const val SHARED_PREF_KEY_CALIBRATION = "shared.pref.calibration"
        private const val SHARED_PREF_KEY_SHOULD_RELOAD_BLE_SETTINGS = "shared.pref.should_reload_ble_settings"
        private const val SHARED_PREF_KEY_IS_REGISTERED = "shared.pref.is_registered"
        private const val SHARED_PREF_KEY_KA = "shared.pref.ka"
        private const val SHARED_PREF_KEY_KEA = "shared.pref.kea"
        private const val SHARED_PREF_KEY_TIME_START = "shared.pref.time_start"
        private const val SHARED_PREF_KEY_TIME_START_ENCRYPTED = "shared.pref.time_start_encrypted"
        private const val SHARED_PREF_KEY_AT_RISK_LAST_REFRESH = "shared.pref.at_risk_last_refresh"
        private const val SHARED_PREF_KEY_AT_RISK_LAST_ERROR = "shared.pref.at_risk_last_error"
        private const val SHARED_PREF_KEY_AT_RISK_STATUS = "shared.pref.at_risk_status"
        private const val SHARED_PREF_KEY_CURRENT_ROBERT_AT_RISK_STATUS = "shared.pref.current_robert_at_risk_status"
        private const val SHARED_PREF_KEY_CURRENT_WARNING_AT_RISK_STATUS = "shared.pref.current_warning_at_risk_status"
        private const val SHARED_PREF_KEY_AT_RISK_MODEL_VERSION = "shared.pref.at_risk_model_version"
        private const val SHARED_PREF_KEY_LAST_RISK_RECEIVED_DATE = "shared.pref.last_risk_received_date"
        private const val SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME = "shared.pref.last_exposure_timeframe"
        private const val SHARED_PREF_KEY_PROXIMITY_ACTIVE = "shared.pref.proximity_active"
        private const val SHARED_PREF_KEY_IS_SICK = "shared.pref.is_sick"
        private const val SHARED_PREF_KEY_SAVE_ATTESTATION_DATA = "shared.pref.save_attestation_data"
        private const val SHARED_PREF_KEY_SAVED_ATTESTATION_DATA = "shared.pref.saved_attestation_data"
        private const val SHARED_PREF_KEY_ATTESTATIONS = "shared.pref.attestations_v2"
        private const val SHARED_PREF_KEY_DEPRECTATED_ATTESTATIONS = "shared.pref.attestations"
        private const val SHARED_PREF_KEY_REPORT_DATE = "shared.pref.report_date"
        private const val SHARED_PREF_KEY_REPORT_DATE_ENCRYPTED = "shared.pref.report_date_encrypted"
        private const val SHARED_PREF_KEY_SAVE_DATA_VENUES_QR_CODE = "shared.pref.venues_qr_code"
        private const val SHARED_PREF_KEY_REPORT_VALIDATION_TOKEN = "shared.pref.report_validation_token"
        private const val SHARED_PREF_KEY_REPORT_TO_SEND_START_TIME = "shared.pref.report_to_send_start_time"
        private const val SHARED_PREF_KEY_REPORT_TO_SEND_END_TIME = "shared.pref.report_to_send_end_time"
        private const val SHARED_PREF_KEY_DECLARATION_TOKEN = "shared.pref.declaration_token"
        private const val SHARED_PREF_KEY_WALLET_CERTIFICATES = "shared.pref.wallet_certificates"

        // Clea
        private const val SHARED_PREF_KEY_CLEA_LAST_STATUS_ITERATION = "shared.pref.clea_last_status_iteration"

        // Add on to ROBERT for isolation
        private const val SHARED_PREF_KEY_REPORT_SYMPTOMS_DATE = "shared.pref.reportSymptomsDate"
        private const val SHARED_PREF_KEY_REPORT_POSITIVE_TEST_DATE = "shared.pref.reportPositiveTestDate"

        // Isolation keys
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
    }
}
