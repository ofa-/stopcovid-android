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
import androidx.navigation.NavController
import com.lunabeestudio.stopcovid.extension.isVenueOnBoardingDone
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fragment.CaptchaFragmentDirections

enum class CaptchaNextFragment {
    Back {
        override val activateProximity: Boolean = true
        override fun registerPostAction(navController: NavController?, sharedPreferences: SharedPreferences, args: Any?) {
            navController?.navigateUp()
        }
    },
    Venue {
        override val activateProximity: Boolean = false
        override fun registerPostAction(navController: NavController?, sharedPreferences: SharedPreferences, args: Any?) {
            val venueFullPath = args as? String
            if (sharedPreferences.isVenueOnBoardingDone || venueFullPath != null) {
                navController?.safeNavigate(CaptchaFragmentDirections.actionCaptchaFragmentToVenueQrCodeFragment(venueFullPath = venueFullPath))
            } else {
                navController?.safeNavigate(CaptchaFragmentDirections.actionCaptchaFragmentToVenueOnBoardingFragment())
            }
        }
    },
    Private {
        override val activateProximity: Boolean = false
        override fun registerPostAction(navController: NavController?, sharedPreferences: SharedPreferences, args: Any?) {
            navController?.safeNavigate(CaptchaFragmentDirections.actionCaptchaFragmentToVenuesPrivateEventFragment())
        }
    };

    abstract val activateProximity: Boolean
    abstract fun registerPostAction(navController: NavController?, sharedPreferences: SharedPreferences, args: Any? = null)
}