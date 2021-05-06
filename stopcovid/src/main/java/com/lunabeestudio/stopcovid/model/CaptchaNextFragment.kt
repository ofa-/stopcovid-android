/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/02/12 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.model

import android.content.SharedPreferences
import androidx.navigation.NavArgs
import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.extension.isVenueOnBoardingDone
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fragment.CaptchaFragmentDirections
import com.lunabeestudio.stopcovid.fragment.VenueQRCodeFragmentArgs

enum class CaptchaNextFragment {
    Back {
        override val activateProximity: Boolean = true
        override fun registerPostAction(navController: NavController?, sharedPreferences: SharedPreferences, args: NavArgs?) {
            navController?.navigateUp()
        }
    },
    Venue {
        override val activateProximity: Boolean = false
        override fun registerPostAction(navController: NavController?, sharedPreferences: SharedPreferences, args: NavArgs?) {
            val arguments = args as? VenueQRCodeFragmentArgs
            if (sharedPreferences.isVenueOnBoardingDone || arguments != null) {
                navController?.safeNavigate(R.id.venueQrCodeFragment, arguments?.toBundle(), navOptions {
                    anim {
                        enter = R.anim.nav_default_enter_anim
                        popEnter = R.anim.nav_default_pop_enter_anim
                        popExit = R.anim.nav_default_pop_exit_anim
                        exit = R.anim.nav_default_exit_anim
                    }
                })
            } else {
                navController?.safeNavigate(CaptchaFragmentDirections.actionCaptchaFragmentToVenueOnBoardingFragment())
            }
        }
    };

    abstract val activateProximity: Boolean
    abstract fun registerPostAction(navController: NavController?, sharedPreferences: SharedPreferences, args: NavArgs? = null)
}