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

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.showAlertSickVenue
import com.lunabeestudio.stopcovid.extension.showExpiredCodeAlert
import com.lunabeestudio.stopcovid.extension.showInvalidCodeAlert
import com.lunabeestudio.stopcovid.extension.showUnknownErrorAlert
import com.lunabeestudio.stopcovid.fastitem.dangerButtonItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.model.VenueExpiredException
import com.lunabeestudio.stopcovid.model.VenueInvalidFormatException
import com.lunabeestudio.stopcovid.viewmodel.ConfirmVenueQrCodeViewModel
import com.lunabeestudio.stopcovid.viewmodel.ConfirmVenueQrCodeViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class ConfirmVenueQrCodeFragment : MainFragment() {

    private val args: ConfirmVenueQrCodeFragmentArgs by navArgs()

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: ConfirmVenueQrCodeViewModel by viewModels {
        ConfirmVenueQrCodeViewModelFactory(
            robertManager,
            analyticsManager,
            venueRepository,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (robertManager.isImmune) {
            MaterialAlertDialogBuilder(requireContext()).showAlertSickVenue(
                strings = strings,
                onIgnore = null,
                onCancel = { findNavControllerOrNull()?.navigateUp() }
            )
        }
        initViewModelObserver()
    }

    private fun initViewModelObserver() {
        viewModel.venueProcessed.observe(viewLifecycleOwner) {
            findNavControllerOrNull()?.navigateUp()
        }
        viewModel.exception.observe(viewLifecycleOwner) { e ->
            when (e) {
                is VenueExpiredException -> context?.showExpiredCodeAlert(strings, null)
                is VenueInvalidFormatException -> context?.showInvalidCodeAlert(strings, null)
                else -> showUnknownErrorAlert(null)
            }
        }
    }

    override fun getTitleKey(): String = "confirmVenueQrCodeController.title"

    override suspend fun getItems(): List<GenericItem> {
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
                viewModel.processVenue(args.venueContent, args.venueVersion, args.venueTime)
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
}
