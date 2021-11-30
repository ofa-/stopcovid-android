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

import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.manager.SignalInfosManager
import com.mikepenz.fastadapter.GenericItem

class OnBoardingVenuesFragment : OnBoardingFragment() {

    override fun getTitleKey(): String = "onboarding.venuesController.title"
    override fun getButtonTitleKey(): String = "onboarding.venuesController.bottomButton"
    override fun getOnButtonClick(): () -> Unit = {
        findNavControllerOrNull()
            ?.safeNavigate(OnBoardingVenuesFragmentDirections.actionOnBoardingVenuesFragmentToOnBoardingGestureFragment())
    }

    override suspend fun getItems(): List<GenericItem> = SignalInfosManager.getInfosItems(requireContext(), strings)
}
