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

import android.content.SharedPreferences
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
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.extension.formatOrNull
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLocale
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.collectDataWithLifecycle
import com.lunabeestudio.stopcovid.extension.countryCode
import com.lunabeestudio.stopcovid.extension.daysTo
import com.lunabeestudio.stopcovid.extension.fullDescription
import com.lunabeestudio.stopcovid.extension.fullNameList
import com.lunabeestudio.stopcovid.extension.fullNameUppercase
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.infosDescription
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.isAutoTest
import com.lunabeestudio.stopcovid.extension.isEligibleForSmartWallet
import com.lunabeestudio.stopcovid.extension.isExpired
import com.lunabeestudio.stopcovid.extension.isFrench
import com.lunabeestudio.stopcovid.extension.isSignatureExpired
import com.lunabeestudio.stopcovid.extension.navGraphWalletViewModels
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.raw
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.shortDateFormat
import com.lunabeestudio.stopcovid.extension.showErrorSnackBar
import com.lunabeestudio.stopcovid.extension.showSmartWallet
import com.lunabeestudio.stopcovid.extension.smartWalletState
import com.lunabeestudio.stopcovid.extension.statusStringKey
import com.lunabeestudio.stopcovid.extension.tagStringKey
import com.lunabeestudio.stopcovid.extension.testResultIsNegative
import com.lunabeestudio.stopcovid.extension.titleDescription
import com.lunabeestudio.stopcovid.extension.toCovidException
import com.lunabeestudio.stopcovid.extension.toRaw
import com.lunabeestudio.stopcovid.extension.vaccineDose
import com.lunabeestudio.stopcovid.extension.vaccineMedicinalProduct
import com.lunabeestudio.stopcovid.fastitem.CertificateCardItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.certificateCardItem
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.CertificateHeaderState
import com.lunabeestudio.stopcovid.model.Eligible
import com.lunabeestudio.stopcovid.model.EligibleSoon
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.ExpireSoon
import com.lunabeestudio.stopcovid.model.Expired
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.model.SanitaryCertificate
import com.lunabeestudio.stopcovid.model.SmartWalletState
import com.lunabeestudio.stopcovid.model.UnknownException
import com.lunabeestudio.stopcovid.model.VaccinationCertificate
import com.lunabeestudio.stopcovid.model.Valid
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.utils.lazyFast
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import com.lunabeestudio.stopcovid.worker.DccLightGenerationWorker
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt

import com.lunabeestudio.stopcovid.extension.showCertificateDetails

class WalletCertificateFragment : MainFragment(), PagerTabFragment {

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

    private val viewModel by navGraphWalletViewModels<WalletContainerFragment> {
        WalletViewModelFactory(
            robertManager,
            blacklistDCCManager,
            blacklist2DDOCManager,
            injectionContainer.walletRepository,
            injectionContainer.generateActivityPassUseCase,
            injectionContainer.getSmartWalletCertificateUseCase,
        )
    }

    private val longDateFormat by lazyFast { SimpleDateFormat.getDateInstance(DateFormat.LONG, getApplicationLocale()) }

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
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            (activity as? MainActivity)?.showProgress(isLoading)
        }

        viewModel.certificates.collectDataWithLifecycle(viewLifecycleOwner) {
            refreshScreen()
            debugManager.logObserveCertificate(it.toRaw())
        }
        viewModel.profileCertificates.collectDataWithLifecycle(viewLifecycleOwner) {
            refreshScreen()
        }
        viewModel.blacklistUpdateEvent.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }

        viewModel.scrollEvent.observeEventAndConsume(viewLifecycleOwner, false, Event.NO_ID, ::scrollToCertificate)
    }

    override fun refreshScreen() {
        super.refreshScreen()
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = true
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        val certificates = viewModel.certificates.value.data

        val blacklistedCertificates = viewModel.certificates.value.data?.filter { viewModel.isBlacklisted(it) } ?: emptyList()

        val hasInvalidDcc = {
            certificates?.any { certificate ->
                val testResultIsNotNegative = (certificate as? EuropeanCertificate)?.greenCertificate?.testResultIsNegative == false
                val isSignatureExpired = (certificate as? EuropeanCertificate)?.isSignatureExpired == true

                testResultIsNotNegative || isSignatureExpired
            } == true
        }

        val hasBlacklistedCertificate = { blacklistedCertificates.isNotEmpty() }

        val hasExpiredDcc = {
            sharedPrefs.showSmartWallet
                && robertManager.configuration.isSmartWalletOn
                && viewModel.profileCertificates.value?.any { (_, certificate) ->
                    val smartWalletState = certificate.smartWalletState(robertManager.configuration)
                    smartWalletState !is Valid
                } == true
        }

        val hasErrorCertificate = hasInvalidDcc() || hasBlacklistedCertificate() || hasExpiredDcc()

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
        viewModel.favoriteCertificates?.forEach { certificate ->
            items += certificateCardItemFromWalletDocument(certificate, blacklistedCertificates.contains(certificate))
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }
        }

        if (!viewModel.recentCertificates.isNullOrEmpty()) {
            viewModel.recentCertificates?.forEach { certificate ->
                items += certificateCardItemFromWalletDocument(certificate, blacklistedCertificates.contains(certificate))
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
            viewModel.olderCertificates?.forEach { certificate ->
                items += certificateCardItemFromWalletDocument(certificate, blacklistedCertificates.contains(certificate))
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
            val identifier = certificate.fastAdapterIdentifier()

            val position = fastAdapter.getItemById(identifier)?.second
            if (position != null) {
                binding?.recyclerView?.smoothScrollToPosition(position)
            } else {
                // Retry scroll once
                binding?.recyclerView?.postDelayed({
                    fastAdapter.getItemById(identifier)?.second?.let { position ->
                        binding?.recyclerView?.smoothScrollToPosition(position)
                    }
                }, 200)
            }
        }
    }

    override fun onTabSelected() {
        findParentFragmentByType<WalletContainerFragment>()?.let { walletContainerFragment ->
            walletContainerFragment.setupBottomAction(strings["walletController.addCertificate"]) {
                walletContainerFragment.findNavControllerOrNull()
                    ?.safeNavigate(WalletContainerFragmentDirections.actionWalletContainerFragmentToWalletQRCodeFragment())
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

    private suspend fun certificateCardItemFromWalletDocument(certificate: WalletCertificate, isBlacklisted: Boolean): CertificateCardItem {
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
           certificate.fullDescription(strings, robertManager.configuration, requireContext())
        } else { "" }

        val firstName = if (! sharedPrefs.showCertificateDetails) {
           certificate.fullNameUppercase().split(" ")[0]
        } else { "" }

        val greenCertificate = (certificate as? EuropeanCertificate)?.greenCertificate

        var smartWalletState: SmartWalletState? = null

        when (certificate) {
            is EuropeanCertificate -> {
                if (robertManager.configuration.isSmartWalletOn
                    && sharedPrefs.showSmartWallet
                    && (viewModel.profileCertificates.value?.values?.any { it.id == certificate.id } == true)
                ) {
                    smartWalletState = certificate.smartWalletState(robertManager.configuration)
                }
            }
        }

        val infoDescription = getInfoDescription(certificate, smartWalletState)
        val warningDescription = getWarningDescription(certificate, isBlacklisted, smartWalletState)
        val errorDescription = getErrorDescription(certificate, smartWalletState)
        val description = getDescription(certificate)

        val (headerState, headerText) = when {
            errorDescription.isNotBlank() -> Pair(CertificateHeaderState.ERROR, errorDescription)
            warningDescription.isNotBlank() -> Pair(CertificateHeaderState.WARNING, warningDescription)
            else -> Pair(CertificateHeaderState.INFO, infoDescription)
        }

        return certificateCardItem {
            this.generateBarcode = generateBarcode
            titleText = formatText
            nameText = firstName
            descriptionText = certificateDetails
            this.headerText = headerText
            this.headerState = headerState
            share = strings["walletController.menu.share"]
            delete = strings["walletController.menu.delete"]
            convertText = strings["walletController.menu.convertToEurope"]
            tag1Text = strings[certificate.tagStringKey()]
            tag2Text = strings[certificate.statusStringKey()]
            readMoreText = strings["common.readNext"]
            onReadMoreClick = {
                findParentFragmentByType<WalletContainerFragment>()?.findNavControllerOrNull()?.safeNavigate(
                    WalletContainerFragmentDirections.actionWalletContainerFragmentToSimpleTextBottomSheetFragment(headerText, headerState)
                )
            }

            when (certificate) {
                is EuropeanCertificate -> {
                    if (certificate.isExpired(robertManager.configuration)) {
                        tag2ColorRes = R.color.color_error
                    }
                }
            }

            //this.allowShare = false
            onShare = { barcodeBitmap ->
                findParentFragmentByType<WalletContainerFragment>()?.let { containerFragment ->
                    val uri = barcodeBitmap?.let { bitmap ->
                        ShareManager.getShareCaptureUriFromBitmap(requireContext(), bitmap, "qrCode")
                    }
                    val text = certificate.fullDescription(strings, robertManager.configuration, context).takeIf { uri != null }
                    ShareManager.setupCertificateSharingBottomSheet(containerFragment, text) {
                        uri
                    }
                    containerFragment.findNavControllerOrNull()?.safeNavigate(
                        WalletContainerFragmentDirections.actionWalletContainerFragmentToCertificateSharingBottomSheetFragment()
                    )
                }
            }
            onDelete = {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["walletController.menu.delete.alert.title"])
                    .setMessage(strings["walletController.menu.delete.alert.message"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        DccLightGenerationWorker.cancel(requireContext(), certificate.id)
                        viewModel.removeCertificate(certificate)
                        debugManager.logDeleteCertificates(certificate.raw, "from wallet")
                    }
                    .show()
            }
            onClick = {
                findParentFragmentByType<WalletContainerFragment>()?.navigateToFullscreenCertificate(certificate)
            }
            onConvert = conversionLambda
            actionContentDescription = strings["accessibility.hint.otherActions"]
            favoriteContentDescription = strings["accessibility.hint.addToFavorite"]

            favoriteState = when {
                certificate is FrenchCertificate -> CertificateCardItem.FavoriteState.HIDDEN
                certificate is EuropeanCertificate && certificate.isFavorite -> CertificateCardItem.FavoriteState.CHECKED
                else -> CertificateCardItem.FavoriteState.NOT_CHECKED
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

    private suspend fun getDescription(certificate: WalletCertificate): String {
        var description = certificate.infosDescription(strings, robertManager.configuration, context)
        val dateFormat = shortDateFormat(context)
        if (robertManager.configuration.isSmartWalletOn && sharedPrefs.showSmartWallet) {
            if (certificate.isEligibleForSmartWallet(blacklistDCCManager)) {
                (certificate as? EuropeanCertificate)
                    ?.smartWalletState(robertManager.configuration)
                    ?.expirationDate
                    ?.let { expirationDate ->
                        if (expirationDate.before(Date())) {
                            description += "\n\uD83D\uDD34 " + stringsFormat(
                                "walletController.certificateExpired",
                                dateFormat.format(expirationDate)
                            )
                        } else if (Date().daysTo(expirationDate) < (robertManager.configuration.smartWalletExp?.displayExpOnAllDcc ?: 0)) {
                            description += "\n" + stringsFormat("walletController.certificateExpiration", dateFormat.format(expirationDate))
                        }
                    }
            }
        }

        return description
    }

    private fun getInfoDescription(certificate: WalletCertificate, smartWalletState: SmartWalletState?): String {
        val eligibilityInfo = when (smartWalletState) {
            is Eligible -> {
                (certificate as? EuropeanCertificate)?.greenCertificate?.vaccineMedicinalProduct?.let { vaccineProduct ->
                    strings["smartWallet.elegibility.info.${certificate.type.code}.$vaccineProduct"]
                } ?: strings["smartWallet.elegibility.info.${certificate.type.code}"]
            }
            is EligibleSoon -> {
                val label = (certificate as? EuropeanCertificate)?.greenCertificate?.vaccineMedicinalProduct?.let { vaccineProduct ->
                    strings["smartWallet.elegibility.soon.info.${certificate.type.code}.$vaccineProduct"]
                } ?: strings["smartWallet.elegibility.soon.info.${certificate.type.code}"]

                label.formatOrNull(longDateFormat.format(smartWalletState.eligibleDate ?: Date()))
            }
            else -> {
                null
            }
        }

        val greenCertificate = (certificate as? EuropeanCertificate)?.greenCertificate
        val foreignCountryInfo = if (greenCertificate?.isFrench == false) {
            strings["wallet.proof.europe.foreignCountryWarning.${greenCertificate.countryCode?.lowercase()}"]
        } else {
            null
        }
        return listOfNotNull(
            eligibilityInfo,
            foreignCountryInfo,
        )
            .joinToString("\n\n")
    }

    private fun getWarningDescription(
        certificate: WalletCertificate,
        isBlacklisted: Boolean,
        smartWalletState: SmartWalletState?
    ): String {
        val expirationWarning = if (smartWalletState is ExpireSoon) {
            val label = (certificate as? EuropeanCertificate)?.greenCertificate?.vaccineMedicinalProduct?.let { vaccineProduct ->
                strings["smartWallet.expiration.soon.warning.${certificate.type.code}.$vaccineProduct"]
            } ?: strings["smartWallet.expiration.soon.warning.${certificate.type.code}"]

            label.formatOrNull(longDateFormat.format(smartWalletState.expirationDate ?: Date()))
        } else {
            null
        }
        val blacklistWarning = if (isBlacklisted) {
            strings["wallet.blacklist.warning"]
        } else {
            null
        }

        // Fix SIDEP has generated positive test instead of recovery
        val greenCertificate = (certificate as? EuropeanCertificate)?.greenCertificate
        val positiveSidepErrorWarning = if (greenCertificate?.testResultIsNegative == false) {
            strings["wallet.proof.europe.test.positiveSidepError"]
        } else {
            null
        }
        val autotestWarning = if (greenCertificate?.isAutoTest == true) {
            strings["wallet.autotest.warning"]
        } else {
            null
        }
        return listOfNotNull(
            expirationWarning,
            blacklistWarning,
            positiveSidepErrorWarning,
            autotestWarning,
        )
            .joinToString("\n\n")
    }

    private fun getErrorDescription(
        certificate: WalletCertificate,
        smartWalletState: SmartWalletState?,
    ): String {
        val expirationError = if (smartWalletState is Expired) {
            val label = (certificate as? EuropeanCertificate)?.greenCertificate?.vaccineMedicinalProduct?.let { vaccineProduct ->
                strings["smartWallet.expiration.error.${certificate.type.code}.$vaccineProduct"]
            } ?: strings["smartWallet.expiration.error.${certificate.type.code}"]

            label.formatOrNull(longDateFormat.format(smartWalletState.expirationDate ?: Date()))
        } else {
            null
        }
        return listOfNotNull(
            expirationError,
        )
            .joinToString("\n\n")
    }

    private fun showConversionConfirmationAlert(certificate: FrenchCertificate) {
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

    private fun requestCertificateConversion(certificate: FrenchCertificate) {
        lifecycleScope.launch {
            showLoading(strings["walletController.convertCertificate.loading"])
            val result = viewModel.convert2ddocToDcc(certificate)
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
            val certificate = injectionContainer.verifyAndGetCertificateCodeValueUseCase(
                certificateCode,
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
