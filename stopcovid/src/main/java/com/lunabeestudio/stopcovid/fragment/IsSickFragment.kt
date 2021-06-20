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
import android.view.View
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem

class IsSickFragment : AboutMainFragment() {

    override fun getTitleKey(): String = "myHealthController.sick.title"

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.sick
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["myHealthController.sick.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["myHealthController.sick.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += buttonItem {
            text = strings["myHealthController.button.recommendations"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                strings["myHealthController.button.recommendations.url"]?.openInExternalBrowser(requireContext())
            }
            identifier = items.count().toLong()
        }
        items += buttonItem {
            text = strings["myHealthController.step.appointment.buttonTitle"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                strings["callCenter.phoneNumber"]?.callPhone(requireContext())
            }
            identifier = items.count().toLong()
        }
        items += buttonItem {
            text = strings["myHealthController.button.cautionMeasures"]
            gravity = Gravity.CENTER
            onClickListener = View.OnClickListener {
                findNavControllerOrNull()?.safeNavigate(IsSickFragmentDirections.actionIsSickFragmentToGestureFragment())
            }
            identifier = items.count().toLong()
        }

        return items
    }
}
