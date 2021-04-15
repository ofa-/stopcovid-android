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
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.BuildConfig
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.catchWalletException
import com.lunabeestudio.stopcovid.extension.fullDescription
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.shortDescription
import com.lunabeestudio.stopcovid.extension.tagStringKey
import com.lunabeestudio.stopcovid.fastitem.QrCodeCardItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.linkCardItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.fastitem.qrCodeCardItem
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.manager.WalletManager
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificateInvalidSignatureException
import com.lunabeestudio.stopcovid.model.WalletCertificateMalformedException
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import timber.log.Timber
import kotlin.time.ExperimentalTime

class WalletFragment : QRCodeListFragment() {

    private val args: WalletFragmentArgs by navArgs()

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val keystoreDataSource by lazy {
        requireContext().secureKeystoreDataSource()
    }

    private val viewModel: WalletViewModel by viewModels {
        WalletViewModelFactory(
            robertManager,
            keystoreDataSource
        )
    }

    private var confirmationAsked: Boolean = false

    override fun getTitleKey(): String = "walletController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.certificates.observe(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(CONFIRMATION_ASKED_KEY, confirmationAsked)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

/*
        if (!robertManager.configuration.displaySanitaryCertificatesWallet) {
            findNavControllerOrNull()?.navigateUp()
        }
*/

        confirmationAsked = savedInstanceState?.getBoolean(CONFIRMATION_ASKED_KEY) == true

        if (args.certificateCode != null && !confirmationAsked) {
            if (checkCodeValue(args.certificateCode!!)) {
                findNavControllerOrNull()?.safeNavigate(
                    WalletFragmentDirections.actionWalletFragmentToConfirmAddWalletCertificateFragment(
                        args.certificateCode!!
                    )
                )
                confirmationAsked = true
            }
        }

        setFragmentResultListener(SCANNED_CODE_RESULT_KEY) { _, bundle ->
            val url = bundle.getString(SCANNED_CODE_BUNDLE_KEY)
            url?.let { processUrlValue(it) }
        }

        setFragmentResultListener(CONFIRM_ADD_CODE_RESULT_KEY) { _, bundle ->
            if (bundle.getBoolean(CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM) && bundle.getString(CONFIRM_ADD_CODE_BUNDLE_KEY_CODE) != null) {
                processCodeValue(bundle.getString(CONFIRM_ADD_CODE_BUNDLE_KEY_CODE)!!)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        if (viewModel.isEmpty()) {
            items += logoItem {
                imageRes = R.drawable.wallet
                identifier = R.drawable.wallet.toLong()
            }

            items += cardWithActionItem {
                mainTitle = strings["walletController.explanations.title"]
                mainBody = strings["walletController.explanations.subtitle"]
                identifier = mainBody.hashCode().toLong()
            }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        items += linkCardItem {
            label = strings["walletController.flashButton.title"]
            iconRes = R.drawable.ic_add
            onClickListener = View.OnClickListener {
                findNavControllerOrNull()?.safeNavigate(WalletFragmentDirections.actionWalletFragmentToWalletQRFragment())
            }
            identifier = label.hashCode().toLong()
        }

        items += captionItem {
            textAppearance = R.style.TextAppearance_StopCovid_Caption_Small_Grey
            text = strings["walletController.flashExplanation"]
        }

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
        val bitmap = barcodeEncoder.encodeBitmap(
            certificate.value,
            BarcodeFormat.DATA_MATRIX,
            qrCodeSize,
            qrCodeSize
        )
        return qrCodeCardItem {
            qrCodeBitmap = bitmap
            text = certificate.fullDescription(strings, robertManager.configuration)
            share = strings["walletController.menu.share"]
            delete = strings["walletController.menu.delete"]
            formatText = Constants.QrCode.FORMAT_2D_DOC
            tagText = strings[certificate.tagStringKey()]
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
                findNavControllerOrNull()?.safeNavigate(
                    WalletFragmentDirections.actionWalletFragmentToFullscreenQRCodeFragment(
                        certificate.value,
                        BarcodeFormat.DATA_MATRIX,
                        certificate.shortDescription()
                    )
                )
            }
            actionContentDescription = strings["accessibility.hint.otherActions"]
            identifier = certificate.value.hashCode().toLong()
        }
    }

    private fun processUrlValue(url: String) {
        try {
            val certificateCode = WalletManager.extractCertificateCodeFromUrl(url)
            processCodeValue(certificateCode)
        } catch (e: Exception) {
            catchWalletException(e)
        }
    }

    private fun checkCodeValue(certificateCode: String): Boolean {
        return try {
            WalletManager.verifyCertificateCodeValue(sharedPreferences, robertManager.configuration, certificateCode)
            true
        } catch (e: Exception) {
            catchWalletException(e)
            false
        }
    }

    private fun processCodeValue(certificateCode: String) {
        try {
            WalletManager.processCertificateCode(sharedPreferences, robertManager, keystoreDataSource, certificateCode)
        } catch (e: Exception) {
            catchWalletException(e)
        }
    }

    companion object {
        const val SCANNED_CODE_RESULT_KEY: String = "SCANNED_CODE_RESULT_KEY"
        const val SCANNED_CODE_BUNDLE_KEY: String = "SCANNED_CODE_BUNDLE_KEY"
        const val CONFIRM_ADD_CODE_RESULT_KEY: String = "CONFIRM_ADD_CODE_RESULT_KEY"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_CODE: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_CODE"

        const val CONFIRMATION_ASKED_KEY: String = "confirmationAsked"
    }

}
