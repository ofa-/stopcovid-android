/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.fastitem.doubleLogoItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.viewmodel.OnBoardingViewModel
import com.lunabeestudio.stopcovid.viewmodel.OnBoardingViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class OnBoardingWelcomeFragment : OnBoardingFragment() {

    private val viewModel: OnBoardingViewModel by activityViewModels { OnBoardingViewModelFactory() }

    override fun getTitleKey(): String = "onboarding.welcomeController.title"
    override fun getButtonTitleKey(): String? = "onboarding.welcomeController.howDoesItWork"
    override fun getOnButtonClick(): () -> Unit = {
        findNavController()
            .navigate(OnBoardingWelcomeFragmentDirections.actionOnBoardingWelcomeFragmentToOnBoardingExplanationFragment())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.showLogo.observe(viewLifecycleOwner) {
            if (binding?.recyclerView?.isComputingLayout == false) {
                refreshScreen()
            }
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.home
            isInvisible = viewModel.showLogo.value != true
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["onboarding.welcomeController.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings["onboarding.welcomeController.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }

        return items
    }
}
