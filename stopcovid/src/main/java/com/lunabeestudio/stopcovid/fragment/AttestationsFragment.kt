/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/29/10 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.analytics.model.ErrorEventName
import com.lunabeestudio.domain.model.Attestation
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.databinding.FragmentRecyclerWithBottomActionBinding
import com.lunabeestudio.stopcovid.extension.isExpired
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showDbFailure
import com.lunabeestudio.stopcovid.extension.showErrorSnackBar
import com.lunabeestudio.stopcovid.extension.showMigrationFailed
import com.lunabeestudio.stopcovid.fastitem.AttestationCardItem
import com.lunabeestudio.stopcovid.fastitem.attestationCardItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.linkCardItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.viewmodel.AttestationsViewModel
import com.lunabeestudio.stopcovid.viewmodel.AttestationsViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.launch

import com.lunabeestudio.stopcovid.viewmodel.NewAttestationViewModel
import com.lunabeestudio.stopcovid.viewmodel.NewAttestationViewModelFactory
import com.lunabeestudio.domain.model.FormEntry
import androidx.fragment.app.activityViewModels


class AttestationsFragment : MainFragment() {

    override val layout: Int = R.layout.fragment_recycler_with_bottom_action
    private var bottomActionBinding: FragmentRecyclerWithBottomActionBinding? = null
    private val barcodeEncoder: BarcodeEncoder = BarcodeEncoder()
    private val qrCodeSize: Int by lazy {
        R.dimen.qr_code_size.toDimensSize(requireContext()).toInt()
    }

    private val robertManager: RobertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: AttestationsViewModel by viewModels {
        AttestationsViewModelFactory(
            requireContext().secureKeystoreDataSource(),
            attestationRepository
        )
    }

    override fun getTitleKey(): String = "attestationsController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.attestations.observe(viewLifecycleOwner) {
            refreshScreen()
        }
        showMigrationFailedIfNeeded()
        showDbFailureIfNeeded(false)

        bottomActionBinding = FragmentRecyclerWithBottomActionBinding.bind(view).apply {
            bottomSheetButton.setOnClickListener {
                findNavControllerOrNull()?.safeNavigate(
                    AttestationsFragmentDirections.actionAttestationsFragmentToNewAttestationFragment()
                )
            }
        }
    }

    override fun refreshScreen() {
        super.refreshScreen()
        bottomActionBinding?.bottomSheetButton?.text = strings["attestationsController.newAttestation"]
    }

    val reasonMap = mapOf(
        1 to "travail",
        2 to "sante",
        3 to "famille",
        8 to "animaux",
    )
    val labelMap = mapOf(
        1 to "Travail",
        2 to "Santé",
        3 to "Famille",
        8 to "Animaux",
    )

    private fun generateNewAttestation(typ: Int) {
        val navm: NewAttestationViewModel by activityViewModels {
            NewAttestationViewModelFactory(requireContext().secureKeystoreDataSource(), attestationRepository, formManager, requireContext())
        }
        navm.infos.put("datetime", FormEntry(System.currentTimeMillis().toString(), "datetime", Constants.Attestation.KEY_DATE_TIME))
        navm.infos.put("reason", FormEntry(reasonMap[typ], "list", Constants.Attestation.DATA_KEY_REASON))
        navm.generateQrCode(robertManager, strings, context)
        refreshScreen()
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        labelMap.forEach { key, labelText ->
        items += linkCardItem {
            label = "$key. $labelText"
            iconRes = R.drawable.ic_add
            onClickListener = View.OnClickListener {
                generateNewAttestation(key)
            }
            identifier = label.hashCode().toLong()
        }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        val validAttestations = viewModel.attestations.value?.filter { attestation ->
            !attestation.isExpired(robertManager.configuration)
        }?.sortedByDescending { attestation ->
            attestation.timestamp
        }
        val expiredAttestations = viewModel.attestations.value?.filter { attestation ->
            attestation.isExpired(robertManager.configuration)
        }?.sortedByDescending { attestation ->
            attestation.timestamp
        }

        if (validAttestations.isNullOrEmpty() && expiredAttestations.isNullOrEmpty()) {
            items += logoItem {
                imageRes = R.drawable.ic_attestation
                identifier = items.size.toLong()
            }

            items += cardWithActionItem {
                mainTitle = strings["attestationController.header.title"]
                mainBody = strings["attestationController.header.subtitle"]
                identifier = "attestationController.header.title".hashCode().toLong()
            }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
            }
        }

        if (!validAttestations.isNullOrEmpty()) {
            items += bigTitleItem {
                text = strings["attestationsController.validAttestationsSection.title"]
                identifier = "attestationsController.validAttestationsSection.subtitle".hashCode().toLong()
            }
            items += captionItem {
                text = strings["attestationsController.validAttestationsSection.subtitle"]
                identifier = "attestationsController.validAttestationsSection.subtitle".hashCode().toLong()
            }
            validAttestations.forEach { attestation ->
                items += qrCodeItemFromAttestation(attestation, true)
            }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.count().toLong()
            }
        }

        if (!expiredAttestations.isNullOrEmpty()) {
            items += bigTitleItem {
                text = strings["attestationsController.expiredSection.title"]
                identifier = "attestationsController.expiredSection.title".hashCode().toLong()
            }
            items += captionItem {
                text = strings["attestationsController.expiredSection.subtitle"]
                identifier = "attestationsController.expiredSection.subtitle".hashCode().toLong()
            }
            expiredAttestations.forEach { attestation ->
                items += qrCodeItemFromAttestation(attestation, false)
            }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.count().toLong()
            }
        }

        addMoreItems(items)

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
        return items
    }

    private fun addMoreItems(items: ArrayList<GenericItem>) {
        items += bigTitleItem {
            text = strings["attestationController.plusSection.title"]
            identifier = "attestationController.plusSection.title".hashCode().toLong()
            importantForAccessibility = ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        items += cardWithActionItem {
            actions = listOfNotNull(
                Action(R.drawable.ic_cgu, strings["attestationsController.termsOfUse"]) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(strings["attestationsController.termsOfUse.alert.title"])
                        .setMessage(strings["attestationsController.termsOfUse.alert.message"])
                        .setPositiveButton(strings["common.readMore"]) { _, _ ->
                            strings["attestationsController.termsOfUse.url"]?.openInExternalBrowser(requireContext())
                        }
                        .setNegativeButton(strings["common.ok"], null)
                        .show()
                },
                Action(R.drawable.ic_compass_light, strings["attestationsController.attestationWebSite"]) {
                    strings["home.moreSection.curfewCertificate.url"]?.openInExternalBrowser(it.context)
                }
            )
            identifier = "attestationsController.termsOfUse".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_small
        }

        items += captionItem {
            text = strings["attestationController.footer"]
            textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
            identifier = "attestationController.footer".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
    }

    private fun qrCodeItemFromAttestation(attestation: Attestation, allowShare: Boolean): AttestationCardItem {
        val generateBarcode = {
            barcodeEncoder.encodeBitmap(
                attestation.qrCode,
                BarcodeFormat.QR_CODE,
                qrCodeSize,
                qrCodeSize
            )
        }
        return attestationCardItem {
            this.generateBarcode = generateBarcode
            mainDescription = attestation.footer
            share = strings["attestationsController.menu.share"]
            delete = strings["attestationsController.menu.delete"]
            this.allowShare = allowShare
            onShare = { barcodeBitmap ->
                val uri = barcodeBitmap?.let { bitmap ->
                    ShareManager.getShareCaptureUriFromBitmap(requireContext(), bitmap, "qrCode")
                }
                val text = listOf(
                    attestation.qrCodeString,
                    strings["attestationsController.menu.share.text"]
                ).joinToString("\n\n")
                ShareManager.shareImageAndText(requireContext(), uri, text) {
                    strings["common.error.unknown"]?.let { showErrorSnackBar(it) }
                }
            }
            onDelete = {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["attestationsController.menu.delete.alert.title"])
                    .setMessage(strings["attestationsController.menu.delete.alert.message"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        viewModel.deleteAttestation(context, attestation)
                        refreshScreen()
                    }
                    .show()
            }
            onClick = {
                findNavControllerOrNull()?.safeNavigate(
                    AttestationsFragmentDirections.actionAttestationsFragmentToFullscreenAttestationFragment(
                        attestation.qrCode,
                        attestation.qrCodeString,
                    )
                )
            }
            actionContentDescription = strings["accessibility.hint.otherActions"]
            identifier = mainDescription.hashCode().toLong()
        }
    }

    private fun showMigrationFailedIfNeeded() {
        if (debugManager.oldAttestationsInSharedPrefs()) {
            context?.let {
                analyticsManager.reportErrorEvent(ErrorEventName.ERR_ATTESTATION_MIG)
                MaterialAlertDialogBuilder(it).showMigrationFailed(strings) {
                    viewModel.deleteDeprecatedAttestations()
                }
            }
        }
    }

    private fun showDbFailureIfNeeded(isRetry: Boolean) {
        lifecycleScope.launch {
            val result = context?.secureKeystoreDataSource()?.attestations()
            if (result is TacResult.Failure) {
                if (isRetry) {
                    analyticsManager.reportErrorEvent(ErrorEventName.ERR_ATTESTATION_DB_RETRY_FAILED)
                } else {
                    analyticsManager.reportErrorEvent(ErrorEventName.ERR_ATTESTATION_DB)
                }
                context?.let { ctx ->
                    MaterialAlertDialogBuilder(ctx)
                        .showDbFailure(
                            strings,
                            onRetry = {
                                lifecycleScope.launch {
                                    viewModel.forceRefreshAttestations()
                                    showDbFailureIfNeeded(true)
                                }
                            },
                            onClear = "android.db.error.clearAttestations" to { viewModel.deleteLostAttestations() }
                        )
                }
            } else if (isRetry && result is TacResult.Success) {
                analyticsManager.reportErrorEvent(ErrorEventName.ERR_ATTESTATION_DB_RETRY_SUCCEEDED)
            }
        }
    }
}
