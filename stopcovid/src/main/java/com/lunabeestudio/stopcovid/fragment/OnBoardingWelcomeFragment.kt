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

import android.view.Gravity
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem

class OnBoardingWelcomeFragment : OnBoardingFragment() {

    override fun getTitleKey(): String = "onboarding.welcomeController.title"
    override fun getButtonTitleKey(): String? = "onboarding.welcomeController.howDoesItWork"
    override fun getOnButtonClick(): () -> Unit = {
        findNavControllerOrNull()
            ?.safeNavigate(OnBoardingWelcomeFragmentDirections.actionOnBoardingWelcomeFragmentToOnBoardingExplanationFragment())
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }
        items += logoItem {
            imageRes = R.drawable.home
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
