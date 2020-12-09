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
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.attestationLongLabelFromKey
import com.lunabeestudio.stopcovid.extension.attestationShortLabelFromKey
import com.lunabeestudio.stopcovid.extension.isExpired
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.fastitem.AttestationQrCodeItem
import com.lunabeestudio.stopcovid.fastitem.attestationQrCodeItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.linkCardItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.AttestationMap
import com.lunabeestudio.stopcovid.viewmodel.AttestationsViewModel
import com.lunabeestudio.stopcovid.viewmodel.AttestationsViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.time.ExperimentalTime

import com.lunabeestudio.stopcovid.viewmodel.NewAttestationViewModel
import com.lunabeestudio.stopcovid.viewmodel.NewAttestationViewModelFactory
import com.lunabeestudio.domain.model.FormEntry
import androidx.fragment.app.activityViewModels


class AttestationsFragment : MainFragment() {

    private val robertManager: RobertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: AttestationsViewModel by viewModels { AttestationsViewModelFactory(requireContext().secureKeystoreDataSource()) }

    private val dateFormat: DateFormat = SimpleDateFormat.getDateInstance(DateFormat.SHORT)
    private val timeFormat: DateFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT)
    private val barcodeEncoder = BarcodeEncoder()
    private val qrCodeSize by lazy {
        R.dimen.qr_code_size.toDimensSize(requireContext()).toInt()
    }

    override fun getTitleKey(): String = "attestationsController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.attestations.observe(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    val reasonMap = mapOf(
        1 to "travail",
        2 to "achats_culturel_cultuel",
        6 to "sport_animaux",
        9 to "enfants",
    )
    val labelMap = mapOf(
        1 to "Travail",
        2 to "Achats",
        6 to "Plein air",
        9 to "Enfants",
    )

    private fun generateNewAttestation(typ: Int) {
        val navm: NewAttestationViewModel by activityViewModels {
            NewAttestationViewModelFactory(requireContext().secureKeystoreDataSource())
        }
        navm.infos.put("datetime", FormEntry(System.currentTimeMillis().toString(), "datetime"))
        navm.infos.put("reason", FormEntry(reasonMap[typ], "list"))
        navm.generateQrCode()
        refreshScreen()
    }

    @OptIn(ExperimentalTime::class)
    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += linkCardItem {
            label = strings["attestationsController.newAttestation"]
            iconRes = R.drawable.ic_add
            onClickListener = View.OnClickListener {
                findNavController().navigate(AttestationsFragmentDirections.actionAttestationsFragmentToNewAttestationFragment())
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
        items += linkCardItem {
            label = strings["attestationsController.termsOfUse"]
            iconRes = R.drawable.ic_cgu
            onClickListener = View.OnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["attestationsController.termsOfUse.alert.title"])
                    .setMessage(strings["attestationsController.termsOfUse.alert.message"])
                    .setPositiveButton(strings["common.readMore"]) { _, _ ->
                        strings["attestationsController.termsOfUse.url"]?.openInExternalBrowser(requireContext())
                    }
                    .setNegativeButton(strings["common.ok"], null)
                    .show()
            }
            identifier = label.hashCode().toLong()
        }
        val validAttestations = viewModel.attestations.value?.filter { attestation ->
            !attestation.isExpired(robertManager)
        }?.sortedByDescending { attestation ->
            attestation["datetime"]?.value?.toLongOrNull() ?: 0L
        }
        val expiredAttestations = viewModel.attestations.value?.filter { attestation ->
            attestation.isExpired(robertManager)
        }?.sortedByDescending { attestation ->
            attestation["datetime"]?.value?.toLongOrNull() ?: 0L
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
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["attestationController.footer"]
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

    private fun qrCodeItemFromAttestation(attestation: AttestationMap, allowShare: Boolean): AttestationQrCodeItem {
        val bitmap = barcodeEncoder.encodeBitmap(
            attestationToFormattedString(attestation),
            BarcodeFormat.QR_CODE,
            qrCodeSize,
            qrCodeSize
        )
        return attestationQrCodeItem {
            qrCodeBitmap = bitmap
            text = attestationToFooterString(attestation)
            share = strings["attestationsController.menu.share"]
            delete = strings["attestationsController.menu.delete"]
            this.allowShare = allowShare
            onShare = {
                val uri = ShareManager.getShareCaptureUriFromBitmap(requireContext(), bitmap, "qrCode")
                val text = listOf(
                    attestationToFormattedStringDisplayed(attestation),
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
                        refreshScreen()
                    }
                    .show()
            }
            onClick = {
                findNavController().navigate(
                    AttestationsFragmentDirections.actionAttestationsFragmentToFullscreenAttestationFragment(
                        attestationToFormattedString(attestation),
                        attestationToFormattedStringDisplayed(attestation)
                    )
                )
            }
            actionContentDescription = strings["accessibility.hint.otherActions"]
            identifier = text.hashCode().toLong()
        }
    }

    private fun attestationToFormattedString(attestation: AttestationMap): String {
        return robertManager.qrCodeFormattedString
            .attestationReplaceKnownValue(attestation)
            .attestationReplaceUnknownValues()
    }

    private fun attestationToFormattedStringDisplayed(attestation: AttestationMap): String {
        return robertManager.qrCodeFormattedStringDisplayed
            .attestationReplaceKnownValue(attestation)
            .attestationReplaceUnknownValues()
    }

    private fun attestationToFooterString(attestation: AttestationMap): String {
        return robertManager.qrCodeFooterString
            .attestationReplaceKnownValue(attestation)
            .attestationReplaceUnknownValues()
    }

    private fun String.attestationReplaceKnownValue(attestation: AttestationMap): String {
        var result = this
        attestation.keys.forEach { key ->
            when (attestation[key]?.type) {
                "date" -> {
                    attestation[key]?.value?.toLongOrNull()?.let { timestamp ->
                        val date = Date(timestamp)
                        result = result.replace("<$key>", dateFormat.format(date))
                    }
                }
                "datetime" -> {
                    attestation[key]?.value?.toLongOrNull()?.let { timestamp ->
                        val date = Date(timestamp)
                        result = result.replace("<$key>", "${dateFormat.format(date)}, ${timeFormat.format(date)}")
                            .replace("<$key-day>", dateFormat.format(date))
                            .replace("<$key-hour>", timeFormat.format(date))
                    }
                }
                "list" -> {
                    result = result.replace("<$key>", attestation[key]?.value ?: strings["qrCode.infoNotAvailable"] ?: "")
                        .replace("<$key-code>", attestation[key]?.value ?: strings["qrCode.infoNotAvailable"] ?: "")
                        .replace(
                            "<$key-shortlabel>",
                            strings[attestation[key]?.value?.attestationShortLabelFromKey()] ?: strings["qrCode.infoNotAvailable"] ?: ""
                        )
                        .replace(
                            "<$key-longlabel>",
                            strings[attestation[key]?.value?.attestationLongLabelFromKey()] ?: strings["qrCode.infoNotAvailable"] ?: ""
                        )
                }
                else -> result = result.replace("<$key>", attestation[key]?.value ?: strings["qrCode.infoNotAvailable"] ?: "")
            }
        }
        return result
    }

    private fun String.attestationReplaceUnknownValues(): String = replace(
        regex = Regex("<[a-zA-Z0-9\\-]+>"),
        strings["qrCode.infoNotAvailable"] ?: ""
    )
}
