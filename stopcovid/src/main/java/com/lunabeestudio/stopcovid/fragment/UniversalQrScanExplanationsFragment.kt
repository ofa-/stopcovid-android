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
import android.view.ViewGroup
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColor
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem

class UniversalQrScanExplanationsFragment : MainFragment() {

    override fun getTitleKey(): String = "universalQrScanExplanationsController.title"

    override suspend fun getItems(): List<GenericItem> {
        val items = mutableListOf<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.ic_qrscan
            imageTint = R.attr.colorPrimary.fetchSystemColor(requireContext())
            identifier = R.drawable.ic_qrscan.toLong()
        }

        items += cardWithActionItem {
            mainTitle = strings["universalQrScanExplanationsController.explanation.title"]
            mainBody = strings["universalQrScanExplanationsController.explanation.android.subtitle"]
            mainGravity = Gravity.CENTER
            identifier = "universalQrScanExplanationsController.explanation.title".hashCode().toLong()
        }

        items += buttonItem {
            text = strings["universalQrScanExplanationsController.button.title"]
            width = ViewGroup.LayoutParams.MATCH_PARENT
            onClickListener = View.OnClickListener {
                findNavControllerOrNull()?.safeNavigate(
                    UniversalQrScanExplanationsFragmentDirections.actionUniversalQrScanExplanationsFragmentToUniversalQrScanFragment()
                )
            }
            identifier = "universalQrScanExplanationsController.button.title".hashCode().toLong()
        }

        return items
    }
}