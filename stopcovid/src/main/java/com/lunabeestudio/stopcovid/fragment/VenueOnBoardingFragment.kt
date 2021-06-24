/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.SharedPreferences
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.extension.isVenueOnBoardingDone
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.manager.SignalInfosManager
import com.mikepenz.fastadapter.GenericItem

class VenueOnBoardingFragment : BottomSheetMainFragment() {

    private val args: VenueOnBoardingFragmentArgs by navArgs()

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun getTitleKey(): String = "venuesRecording.onboardingController.title"
    override fun getBottomSheetButtonKey(): String = "venuesRecording.onboardingController.button.participate"
    override fun onBottomSheetButtonClicked() {
        sharedPreferences.isVenueOnBoardingDone = true
        findNavControllerOrNull()?.safeNavigate(R.id.venueQrCodeFragment, args.venueArgs)
    }

    override fun getItems(): List<GenericItem> = SignalInfosManager.getInfosItems(requireContext(), strings)
}