/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.content.SharedPreferences
import androidx.core.content.edit
import com.lunabeestudio.stopcovid.Constants
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

val SharedPreferences.isOnBoardingDone: Boolean
    get() = getBoolean(Constants.SharedPrefs.ON_BOARDING_DONE, false)

var SharedPreferences.lastInfoCenterRefresh: Long
    get() = getLong(Constants.SharedPrefs.LAST_INFO_CENTER_REFRESH, 0L)
    set(value) = edit { putLong(Constants.SharedPrefs.LAST_INFO_CENTER_REFRESH, value) }

var SharedPreferences.lastInfoCenterFetch: Duration
    get() = getLong(Constants.SharedPrefs.LAST_INFO_CENTER_FETCH, 0L).milliseconds
    set(value) = edit { putLong(Constants.SharedPrefs.LAST_INFO_CENTER_FETCH, value.inWholeMilliseconds) }

var SharedPreferences.areInfoNotificationsEnabled: Boolean
    get() = getBoolean(Constants.SharedPrefs.ARE_INFO_NOTIFICATIONS_ENABLED, true)
    set(value) = edit { putBoolean(Constants.SharedPrefs.ARE_INFO_NOTIFICATIONS_ENABLED, value) }

var SharedPreferences.chosenPostalCode: String?
    get() = getString(Constants.SharedPrefs.CHOSEN_POSTAL_CODE, null)
    set(value) = edit { putString(Constants.SharedPrefs.CHOSEN_POSTAL_CODE, value) }

val SharedPreferences.hasChosenPostalCode: Boolean
    get() = chosenPostalCode != null

var SharedPreferences.lastVersionCode: Int
    get() = getInt(Constants.SharedPrefs.LAST_VERSION_CODE, 0)
    set(value) = edit { putInt(Constants.SharedPrefs.LAST_VERSION_CODE, value) }

var SharedPreferences.lastVersionName: String?
    get() = getString(Constants.SharedPrefs.LAST_VERSION_NAME, null)
    set(value) = edit { putString(Constants.SharedPrefs.LAST_VERSION_NAME, value) }

var SharedPreferences.venuesFeaturedWasActivatedAtLeastOneTime: Boolean
    get() = getBoolean(Constants.SharedPrefs.VENUES_FEATURED_ACTIVATED, false)
    set(value) = edit { putBoolean(Constants.SharedPrefs.VENUES_FEATURED_ACTIVATED, value) }

var SharedPreferences.isVenueOnBoardingDone: Boolean
    get() = getBoolean(Constants.SharedPrefs.VENUES_ON_BOARDING_DONE, false)
    set(value) = edit { putBoolean(Constants.SharedPrefs.VENUES_ON_BOARDING_DONE, value) }

var SharedPreferences.currentVaccinationReferenceDepartmentCode: String?
    get() = getString(Constants.SharedPrefs.CURRENT_VACCINATION_REFERENCE_DEPARTMENT_CODE, null)
    set(value) = edit { putString(Constants.SharedPrefs.CURRENT_VACCINATION_REFERENCE_DEPARTMENT_CODE, value) }

var SharedPreferences.currentVaccinationReferenceLatitude: Double?
    get() = if (contains(Constants.SharedPrefs.CURRENT_VACCINATION_REFERENCE_LATITUDE)) {
        getFloat(
            Constants.SharedPrefs.CURRENT_VACCINATION_REFERENCE_LATITUDE,
            0f
        ).toDouble()
    } else {
        null
    }
    set(value) = edit {
        if (value != null) {
            putFloat(Constants.SharedPrefs.CURRENT_VACCINATION_REFERENCE_LATITUDE, value.toFloat())
        } else {
            remove(Constants.SharedPrefs.CURRENT_VACCINATION_REFERENCE_LATITUDE)
        }
    }

var SharedPreferences.currentVaccinationReferenceLongitude: Double?
    get() = if (contains(Constants.SharedPrefs.CURRENT_VACCINATION_REFERENCE_LONGITUDE)) {
        getFloat(
            Constants.SharedPrefs.CURRENT_VACCINATION_REFERENCE_LONGITUDE,
            0f
        ).toDouble()
    } else {
        null
    }
    set(value) = edit {
        if (value != null) {
            putFloat(Constants.SharedPrefs.CURRENT_VACCINATION_REFERENCE_LONGITUDE, value.toFloat())
        } else {
            remove(Constants.SharedPrefs.CURRENT_VACCINATION_REFERENCE_LONGITUDE)
        }
    }

var SharedPreferences.zipGeolocVersion: Int
    get() = getInt(Constants.SharedPrefs.ZIP_GEOLOC_VERSION, 0)
    set(value) = edit { putInt(Constants.SharedPrefs.ZIP_GEOLOC_VERSION, value) }

var SharedPreferences.alertRiskLevelChanged: Boolean
    get() = getBoolean(Constants.SharedPrefs.ALERT_RISK_LEVEL_CHANGED, false)
    set(value) = edit { putBoolean(Constants.SharedPrefs.ALERT_RISK_LEVEL_CHANGED, value) }

var SharedPreferences.hideRiskStatus: Boolean
    get() = getBoolean(Constants.SharedPrefs.HIDE_RISK_STATUS, false)
    set(value) = edit { putBoolean(Constants.SharedPrefs.HIDE_RISK_STATUS, value) }

var SharedPreferences.showSmartWallet: Boolean
    get() = getBoolean(Constants.SharedPrefs.SHOW_SMART_WALLET, true)
    set(value) = edit { putBoolean(Constants.SharedPrefs.SHOW_SMART_WALLET, value) }

var SharedPreferences.hasUsedUniversalQrScan: Boolean
    get() = getBoolean(Constants.SharedPrefs.HAS_USED_UNIVERSAL_QR_SCAN, false)
    set(value) = edit { putBoolean(Constants.SharedPrefs.HAS_USED_UNIVERSAL_QR_SCAN, value) }

var SharedPreferences.ratingsKeyFiguresOpening: Long
    get() = getLong(Constants.SharedPrefs.RATINGS_KEY_FIGURES_OPENING, 0L)
    set(value) = edit { putLong(Constants.SharedPrefs.RATINGS_KEY_FIGURES_OPENING, value) }

var SharedPreferences.ratingPopInShown: Boolean
    get() = getBoolean(Constants.SharedPrefs.RATINGS_SHOWN, false)
    set(value) = edit { putBoolean(Constants.SharedPrefs.RATINGS_SHOWN, value) }

var SharedPreferences.googleReviewShown: Boolean
    get() = getBoolean(Constants.SharedPrefs.GOOGLE_REVIEW_SHOWN, false)
    set(value) = edit { putBoolean(Constants.SharedPrefs.GOOGLE_REVIEW_SHOWN, value) }

var SharedPreferences.notificationVersionClosed: Int
    get() = getInt(Constants.SharedPrefs.NOTIFICATION_VERSION_CLOSED, 0)
    set(value) = edit { putInt(Constants.SharedPrefs.NOTIFICATION_VERSION_CLOSED, value) }

var SharedPreferences.lowStorageAlertShown: Boolean
    get() = getBoolean(Constants.SharedPrefs.LOW_STORAGE_ALERT_SHOWN, false)
    set(value) = edit { putBoolean(Constants.SharedPrefs.LOW_STORAGE_ALERT_SHOWN, value) }

var SharedPreferences.blacklistDccIteration: Int
    get() = getInt(Constants.SharedPrefs.BLACKLIST_DCC_ITERATION, -1)
    set(value) = edit { putInt(Constants.SharedPrefs.BLACKLIST_DCC_ITERATION, value) }

var SharedPreferences.blacklist2DdocIteration: Int
    get() = getInt(Constants.SharedPrefs.BLACKLIST_2DDOC_ITERATION, -1)
    set(value) = edit { putInt(Constants.SharedPrefs.BLACKLIST_2DDOC_ITERATION, value) }

var SharedPreferences.enableAutoFullscreenBrightness: Boolean
    get() = getBoolean(Constants.SharedPrefs.ENABLE_AUTO_FULLSCREEN_BRIGHTNESS, true)
    set(value) = edit { putBoolean(Constants.SharedPrefs.ENABLE_AUTO_FULLSCREEN_BRIGHTNESS, value) }

var SharedPreferences.keyFigureCompare1: String?
    get() = getString(Constants.SharedPrefs.COMPARE_KEY_FIGURE_LABEL_1, null)
    set(value) = edit { putString(Constants.SharedPrefs.COMPARE_KEY_FIGURE_LABEL_1, value) }

var SharedPreferences.keyFigureCompare2: String?
    get() = getString(Constants.SharedPrefs.COMPARE_KEY_FIGURE_LABEL_2, null)
    set(value) = edit { putString(Constants.SharedPrefs.COMPARE_KEY_FIGURE_LABEL_2, value) }

var SharedPreferences.notificationSent: Set<String>
    get() = getStringSet(Constants.SharedPrefs.SMART_WALLET_SENT_NOTIFICATIONS, null) ?: emptySet()
    set(value) = edit {
        putStringSet(Constants.SharedPrefs.SMART_WALLET_SENT_NOTIFICATIONS, value)
    }
