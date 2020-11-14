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
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

val SharedPreferences.isOnBoardingDone: Boolean
    get() = getBoolean(Constants.SharedPrefs.ON_BOARDING_DONE, false)

var SharedPreferences.lastInfoCenterRefresh: Long
    get() = getLong(Constants.SharedPrefs.LAST_INFO_CENTER_REFRESH, 0L)
    set(value) = edit { putLong(Constants.SharedPrefs.LAST_INFO_CENTER_REFRESH, value) }

@OptIn(ExperimentalTime::class)
var SharedPreferences.lastInfoCenterFetch: Duration
    get() = getLong(Constants.SharedPrefs.LAST_INFO_CENTER_FETCH, 0L).milliseconds
    set(value) = edit { putLong(Constants.SharedPrefs.LAST_INFO_CENTER_FETCH, value.toLongMilliseconds()) }

fun SharedPreferences.isAdvertisementAvailable(defValue: Boolean): Boolean = getBoolean(
    Constants.SharedPrefs.IS_ADVERTISEMENT_AVAILABLE,
    defValue
)

fun SharedPreferences.setAdvertisementAvailable(value: Boolean) = edit {
    putBoolean(Constants.SharedPrefs.IS_ADVERTISEMENT_AVAILABLE, value)
}

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