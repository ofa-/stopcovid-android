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

import android.os.Bundle
import android.view.View
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.fillItems
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.manager.PrivacyManager
import com.mikepenz.fastadapter.GenericItem

class OnBoardingPrivacyFragment : OnBoardingFragment() {

    val privacyManager: PrivacyManager by lazy(LazyThreadSafetyMode.NONE) {
        injectionContainer.privacyManager
    }

    override fun getTitleKey(): String = "onboarding.privacyController.title"
    override fun getButtonTitleKey(): String = "onboarding.privacyController.accept"
    override fun getOnButtonClick(): () -> Unit = {
        findNavControllerOrNull()
            ?.safeNavigate(OnBoardingPrivacyFragmentDirections.actionOnBoardingPrivacyFragmentToOnBoardingProximityFragment())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        privacyManager.privacySections.observe(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
        }
        privacyManager.privacySections.value?.fillItems(items)

        return items
    }
}
