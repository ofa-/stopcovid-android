/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/26/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.view.Gravity
import android.view.View
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.lightButtonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem

class OnBoardingNoBleFragment : OnBoardingFragment() {

    override fun getTitleKey(): String = "onboarding.runWithoutBleController.title"
    override fun getButtonTitleKey(): String = "onboarding.runWithoutBleController.accept"

    override fun getOnButtonClick(): () -> Unit = {
        if (requireContext().robertManager().configuration.displayRecordVenues) {
            findNavControllerOrNull()
                ?.safeNavigate(
                    OnBoardingNoBleFragmentDirections.actionOnBoardingNoBleFragmentToOnBoardingNotificationFragment()
                )
        } else {
            findNavControllerOrNull()
                ?.safeNavigate(
                    OnBoardingNoBleFragmentDirections.actionOnBoardingNoBleFragmentToOnBoardingGestureFragment()
                )
        }
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.no_ble
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["onboarding.runWithoutBleController.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings["onboarding.runWithoutBleController.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += lightButtonItem {
            text = strings["onboarding.runWithoutBleController.infos"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                strings["onboarding.runWithoutBleController.infosUrl"]?.openInExternalBrowser(requireContext())
            }
            identifier = items.size.toLong()
        }

        return items
    }
}