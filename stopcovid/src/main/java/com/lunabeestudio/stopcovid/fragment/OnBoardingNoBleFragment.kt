/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/26/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.view.Gravity
import android.view.View
import androidx.core.app.ShareCompat
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.lightButtonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.openInChromeTab
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem

class OnBoardingNoBleFragment : OnBoardingFragment() {

    override fun getTitleKey(): String = "onboarding.noBleController.title"
    override fun getButtonTitleKey(): String? = "onboarding.noBleController.accept"

    override fun getOnButtonClick(): () -> Unit = {
        ShareCompat.IntentBuilder.from(requireActivity())
            .setType("text/plain")
            .setText(strings["sharingController.appSharingMessage"])
            .startChooser()
    }

    override fun getItems(): List<GenericItem> {
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
            text = strings["onboarding.noBleController.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings["onboarding.noBleController.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += lightButtonItem {
            text = strings["onboarding.noBleController.infos"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                strings["onboarding.noBleController.infosUrl"]?.openInChromeTab(requireContext())
            }
            identifier = items.size.toLong()
        }

        return items
    }
}