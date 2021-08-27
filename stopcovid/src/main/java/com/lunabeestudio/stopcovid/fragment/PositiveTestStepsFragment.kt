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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.dccCertificatesManager
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showUnknownErrorAlert
import com.lunabeestudio.stopcovid.extension.walletCertificateError
import com.lunabeestudio.stopcovid.fastitem.selectionItem
import com.lunabeestudio.stopcovid.manager.WalletManager
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.viewmodel.PositiveTestStepsViewModel
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.launch

class PositiveTestStepsFragment : BottomSheetMainFragment() {

    val args: PositiveTestStepsFragmentArgs by navArgs()

    private val keystoreDataSource by lazy {
        requireContext().secureKeystoreDataSource()
    }

    val viewModel: PositiveTestStepsViewModel by viewModels {
        PositiveTestStepsViewModel.PositiveTestStepsViewModelFactory(this, keystoreDataSource)
    }

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val dccCertificatesManager by lazy {
        requireContext().dccCertificatesManager()
    }

    override fun getTitleKey(): String = "positiveTestStepsController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.currentStep.observe(viewLifecycleOwner) { refreshScreen() }
    }

    override fun onBottomSheetButtonClicked() {
        if (viewModel.currentStep.value == 0) {
            lifecycleScope.launch {
                addCertificate()
            }
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

    override fun getItems(): List<GenericItem> {
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

    private suspend fun addCertificate() {
        try {
            val certificate = WalletManager.verifyAndGetCertificateCodeValue(
                robertManager.configuration,
                args.positiveTestDccValue,
                dccCertificatesManager.certificates,
                WalletCertificateType.Format.WALLET_DCC,
            )

            viewModel.saveCertificate(certificate)
        } catch (e: Exception) {
            handleCertificateError(e, args.positiveTestDccValue)
            viewModel.skipCertificate()
        }
    }

    private suspend fun handleCertificateError(error: Exception, certificateCode: String?) {
        val certificateType = certificateCode?.let { WalletCertificate.getTypeFromValue(it) } ?: WalletCertificateType.VACCINATION_EUROPE
        handleCertificateError(error, certificateType)
    }

    private fun handleCertificateError(error: Exception, certificateType: WalletCertificateType) {
        val certificateError = error.walletCertificateError()
        if (certificateError != null) {
            findNavControllerOrNull()?.safeNavigate(
                PositiveTestStepsFragmentDirections.actionPositiveTestStepsFragmentToWalletCertificateErrorFragment(
                    certificateType,
                    certificateError,
                )
            )
        } else {
            showUnknownErrorAlert(null)
        }
    }
}