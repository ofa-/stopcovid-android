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
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.lottieItem
import com.mikepenz.fastadapter.GenericItem
import java.util.Locale

class VenueConfirmationFragment : BottomSheetMainFragment() {

    private val args: VenueConfirmationFragmentArgs by navArgs()

    override fun getTitleKey(): String = "venuesRecording.confirmationController.title"

    override fun getBottomSheetButtonKey(): String = "common.ok"

    override fun onBottomSheetButtonClicked() {
        findNavControllerOrNull()?.navigateUp()
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += lottieItem(R.raw.erp_waving) {
            identifier = R.raw.erp_waving.toLong()
        }
        items += bigTitleItem {
            text = strings["erp.confirmationMessage.${args.venueType.toLowerCase(Locale.getDefault())}.title"]
                ?: strings["erp.confirmationMessage.default.title"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["erp.confirmationMessage.${args.venueType.toLowerCase(Locale.getDefault())}.message"]
                ?: strings["erp.confirmationMessage.default.message"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        return items
    }
}