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
import android.view.View
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.databinding.FragmentRecyclerWithBottomActionBinding
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem

class ReportFragment : MainFragment() {

    private val args: ReportFragmentArgs by navArgs()
    override val layout: Int = R.layout.fragment_recycler_with_bottom_action
    private var bottomActionBinding: FragmentRecyclerWithBottomActionBinding? = null
    private var codeUsed: Boolean = false
    private val robertManager by lazy {
        requireContext().robertManager()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (robertManager.isRegistered) {
            if (args.code != null && !codeUsed) {
                findNavControllerOrNull()?.safeNavigate(ReportFragmentDirections.actionReportFragmentToCodeFragment(args.code))
                codeUsed = true
            }
        }

        bottomActionBinding = FragmentRecyclerWithBottomActionBinding.bind(view).apply {
            bottomSheetButton.setOnClickListener {
                findNavControllerOrNull()?.safeNavigate(ReportFragmentDirections.actionReportFragmentToReportBottomSheetFragment())
            }
        }
    }

    override fun refreshScreen() {
        super.refreshScreen()
        bottomActionBinding?.bottomSheetButton?.text = strings["declareController.title"]
    }

    override fun getTitleKey(): String = "declareController.title"

    override suspend fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.declare
            identifier = items.count().toLong()
        }
        if (robertManager.isRegistered) {
            items += cardWithActionItem {
                mainTitle = strings["declareController.message.testedPositive.title"]
                mainBody = strings["declareController.message.testedPositive.subtitle"]
                identifier = "declareController.message.testedPositive.title".hashCode().toLong()
                actions = listOf(
                    Action(
                        label = strings["declareController.codeNotReceived.buttonTitle"],
                        onClickListener = {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(strings["declareController.codeNotReceived.alert.title"])
                                .setMessage(strings["declareController.codeNotReceived.alert.message"])
                                .setPositiveButton(strings["common.ok"], null)
                                .setNegativeButton(strings["declareController.codeNotReceived.alert.showVideo"]) { _, _ ->
                                    strings["declareController.codeNotReceived.alert.video.url"]?.openInExternalBrowser(requireContext())
                                }
                                .setNeutralButton(strings["declareController.codeNotReceived.alert.contactUs"]) { _, _ ->
                                    strings["contactUs.url"]?.openInExternalBrowser(requireContext())
                                }
                                .show()
                        }
                    )
                )
            }
        } else {
            items += cardWithActionItem {
                mainTitle = strings["declareController.notRegistered.mainMessage.title"]
                mainBody = strings["declareController.notRegistered.mainMessage.subtitle"]
                identifier = "declareController.notRegistered.mainMessage.title".hashCode().toLong()
            }
        }

        return items
    }
}
