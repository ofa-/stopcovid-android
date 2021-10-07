/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/02/12 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.analytics.model.ErrorEventName
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeMs
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setImageResourceOrHide
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showDbFailure
import com.lunabeestudio.stopcovid.extension.showMigrationFailed
import com.lunabeestudio.stopcovid.fastitem.deleteCardItem
import com.lunabeestudio.stopcovid.viewmodel.VenuesHistoryViewModel
import com.lunabeestudio.stopcovid.viewmodel.VenuesHistoryViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VenuesHistoryFragment : MainFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: VenuesHistoryViewModel by viewModels {
        VenuesHistoryViewModelFactory(venueRepository)
    }

    override fun getTitleKey(): String = "venuesHistoryController.title"

    private val dateTimeFormat: DateFormat = SimpleDateFormat.getDateTimeInstance(
        DateFormat.LONG,
        DateFormat.SHORT,
        Locale.getDefault()
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModelObserver()

        binding?.emptyLayout?.emptyImageView?.setImageResourceOrHide(R.drawable.signal)
        binding?.emptyLayout?.emptyTitleTextView?.isVisible = false
        binding?.emptyLayout?.emptyButton?.isVisible = false

        showMigrationFailedIfNeeded()
        showDbFailureIfNeeded()
    }

    private fun initViewModelObserver() {
        viewModel.venuesQrCodeLiveData.observe(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items.addAll(
            viewModel.venuesQrCodeLiveData
                .value
                ?.reversed()
                ?.map { venueQrCode ->
                    val venueType = strings["venueType.default"] ?: ""
                    val venueDate = dateTimeFormat.format(Date(venueQrCode.ntpTimestamp.ntpTimeSToUnixTimeMs()))
                    deleteCardItem {
                        title = stringsFormat("venuesHistoryController.entry", venueType, venueDate)
                        caption = venueQrCode.ltid
                        deleteContentDescription = strings["common.delete"]
                        onDelete = {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(strings["venuesHistoryController.delete.alert.title"])
                                .setMessage(strings["venuesHistoryController.delete.alert.message"])
                                .setPositiveButton(strings["common.delete"]) { _, _ ->
                                    viewModel.deleteVenue(venueQrCode.id)
                                }
                                .setNegativeButton(strings["common.cancel"], null)
                                .show()
                        }
                        identifier = venueQrCode.id.hashCode().toLong()
                    }
                } ?: emptyList()
        )

        val footer = strings["venuesHistoryController.footer"]
        if (items.isNotEmpty() && !footer.isNullOrBlank()) {
            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.count().toLong()
            }
            items += captionItem {
                text = footer
                textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
                identifier = "venuesHistoryController.footer".hashCode().toLong()
            }
        }

        return items
    }

    override fun refreshScreen() {
        super.refreshScreen()
        if (robertManager.isImmune) {
            binding?.emptyLayout?.emptyDescriptionTextView?.text = strings["venuesHistoryController.noVenuesEmptyView.isSick.title"]
        } else {
            binding?.emptyLayout?.emptyDescriptionTextView?.text = strings["venuesHistoryController.noVenuesEmptyView.title"]
        }
    }

    private fun showMigrationFailedIfNeeded() {
        if (debugManager.oldVenuesInSharedPrefs()) {
            context?.let {
                analyticsManager.reportErrorEvent(ErrorEventName.ERR_VENUES_MIG)
                MaterialAlertDialogBuilder(it).showMigrationFailed(strings) {
                    viewModel.deleteDeprecatedVenues()
                }
            }
        }
    }

    private fun showDbFailureIfNeeded() {
        lifecycleScope.launch {
            try {
                context?.secureKeystoreDataSource()?.venuesQrCode()
            } catch (e: Exception) {
                context?.let {
                    analyticsManager.reportErrorEvent(ErrorEventName.ERR_VENUES_DB)
                    MaterialAlertDialogBuilder(it).showDbFailure(strings)
                }
            }
        }
    }
}