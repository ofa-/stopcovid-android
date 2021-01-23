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
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.lightButtonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.textButtonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.startEmailIntent
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem

class ReportFragment : MainFragment() {

    private val args: ReportFragmentArgs by navArgs()
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
    }

    override fun getTitleKey(): String = "declareController.title"

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.declare
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }
        if (robertManager.isRegistered) {
            items += titleItem {
                text = strings["sickController.message.testedPositive.title"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
            items += captionItem {
                text = strings["sickController.message.testedPositive.subtitle"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.size.toLong()
            }
            items += textButtonItem {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                text = strings["declareController.codeNotReceived.buttonTitle"]
                onClickListener = View.OnClickListener {
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
                identifier = items.count().toLong()
            }
            items += buttonItem {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                text = strings["sickController.button.flash"]
                onClickListener = View.OnClickListener {
                    findNavControllerOrNull()?.safeNavigate(ReportFragmentDirections.actionReportFragmentToReportQrCodeFragment())
                }
                identifier = items.count().toLong()
            }
            items += lightButtonItem {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                text = strings["sickController.button.tap"]
                onClickListener = View.OnClickListener {
                    findNavControllerOrNull()?.safeNavigate(ReportFragmentDirections.actionReportFragmentToCodeFragment())
                }
                identifier = items.count().toLong()
            }
        } else {
            items += titleItem {
                text = strings["declareController.notRegistered.mainMessage.title"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
            items += captionItem {
                text = strings["declareController.notRegistered.mainMessage.subtitle"]
                gravity = Gravity.CENTER
                identifier = items.count().toLong()
            }
        }

        return items
    }
}