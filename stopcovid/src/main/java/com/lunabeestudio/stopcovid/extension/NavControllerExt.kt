/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/15/07 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions

fun NavController.safeNavigate(@IdRes resId: Int, args: Bundle?,
    navOptions: NavOptions?) {
    try {
        navigate(resId, args, navOptions, null)
    } catch (e: IllegalArgumentException) {
        // back and button pressed quickly can trigger this exception.
    }
}

fun NavController.safeNavigate(directions: NavDirections) {
    try {
        navigate(directions.actionId, directions.arguments)
    } catch (e: IllegalArgumentException) {
        // back and button pressed quickly can trigger this exception.
    }
}