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

import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem

class IsSickFragment : AboutMainFragment() {

    override fun getTitleKey(): String = "myHealthController.sick.title"

    override suspend fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.sick
            identifier = items.count().toLong()
        }
        items += cardWithActionItem {
            mainTitle = strings["myHealthController.sick.mainMessage.title"]
            mainBody = strings["myHealthController.sick.mainMessage.subtitle"]
            actions = listOfNotNull(
                Action(null, strings["myHealthController.button.recommendations"]) {
                    strings["myHealthController.button.recommendations.url"]?.openInExternalBrowser(requireContext())
                },
                Action(null, strings["myHealthController.step.appointment.buttonTitle"]) {
                    strings["callCenter.phoneNumber"]?.callPhone(requireContext())
                },
                Action(null, strings["myHealthController.button.cautionMeasures"]) {
                    findNavControllerOrNull()?.safeNavigate(IsSickFragmentDirections.actionIsSickFragmentToGestureFragment())
                }
            )
            identifier = "myHealthController.sick.mainMessage.title".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
        }

        return items
    }
}
