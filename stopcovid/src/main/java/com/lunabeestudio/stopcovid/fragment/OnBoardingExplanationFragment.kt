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

import android.view.Gravity
import android.view.View
import androidx.navigation.fragment.findNavController
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.fastitem.numberItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.mikepenz.fastadapter.GenericItem

class OnBoardingExplanationFragment : OnBoardingFragment() {

    override fun getTitleKey(): String = "onboarding.explanationsController.title"
    override fun getButtonTitleKey(): String? = "onboarding.explanationsController.dataPrivacy"
    override fun getOnButtonClick(): () -> Unit = {
        findNavController()
            .navigate(OnBoardingExplanationFragmentDirections.actionOnBoardingExplanationFragmentToOnBoardingPrivacyFragment())
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += numberItem {
            text = 1.toString()
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["onboarding.explanationsController.stepFollow.title"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings["onboarding.explanationsController.stepFollow.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }
        items += numberItem {
            text = 2.toString()
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["onboarding.explanationsController.stepInform.title"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings["onboarding.explanationsController.stepInform.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }
        items += numberItem {
            text = 3.toString()
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["onboarding.explanationsController.stepBeAware.title"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings["onboarding.explanationsController.stepBeAware.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }

        return items
    }
}
