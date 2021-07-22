/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/19/7 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.extension

import android.content.SharedPreferences
import androidx.core.content.edit
import com.lunabeestudio.stopcovid.coreui.UiConstants

var SharedPreferences.userLanguage: String?
    get() = getString(UiConstants.SharedPrefs.USER_LANGUAGE, null)
    set(value) = edit { putString(UiConstants.SharedPrefs.USER_LANGUAGE, value) }