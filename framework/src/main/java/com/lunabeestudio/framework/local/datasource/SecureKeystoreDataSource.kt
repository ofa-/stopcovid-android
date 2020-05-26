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
import com.lunabeestudio.domain.model.ClientFilteringAlgorithmConfiguration
import com.lunabeestudio.framework.utils.CryptoManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource

class SecureKeystoreDataSource(context: Context, private val cryptoManager: CryptoManager) : LocalKeystoreDataSource {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)

    override var sharedKey: ByteArray?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_SHARED_KEY, null)
            return if (encryptedText != null) {
                cryptoManager.decrypt(encryptedText)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_SHARED_KEY, cryptoManager.encryptToString(value))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_SHARED_KEY).apply()
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

    override var filteringInfo: List<ClientFilteringAlgorithmConfiguration>?
        get() {
            val encryptedText = sharedPreferences.getString(SHARED_PREF_KEY_FILTERING_INFO, null)
            return if (encryptedText != null) {
                val typeToken = object : TypeToken<List<ClientFilteringAlgorithmConfiguration>>() {}.type
                Gson().fromJson(cryptoManager.decryptToString(encryptedText), typeToken)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                sharedPreferences.edit()
                    .putString(SHARED_PREF_KEY_FILTERING_INFO, cryptoManager.encryptToString(Gson().toJson(value)))
                    .apply()
            } else {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_FILTERING_INFO).apply()
            }
        }

    companion object {
        private const val SHARED_PREF_NAME = "robert_prefs"
        private const val SHARED_PREF_KEY_SHARED_KEY = "shared.pref.shared_key"
        private const val SHARED_PREF_KEY_TIME_START = "shared.pref.time_start"
        private const val SHARED_PREF_KEY_AT_RISK = "shared.pref.at_risk"
        private const val SHARED_PREF_KEY_LAST_EXPOSURE_TIMEFRAME = "shared.pref.last_exposure_timeframe"
        private const val SHARED_PREF_KEY_PROXIMITY_ACTIVE = "shared.pref.proximity_active"
        private const val SHARED_PREF_KEY_IS_SICK = "shared.pref.is_sick"
        private const val SHARED_PREF_KEY_FILTERING_INFO = "shared.pref.filtering_info"
    }
}
