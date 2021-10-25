/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/15/07 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.lunabeestudio.stopcovid.R

fun NavController.safeNavigate(@IdRes resId: Int, args: Bundle?, navOptions: NavOptions?) {
    try {
        navigate(resId, args, navOptions, null)
    } catch (e: IllegalArgumentException) {
        // back and button pressed quickly can trigger this exception.
    }
}

fun NavController.safeNavigate(@IdRes resId: Int, args: Bundle?) {
    safeNavigate(
        resId, args,
        navOptions {
            anim {
                enter = R.anim.nav_default_enter_anim
                popEnter = R.anim.nav_default_pop_enter_anim
                popExit = R.anim.nav_default_pop_exit_anim
                exit = R.anim.nav_default_exit_anim
            }
        }
    )
}

fun NavController.safeNavigate(directions: NavDirections, navOptions: NavOptions? = null) {
    try {
        navigate(directions, navOptions)
    } catch (e: IllegalArgumentException) {
        // back and button pressed quickly can trigger this exception.
    }
}