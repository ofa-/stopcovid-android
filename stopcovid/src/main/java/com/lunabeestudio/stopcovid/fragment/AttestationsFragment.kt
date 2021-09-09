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
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.domain.model.Attestation
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.isExpired
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.fastitem.QrCodeCardItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.linkCardItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.fastitem.qrCodeCardItem
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.viewmodel.AttestationsViewModel
import com.lunabeestudio.stopcovid.viewmodel.AttestationsViewModelFactory
import com.lunabeestudio.stopcovid.widgetshomescreen.AttestationWidget
import com.mikepenz.fastadapter.GenericItem
import kotlin.time.ExperimentalTime

import com.lunabeestudio.stopcovid.viewmodel.NewAttestationViewModel
import com.lunabeestudio.stopcovid.viewmodel.NewAttestationViewModelFactory
import com.lunabeestudio.domain.model.FormEntry
import androidx.fragment.app.activityViewModels


class AttestationsFragment : MainFragment() {

    private val barcodeEncoder: BarcodeEncoder = BarcodeEncoder()
    private val qrCodeSize: Int by lazy {
        R.dimen.qr_code_size.toDimensSize(requireContext()).toInt()
    }

    private val robertManager: RobertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: AttestationsViewModel by viewModels { AttestationsViewModelFactory(requireContext().secureKeystoreDataSource()) }

    override fun getTitleKey(): String = "attestationsController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.attestations.observe(viewLifecycleOwner) {
            refreshScreen()
        }
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
            NewAttestationViewModelFactory(requireContext().secureKeystoreDataSource(), formManager)
        }
        navm.infos.put("datetime", FormEntry(System.currentTimeMillis().toString(), "datetime", Constants.Attestation.KEY_DATE_TIME))
        navm.infos.put("reason", FormEntry(reasonMap[typ], "list", Constants.Attestation.DATA_KEY_REASON))
        navm.generateQrCode(robertManager, strings)
        refreshScreen()
    }

    @OptIn(ExperimentalTime::class)
    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        items += linkCardItem {
            label = strings["attestationsController.newAttestation"]
            iconRes = R.drawable.ic_add
            onClickListener = View.OnClickListener {
                findNavControllerOrNull()?.safeNavigate(AttestationsFragmentDirections.actionAttestationsFragmentToNewAttestationFragment())
            }
            identifier = label.hashCode().toLong()
        }

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

        if (!validAttestations.isNullOrEmpty()) {
            items += bigTitleItem {
                text = strings["attestationsController.validAttestationsSection.title"]
                identifier = text.hashCode().toLong()
            }
            items += captionItem {
                text = strings["attestationsController.validAttestationsSection.subtitle"]
                identifier = text.hashCode().toLong()
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
                identifier = text.hashCode().toLong()
            }
            items += captionItem {
                text = strings["attestationsController.expiredSection.subtitle"]
                identifier = text.hashCode().toLong()
            }
            expiredAttestations.forEach { attestation ->
                items += qrCodeItemFromAttestation(attestation, false)
            }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.count().toLong()
            }
        }

        items += captionItem {
            text = strings["attestationController.footer"]
            textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
            identifier = text.hashCode().toLong()
        }
        items += linkItem {
            text = strings["attestationsController.attestationWebSite"]
            url = strings["home.moreSection.curfewCertificate.url"]
            identifier = text.hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        return items
    }

    private fun qrCodeItemFromAttestation(attestation: Attestation, allowShare: Boolean): QrCodeCardItem {
        val generateBarcode = {
            barcodeEncoder.encodeBitmap(
                attestation.qrCode,
                BarcodeFormat.QR_CODE,
                qrCodeSize,
                qrCodeSize
            )
        }
        return qrCodeCardItem {
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
                        viewModel.removeAttestation(attestation)
                        context?.let { AttestationWidget.updateWidget(it, false) }
                        refreshScreen()
                    }
                    .show()
            }
            onClick = {
                findNavControllerOrNull()?.safeNavigate(
                    AttestationsFragmentDirections.actionAttestationsFragmentToFullscreenQRCodeFragment(
                        attestation.qrCode,
                        BarcodeFormat.QR_CODE,
                        attestation.qrCodeString,
                    )
                )
            }
            actionContentDescription = strings["accessibility.hint.otherActions"]
            identifier = mainDescription.hashCode().toLong()
        }
    }
}
