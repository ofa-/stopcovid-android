/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/18 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.robert.model.AggregateBackendException
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.multipassPickerDescription
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.selectionItem
import com.lunabeestudio.stopcovid.viewmodel.MultipassCertificatesPickerUiModel
import com.lunabeestudio.stopcovid.viewmodel.MultipassCertificatesPickerViewModel
import com.lunabeestudio.stopcovid.viewmodel.MultipassCertificatesPickerViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MultipassCertificatesPickerFragment : BottomSheetMainFragment() {

    private val args by navArgs<MultipassCertificatesPickerFragmentArgs>()

    private val viewModel: MultipassCertificatesPickerViewModel by viewModels {
        MultipassCertificatesPickerViewModelFactory(
            profileId = args.profileId,
            getFilteredMultipassProfileFromIdUseCase = injectionContainer.getFilteredMultipassProfileFromIdUseCase,
            generateMultipassUseCase = injectionContainer.generateMultipassUseCase,
            robertManager = injectionContainer.robertManager,
        )
    }

    override fun getTitleKey(): String = "multiPass.selectionScreen.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.multipass.observe(viewLifecycleOwner) {
            refreshScreen()
        }

        viewModel.uiModel.observe(viewLifecycleOwner) { (bottomButtonState, profileNoDccMin, selectionMaxReached) ->
            when (bottomButtonState) {
                MultipassCertificatesPickerUiModel.ValidateButtonState.DISABLED -> setBottomButtonEnabled(false)
                MultipassCertificatesPickerUiModel.ValidateButtonState.ENABLED -> setBottomButtonEnabled(true)
            }

            if (profileNoDccMin?.getContentIfNotHandled(Event.NO_ID) != null) {
                findNavControllerOrNull()?.safeNavigate(
                    MultipassCertificatesPickerFragmentDirections
                        .actionMultipassCertificatesPickerFragmentToMultipassErrorFragment(
                            displayName = viewModel.multipass.value?.displayName ?: "",
                            errorKeys = emptyArray(),
                            notEnoughDcc = true,
                        ),
                    NavOptions.Builder().setPopUpTo(R.id.multipassCertificatesPickerFragment, true).build(),
                )
            }

            context?.let { ctx ->
                if (selectionMaxReached?.getContentIfNotHandled(Event.NO_ID) != null) {
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle(strings["multiPass.selectionScreen.alert.maximumSelection.title"])
                        .setMessage(strings["multiPass.selectionScreen.alert.maximumSelection.subtitle"])
                        .setPositiveButton(strings["common.ok"], null)
                        .show()
                }
            }
        }
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        viewModel.multipass.value?.let { (_, displayName, certificates) ->
            items += captionItem {
                text = stringsFormat("multiPass.selectionScreen.header.title", displayName)
                identifier = "multiPass.selectionScreen.header.title".hashCode().toLong()
            }

            certificates.forEachIndexed { index, certificate ->
                items += selectionItem {
                    title = strings["enum.HCertType.${certificate.type.code}"]?.uppercase()
                    caption = context?.let { certificate.multipassPickerDescription(strings, it) }
                    showSelection = viewModel.isCertificateSelected(certificate)
                    iconSelectionOff = R.drawable.ic_check_off
                    isEnabled = true
                    onClick = {
                        viewModel.toggleCertificate(certificate)
                        refreshScreen()
                    }
                    identifier = certificate.sha256.hashCode().toLong()
                }

                if (index < certificates.size - 1) {
                    items += dividerItem {
                        identifier = items.size.toLong()
                    }
                }
            }
        }

        return items
    }

    private fun showMultipassConfirmationAlert() {
        context?.let {
            MaterialAlertDialogBuilder(it)
                .setTitle(strings["multiPass.CGU.alert.title"])
                .setMessage(strings["multiPass.CGU.alert.subtitle"])
                .setPositiveButton(strings["common.ok"]) { _, _ ->
                    requestMultipass()
                }
                .setNegativeButton(strings["common.cancel"], null)
                .setNeutralButton(strings["multiPass.CGU.alert.linkButton.title"]) { _, _ ->
                    context?.let { ctx -> strings["multiPass.CGU.alert.url"]?.openInExternalBrowser(ctx) }
                }
                .show()
        }
    }

    private fun requestMultipass() {
        viewModel.generateMultipass().onEach { result ->
            (activity as? MainActivity)?.showProgress(result is TacResult.Loading)

            when (result) {
                is TacResult.Loading -> { /* no-op */
                }
                is TacResult.Success -> {
                    context?.let { ctx ->
                        MaterialAlertDialogBuilder(ctx)
                            .setTitle(strings["multiPass.generation.successAlert.title"])
                            .setMessage(strings["multiPass.generation.successAlert.subtitle"])
                            .setPositiveButton(strings["multiPass.generation.successAlert.buttonTitle"]) { _, _ ->
                                findNavControllerOrNull()?.safeNavigate(
                                    MultipassCertificatesPickerFragmentDirections
                                        .actionMultipassCertificatesPickerFragmentToWalletContainerFragment(
                                            scrollCertificateId = result.successData.id,
                                        )
                                )
                            }
                            .setCancelable(false)
                            .show()
                    }
                }
                is TacResult.Failure -> {
                    findNavControllerOrNull()?.safeNavigate(
                        MultipassCertificatesPickerFragmentDirections
                            .actionMultipassCertificatesPickerFragmentToMultipassErrorFragment(
                                displayName = viewModel.multipass.value?.displayName ?: "",
                                errorKeys = (result.throwable as? AggregateBackendException)?.errorCodes.orEmpty().toTypedArray(),
                                notEnoughDcc = false,
                            )
                    )
                }
            }
        }.launchIn(lifecycleScope)
    }

    override fun onBottomSheetButtonClicked() {
        showMultipassConfirmationAlert()
    }

    override fun getBottomSheetButtonKey(): String = "multiPass.selectionScreen.button.validate"
}
