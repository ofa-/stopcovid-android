/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/27/7 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.selectionItem
import com.lunabeestudio.stopcovid.viewmodel.PositiveTestStepsViewModel
import com.lunabeestudio.stopcovid.viewmodel.PositiveTestStepsViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class PositiveTestStepsFragment : BottomSheetMainFragment() {

    val args: PositiveTestStepsFragmentArgs by navArgs()

    val viewModel: PositiveTestStepsViewModel by viewModels {
        PositiveTestStepsViewModelFactory(this)
    }

    override fun getTitleKey(): String = "positiveTestStepsController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.currentStep.observe(viewLifecycleOwner) { refreshScreen() }
    }

    override fun onBottomSheetButtonClicked() {
        if (viewModel.currentStep.value == 0) {
            findNavControllerOrNull()?.safeNavigate(
                PositiveTestStepsFragmentDirections.actionPositiveTestStepsFragmentToConfirmAddWalletCertificateFragment(
                    args.positiveTestDccValue,
                    WalletCertificateType.Format.WALLET_DCC.name,
                )
            )
            viewModel.completeAddCertificateStep()
        } else {
            findNavControllerOrNull()?.safeNavigate(
                PositiveTestStepsFragmentDirections.actionPositiveTestStepsFragmentToSymptomsOriginFragment(args.reportCode)
            )
        }
    }

    override fun getBottomSheetButtonKey(): String {
        return if (viewModel.currentStep.value == 0) {
            "positiveTestStepsController.step.addCertificate.button"
        } else {
            "positiveTestStepsController.step.declare.button"
        }
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = mutableListOf<GenericItem>()
        val currentStep = viewModel.currentStep.value ?: return items

        items += titleItem {
            text = strings["positiveTestStepsController.sectionTitle"]
            identifier = items.count().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }

        items += dividerItem {
            marginStartRes = null
            identifier = items.count().toLong()
        }

        items += selectionItem {
            caption = strings["positiveTestStepsController.step.addCertificate.label"]
            showSelection = currentStep > 0
            isEnabled = currentStep <= 0
            identifier = items.count().toLong()
        }

        items += dividerItem {
            identifier = items.count().toLong()
        }

        items += selectionItem {
            caption = strings["positiveTestStepsController.step.declare.label"]
            showSelection = currentStep > 1
            isEnabled = currentStep <= 1
            identifier = items.count().toLong()
        }

        items += dividerItem {
            marginStartRes = null
            identifier = items.count().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }

        items += captionItem {
            text = strings["positiveTestStepsController.sectionFooter"]
            textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
            identifier = items.count().toLong()
        }

        return items
    }
}
