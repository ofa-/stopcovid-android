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
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ImageSpan
import android.text.util.Linkify
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.toSpannable
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.barcodeFormat
import com.lunabeestudio.stopcovid.extension.countryCode
import com.lunabeestudio.stopcovid.extension.dccCertificatesManager
import com.lunabeestudio.stopcovid.extension.fullDescription
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.isAutoTest
import com.lunabeestudio.stopcovid.extension.isFrench
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.raw
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.shortDescription
import com.lunabeestudio.stopcovid.extension.showUnknownErrorAlert
import com.lunabeestudio.stopcovid.extension.statusStringKey
import com.lunabeestudio.stopcovid.extension.tagStringKey
import com.lunabeestudio.stopcovid.extension.testResultIsNegative
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.extension.vaccineDose
import com.lunabeestudio.stopcovid.fastitem.QrCodeCardItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.qrCodeCardItem
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.SanitaryCertificate
import com.lunabeestudio.stopcovid.model.UnknownException
import com.lunabeestudio.stopcovid.model.VaccinationCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.lunabeestudio.stopcovid.extension.showCertificateDetails

class WalletCertificateFragment : MainFragment() {

    private val barcodeEncoder: BarcodeEncoder = BarcodeEncoder()
    private val qrCodeSize: Int by lazy {
        R.dimen.qr_code_size.toDimensSize(requireContext()).toInt()
    }

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val dccCertificatesManager by lazy {
        requireContext().dccCertificatesManager()
    }

    private val viewModel: WalletViewModel by viewModels(
        {
            findParentFragmentByType<WalletContainerFragment>() ?: requireParentFragment()
        },
        {
            WalletViewModelFactory(robertManager, blacklistDCCManager, blacklist2DDOCManager, walletRepository)
        }
    )

    override fun getTitleKey(): String = "walletController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.error.observe(viewLifecycleOwner) {
            showErrorSnackBar(
                when (it) {
                    is RobertException -> it.toCovidException().getString(strings)
                    else -> UnknownException(it.message ?: "").getString(strings)
                }
            )
        }
        viewModel.certificates.observe(viewLifecycleOwner) {
            lifecycleScope.launch {
                debugManager.logObserveCertificate(it?.map { certificate -> certificate.raw })
            }
            refreshScreen()
        }
        viewModel.blacklistDCC.observe(viewLifecycleOwner) {
            refreshScreen()
        }
        viewModel.blacklist2DDOC.observe(viewLifecycleOwner) {
            refreshScreen()
        }

        viewModel.scrollEvent.observeEventAndConsume(viewLifecycleOwner, ::scrollToCertificate)
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

        val hasErrorCertificate = viewModel.certificates.value?.any { certificate ->
            (certificate as? EuropeanCertificate)?.greenCertificate?.testResultIsNegative == false
                || viewModel.isBlacklisted(certificate)
        } == true
        if (hasErrorCertificate) {
            items += captionItem {
                text = strings["walletController.certificateWarning"]
                identifier = text.hashCode().toLong()
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.size.toLong()
            }
        }

        if (!viewModel.favoriteCertificates.isNullOrEmpty()) {
        items += bigTitleItem {
            text = strings["walletController.favoriteCertificateSection.title"]
            identifier = text.hashCode().toLong()
        }
        viewModel.favoriteCertificates?.forEach { certificate ->
            items += codeItemFromWalletDocument(certificate)
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }
        }

        if (!viewModel.recentCertificates.isNullOrEmpty()) {
            items += bigTitleItem {
                text = strings["walletController.recentCertificatesSection.title"]
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

    private fun scrollToCertificate(certificate: WalletCertificate) {
        binding?.recyclerView?.doOnNextLayout {
            fastAdapter.getItemById(certificate.fastAdapterIdentifier())?.second?.let { position ->
                binding?.recyclerView?.smoothScrollToPosition(position)
            }
        }
    }

    private fun transformHeartEmoji(spannedSubtitle: Spannable?) {
        val heartIndex = spannedSubtitle?.indexOf("❤️") ?: -1
        if (heartIndex >= 0) {
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_empty_heart)?.let { drawable ->
                val textHeight = R.dimen.caption_font_size.toDimensSize(requireContext())
                val ratio = textHeight / drawable.intrinsicHeight.toFloat()
                drawable.setBounds(
                    0,
                    0,
                    (drawable.intrinsicWidth * ratio).roundToInt(),
                    (drawable.intrinsicHeight * ratio).roundToInt()
                )
                spannedSubtitle
                    ?.setSpan(
                        ImageSpan(drawable),
                        heartIndex,
                        heartIndex + 1,
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE,
                    )
            }
        }
    }

    private fun codeItemFromWalletDocument(certificate: WalletCertificate): QrCodeCardItem {
        val barcodeFormat = when (certificate) {
            is FrenchCertificate -> BarcodeFormat.DATA_MATRIX
            is EuropeanCertificate -> BarcodeFormat.QR_CODE
        }
        val generateBarcode = { barcodeEncoder.encodeBitmap(
            certificate.value,
            barcodeFormat,
            qrCodeSize,
            qrCodeSize
        )}
        val formatText = when (certificate) {
            is FrenchCertificate -> Constants.QrCode.FORMAT_2D_DOC
            is EuropeanCertificate -> "🇪🇺 EU-DGC 🇪🇺"
        }
        val conversionLambda = when (certificate) {
            is FrenchCertificate -> { ->
                        showConversionConfirmationAlert(certificate)
            }
            is EuropeanCertificate -> null
        }
        val certificateDetails = if (sharedPrefs.showCertificateDetails) {
           certificate.fullDescription(strings, robertManager.configuration)
        } else { "" }

        return qrCodeCardItem {
            this.generateBarcode = generateBarcode
            mainDescription = certificateDetails
            val greenCertificate = (certificate as? EuropeanCertificate)?.greenCertificate
            footerDescription = when {
                viewModel.isBlacklisted(certificate) -> strings["wallet.blacklist.warning"]?.toSpannable()
                greenCertificate == null -> null
                greenCertificate.testResultIsNegative == false -> {
                    // Fix SIDEP has generated positive test instead of recovery
                    strings["wallet.proof.europe.test.positiveSidepError"]?.toSpannable()?.also {
                        Linkify.addLinks(it, Linkify.WEB_URLS)
                    }
                }
                greenCertificate.isAutoTest -> strings["wallet.autotest.warning"]?.toSpannable()
                !greenCertificate.isFrench ->
                    strings["wallet.proof.europe.foreignCountryWarning.${greenCertificate.countryCode?.lowercase()}"]?.toSpannable()
                else -> null
            }
            share = strings["walletController.menu.share"]
            delete = strings["walletController.menu.delete"]
            convertText = strings["walletController.menu.convertToEurope"]
            this.formatText = formatText
            tag1Text = strings[certificate.tagStringKey()]
            tag2Text = strings[certificate.statusStringKey()]

            this.allowShare = true
            onShare = { barcodeBitmap ->
                val uri = barcodeBitmap?.let { bitmap ->
                    ShareManager.getShareCaptureUriFromBitmap(requireContext(), bitmap, "qrCode")
                }
                val text = mainDescription.takeIf { uri != null }
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
                        lifecycleScope.launch {
                            debugManager.logDeleteCertificates(certificate.raw, "from wallet")
                        }
                        refreshScreen()
                    }
                    .show()
            }
            onClick = {
                if (certificate is EuropeanCertificate) {
                    findParentFragmentByType<WalletContainerFragment>()?.findNavControllerOrNull()?.safeNavigate(
                        WalletContainerFragmentDirections.actionWalletContainerFragmentToFullscreenDccFragment(
                            id = certificate.id,
                        )
                    )
                } else {
                    findParentFragmentByType<WalletContainerFragment>()?.findNavControllerOrNull()?.safeNavigate(
                        WalletContainerFragmentDirections.actionWalletContainerFragmentToFullscreenQRCodeFragment(
                            certificate.value,
                            certificate.type.barcodeFormat,
                            certificate.shortDescription(),
                            certificate.sha256,
                        )
                    )
                }
            }
            onConvert = conversionLambda
            actionContentDescription = strings["accessibility.hint.otherActions"]
            favoriteContentDescription = strings["accessibility.hint.addToFavorite"]

            favoriteState = when {
                certificate is FrenchCertificate -> QrCodeCardItem.FavoriteState.HIDDEN
                certificate is EuropeanCertificate && certificate.isFavorite -> QrCodeCardItem.FavoriteState.CHECKED
                else -> QrCodeCardItem.FavoriteState.NOT_CHECKED
            }

            onFavoriteClick = {
                if ((certificate as? EuropeanCertificate)?.isFavorite != true) {
                    binding?.recyclerView?.doOnNextLayout {
                        binding?.recyclerView?.smoothScrollToPosition(0)
                    }
                }

                (certificate as? EuropeanCertificate)?.let(viewModel::toggleFavorite)
            }

            identifier = certificate.fastAdapterIdentifier()
        }
    }

    private fun showConversionConfirmationAlert(certificate: WalletCertificate) {
        if ((certificate as? SanitaryCertificate)?.analysisCode in robertManager.configuration.certificateConversionSidepOnlyCode) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(strings["walletController.convertCertificate.antigenicAlert.title"])
                .setMessage(strings["walletController.convertCertificate.antigenicAlert.message"])
                .setNegativeButton(strings["common.ok"], null)
                .setPositiveButton(strings["walletController.convertCertificate.antigenicAlert.link"]) { _, _ ->
                    strings["walletController.convertCertificate.antigenicAlert.urlLink"]?.openInExternalBrowser(requireContext())
                }
                .show()
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(strings["walletController.menu.convertToEurope.alert.title"])
                .setMessage(strings["walletController.menu.convertToEurope.alert.message"])
                .setPositiveButton(strings["common.ok"]) { _, _ ->
                    requestCertificateConversion(certificate)
                }
                .setNegativeButton(strings["common.cancel"], null)
                .setNeutralButton(strings["walletController.menu.convertToEurope.alert.terms"]) { _, _ ->
                    strings["walletController.menu.convertToEurope.alert.termsUrl"]?.openInExternalBrowser(requireContext())
                }
                .show()
        }
    }

    private fun requestCertificateConversion(certificate: WalletCertificate) {
        lifecycleScope.launch {
            showLoading(strings["walletController.convertCertificate.loading"])
            val result = (activity?.application as? StopCovid)?.injectionContainer?.walletRepository?.convertCertificate(
                robertManager,
                certificate.raw,
                WalletCertificateType.Format.WALLET_DCC
            )

            when (result) {
                is RobertResultData.Failure -> showConversionFailedAlert()
                is RobertResultData.Success -> {
                    if (processConvertedCertificate(result.data, WalletCertificateType.Format.WALLET_DCC)) {
                        viewModel.removeCertificate(certificate)
                        debugManager.logDeleteCertificates(certificate.raw, "after conversion succeeded")
                    } else {
                        showConversionFailedAlert()
                    }
                }
                null -> showUnknownErrorAlert(null)
            }

            showData()
        }
    }

    private fun showConversionFailedAlert() {
        val message = listOfNotNull(
            "${strings["walletController.convertCertificate.error.message"]}".takeIf { it.isNotEmpty() },
            "${strings["walletController.convertCertificate.error.url1"]}".takeIf { it.isNotEmpty() },
            "${strings["walletController.convertCertificate.error.url2"]}".takeIf { it.isNotEmpty() },
        ).joinToString("\n\n")

        val span = SpannableString(message)
        Linkify.addLinks(span, Linkify.WEB_URLS)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(strings["common.error.unknown"])
            .setMessage(span)
            .setPositiveButton(strings["common.ok"], null)
            .show()
            .findViewById<TextView>(android.R.id.message)?.apply {
                movementMethod = LinkMovementMethod.getInstance()
            }
    }

    private suspend fun processConvertedCertificate(
        certificateCode: String,
        certificateFormat: WalletCertificateType.Format?,
    ): Boolean {
        return try {
            val certificate = walletRepository.verifyAndGetCertificateCodeValue(
                robertManager.configuration,
                certificateCode,
                dccCertificatesManager.certificates,
                certificateFormat,
            )

            viewModel.saveCertificate(certificate)
            debugManager.logSaveCertificates(certificate.raw, "from conversion")

            val vaccination = (certificate as? EuropeanCertificate)?.greenCertificate?.vaccinations?.lastOrNull()
            if (vaccination != null && vaccination.doseNumber >= vaccination.totalSeriesOfDoses) {
                findParentFragmentByType<WalletContainerFragment>()?.findNavControllerOrNull()?.safeNavigate(
                    WalletContainerFragmentDirections.actionWalletContainerFragmentToVaccineCompletionFragment(
                        certificate.id
                    )
                )
            } else {
                strings["walletController.convertCertificate.conversionSucceeded"]?.let {
                    (activity as? MainActivity)?.showSnackBar(it)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun WalletCertificate.fastAdapterIdentifier(): Long = id.hashCode().toLong()
}
