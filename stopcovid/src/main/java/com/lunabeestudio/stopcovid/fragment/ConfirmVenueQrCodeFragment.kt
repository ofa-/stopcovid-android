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
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showExpiredCodeAlert
import com.lunabeestudio.stopcovid.extension.showInvalidCodeAlert
import com.lunabeestudio.stopcovid.fastitem.dangerButtonItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.manager.VenuesManager
import com.mikepenz.fastadapter.GenericItem

class ConfirmVenueQrCodeFragment : MainFragment() {

    private val args: ConfirmVenueQrCodeFragmentArgs by navArgs()

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    override fun getTitleKey(): String = "confirmVenueQrCodeController.title"

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.signal
            identifier = R.drawable.signal.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["confirmVenueQrCodeController.explanation.title"]
            gravity = Gravity.CENTER
            identifier = "confirmVenueQrCodeController.explanation.title".hashCode().toLong()
        }
        items += titleItem {
            text = strings["confirmVenueQrCodeController.explanation.subtitle"]
            gravity = Gravity.CENTER
            identifier = "confirmVenueQrCodeController.explanation.subtitle".hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
        items += buttonItem {
            text = strings["confirmVenueQrCodeController.confirm"]
            gravity = Gravity.CENTER
            width = ViewGroup.LayoutParams.MATCH_PARENT
            onClickListener = View.OnClickListener {
                val venueType = VenuesManager.processVenuePath(
                    robertManager = robertManager,
                    secureKeystoreDataSource = requireContext().secureKeystoreDataSource(),
                    args.venueFullPath,
                )
                handleVenueType(args.venueFullPath, venueType)
            }
            identifier = "confirmVenueQrCodeController.confirm".hashCode().toLong()
        }
        items += dangerButtonItem {
            text = strings["common.cancel"]
            gravity = Gravity.CENTER
            width = ViewGroup.LayoutParams.MATCH_PARENT
            onClickListener = View.OnClickListener {
                findNavControllerOrNull()?.navigateUp()
            }
            identifier = "common.cancel".hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        return items
    }

    private fun handleVenueType(code: String, venueType: String?) {
        if (venueType == null) {
            if (VenuesManager.isVenuePathExpired(robertManager, code)) {
                context?.showExpiredCodeAlert(strings)
            } else {
                context?.showInvalidCodeAlert(strings)
            }
            findNavControllerOrNull()?.navigateUp()
        } else {
            findNavControllerOrNull()
                ?.safeNavigate(ConfirmVenueQrCodeFragmentDirections.actionConfirmVenueQrCodeFragmentToVenueConfirmationFragment(venueType))
        }
    }
}