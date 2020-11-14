/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/06 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.extension

import timber.log.Timber

inline fun <reified T : Enum<T>> safeEnumValueOf(name: String?): T? {
    return try {
        enumValueOf<T>(name!!)
    } catch (e: IllegalArgumentException) {
        Timber.e("Failed to get enum ${T::class.java.simpleName} for string \"$name\"")
        null
    }
}