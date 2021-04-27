/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/27/04 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.analytics.extension

import android.content.SharedPreferences
import androidx.core.content.edit

private const val SHARED_PREFS_INSTALLATION_UUID: String = "Shared.Prefs.Installation.UUID"
private const val SHARED_PREFS_PROXIMITY_START_TIME: String = "Shared.Prefs.Proximity.Start.Time"
private const val SHARED_PREFS_PROXIMITY_ACTIVE_DURATION: String = "Shared.Prefs.Proximity.Active.Duration"
private const val SHARED_PREFS_STATUS_SUCCESS_COUNT: String = "Shared.Prefs.Status.Success.Count"
private const val SHARED_PREFS_IS_OPT_IN: String = "Shared.Prefs.Is.Opt.In"

var SharedPreferences.installationUUID: String?
    get() = getString(SHARED_PREFS_INSTALLATION_UUID, null)
    set(value) = edit {
        if (value.isNullOrBlank()) {
            remove(SHARED_PREFS_INSTALLATION_UUID)
        } else {
            putString(SHARED_PREFS_INSTALLATION_UUID, value)
        }
    }

var SharedPreferences.proximityStartTime: Long?
    get() = if (contains(SHARED_PREFS_PROXIMITY_START_TIME)) {
        getLong(SHARED_PREFS_PROXIMITY_START_TIME, System.currentTimeMillis())
    } else {
        null
    }
    set(value) = edit {
        if (value == null) {
            remove(SHARED_PREFS_PROXIMITY_START_TIME)
        } else {
            putLong(SHARED_PREFS_PROXIMITY_START_TIME, value)
        }
    }

var SharedPreferences.proximityActiveDuration: Long
    get() = getLong(SHARED_PREFS_PROXIMITY_ACTIVE_DURATION, 0L)
    set(value) = edit {
        putLong(SHARED_PREFS_PROXIMITY_ACTIVE_DURATION, value)
    }

var SharedPreferences.statusSuccessCount: Int
    get() = getInt(SHARED_PREFS_STATUS_SUCCESS_COUNT, 0)
    set(value) = edit {
        putInt(SHARED_PREFS_STATUS_SUCCESS_COUNT, value)
    }

var SharedPreferences.isOptIn: Boolean
    get() = getBoolean(SHARED_PREFS_IS_OPT_IN, true)
    set(value) = edit {
        putBoolean(SHARED_PREFS_IS_OPT_IN, value)
    }