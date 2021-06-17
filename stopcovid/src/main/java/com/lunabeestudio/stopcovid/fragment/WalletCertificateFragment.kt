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

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.barcodeFormat
import com.lunabeestudio.stopcovid.extension.fullDescription
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.shortDescription
import com.lunabeestudio.stopcovid.extension.statusStringKey
import com.lunabeestudio.stopcovid.extension.tagStringKey
import com.lunabeestudio.stopcovid.extension.vaccineDose
import com.lunabeestudio.stopcovid.fastitem.QrCodeCardItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.qrCodeCardItem
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.VaccinationCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlin.time.ExperimentalTime

class WalletCertificateFragment : QRCodeListFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val keystoreDataSource by lazy {
        requireContext().secureKeystoreDataSource()
    }

    private val viewModel: WalletViewModel by viewModels(
        {
            findParentFragmentByType<WalletContainerFragment>() ?: requireParentFragment()
        },
        {
            WalletViewModelFactory(robertManager, keystoreDataSource)
        }
    )

    override fun getTitleKey(): String = "walletController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.certificates.observe(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    override fun refreshScreen() {
        super.refreshScreen()
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = true
    }

    @OptIn(ExperimentalTime::class)
    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        if (!viewModel.recentCertificates.isNullOrEmpty()) {
            items += bigTitleItem {
                text = strings["walletController.recentCertificatesSection.title"]
                identifier = text.hashCode().toLong()
            }
            items += captionItem {
                text = strings["walletController.recentCertificatesSection.subtitle"]
                identifier = text.hashCode().toLong()
            }
            viewModel.recentCertificates?.forEach { certificate ->
                items += codeItemFromWalletDocument(certificate)
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.size.toLong()
            }
        }

        if (!viewModel.olderCertificates.isNullOrEmpty()) {
            items += bigTitleItem {
                text = strings["walletController.oldCertificatesSection.title"]
                identifier = text.hashCode().toLong()
            }
            items += captionItem {
                text = strings["walletController.oldCertificatesSection.subtitle"]
                identifier = text.hashCode().toLong()
            }
            viewModel.olderCertificates?.forEach { certificate ->
                items += codeItemFromWalletDocument(certificate)
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.size.toLong()
            }
        }

        return items
    }

    private fun codeItemFromWalletDocument(certificate: WalletCertificate): QrCodeCardItem {
        val bitmap: Bitmap
        val formatText: String?

        when (certificate) {
            is FrenchCertificate -> {
                bitmap = barcodeEncoder.encodeBitmap(
                    certificate.value,
                    BarcodeFormat.DATA_MATRIX,
                    qrCodeSize,
                    qrCodeSize
                )
                formatText = Constants.QrCode.FORMAT_2D_DOC
            }
            is EuropeanCertificate -> {
                bitmap = barcodeEncoder.encodeBitmap(
                    certificate.value,
                    BarcodeFormat.QR_CODE,
                    qrCodeSize,
                    qrCodeSize
                )
                formatText = null
            }
        }

        return qrCodeCardItem {
            qrCodeBitmap = bitmap
            text = certificate.fullDescription(strings, robertManager.configuration)
            share = strings["walletController.menu.share"]
            delete = strings["walletController.menu.delete"]
            this.formatText = formatText
            tag1Text = strings[certificate.tagStringKey()]

            if (certificate is VaccinationCertificate) {
                tag2Text = strings[certificate.statusStringKey()]
            } else if ((certificate as? EuropeanCertificate)?.type == WalletCertificateType.VACCINATION_EUROPE) {
                certificate.greenCertificate.vaccineDose?.let { (first, second) ->
                    tag2Text = stringsFormat("wallet.proof.europe.vaccine.doses", first, second)
                }
            }

            this.allowShare = true
            onShare = {
                val uri = ShareManager.getShareCaptureUriFromBitmap(requireContext(), bitmap, "qrCode")
                val text = text
                ShareManager.shareImageAndText(requireContext(), uri, text) {
                    strings["common.error.unknown"]?.let { showErrorSnackBar(it) }
                }
            }
            onDelete = {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["walletController.menu.delete.alert.title"])
                    .setMessage(strings["walletController.menu.delete.alert.message"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        viewModel.removeCertificate(certificate)
                        refreshScreen()
                    }
                    .show()
            }
            onClick = {
                findParentFragmentByType<WalletContainerFragment>()?.findNavControllerOrNull()?.safeNavigate(
                    WalletContainerFragmentDirections.actionWalletContainerFragmentToFullscreenQRCodeFragment(
                        certificate.value,
                        certificate.type.barcodeFormat,
                        certificate.shortDescription()
                    )
                )
            }
            actionContentDescription = strings["accessibility.hint.otherActions"]
            identifier = certificate.value.hashCode().toLong()
        }
    }
}
