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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.extension.safeEnumValueOf
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentWalletContainerBinding
import com.lunabeestudio.stopcovid.extension.dccCertificatesManager
import com.lunabeestudio.stopcovid.extension.isFrench
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showUnknownErrorAlert
import com.lunabeestudio.stopcovid.extension.splitUrlFragment
import com.lunabeestudio.stopcovid.extension.walletCertificateError
import com.lunabeestudio.stopcovid.manager.DeeplinkManager
import com.lunabeestudio.stopcovid.manager.WalletManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificateMalformedException
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import kotlinx.coroutines.launch

class WalletContainerFragment : BaseFragment() {

    private val args: WalletContainerFragmentArgs by navArgs()

    private lateinit var binding: FragmentWalletContainerBinding

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val keystoreDataSource by lazy {
        requireContext().secureKeystoreDataSource()
    }

    private val dccCertificatesManager by lazy {
        requireContext().dccCertificatesManager()
    }

    private val viewModel: WalletViewModel by viewModels {
        val app = requireActivity().application as StopCovid
        WalletViewModelFactory(
            robertManager,
            keystoreDataSource,
            app.injectionContainer.blacklistDCCManager,
            app.injectionContainer.blacklist2DDOCManager,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) { // do not replay navigation on config change
            handleRawCode(args.code, args.certificateFormat?.let { WalletCertificateType.Format.fromValue(it) }, args.origin)
        }

        setupResultListener()
    }

    private fun handleRawCode(
        rawCode: String?,
        certificateFormat: WalletCertificateType.Format?,
        origin: DeeplinkManager.Origin?,
        skipMaxWarning: Boolean = false,
    ) {
        val code = rawCode?.splitUrlFragment()
        val certificateValue = code?.getOrNull(0)
        val reportCode = code?.getOrNull(1)

        val shouldAddCertificate = robertManager.configuration.displaySanitaryCertificatesWallet && certificateValue != null
        val shouldReport = reportCode != null && robertManager.isRegistered
        val shouldShowMaxCertificatesWarning = shouldAddCertificate && !skipMaxWarning &&
            (WalletManager.walletCertificateLiveData.value?.size ?: 0) >= robertManager.configuration.maxCertBeforeWarning

        when {
            shouldShowMaxCertificatesWarning -> {
                setFragmentResultListener(WalletQuantityWarningFragment.MAX_CERTIFICATES_CONFIRM_ADD_RESULT_KEY) { _, bundle ->
                    val confirmAdd = bundle.getBoolean(WalletQuantityWarningFragment.MAX_CERTIFICATES_CONFIRM_ADD_BUNDLE_KEY, false)
                    if (confirmAdd) {
                        handleRawCode(rawCode, certificateFormat, origin, true)
                    }
                }
                findNavControllerOrNull()?.safeNavigate(
                    WalletContainerFragmentDirections.actionWalletContainerFragmentToWalletQuantityWarningFragment()
                )
            }
            shouldAddCertificate && shouldReport -> findNavControllerOrNull()?.safeNavigate(
                WalletContainerFragmentDirections.actionWalletContainerFragmentToPositiveTestStepsFragment(
                    positiveTestDccValue = certificateValue!!,
                    reportCode = reportCode!!,
                )
            )
            shouldAddCertificate && !shouldReport -> showConfirmOrProcessCodeValue(certificateValue!!, certificateFormat, origin)
            !shouldAddCertificate && shouldReport -> findNavControllerOrNull()?.safeNavigate(
                WalletContainerFragmentDirections.actionWalletContainerFragmentToSymptomsOriginFragment(
                    code = reportCode!!,
                )
            )
            !shouldAddCertificate && !shouldReport && !robertManager.configuration.displaySanitaryCertificatesWallet ->
                findNavControllerOrNull()?.navigateUp()
        }
    }

    private fun showConfirmOrProcessCodeValue(
        certificateCode: String,
        certificateFormat: WalletCertificateType.Format?,
        origin: DeeplinkManager.Origin?
    ) {
        lifecycleScope.launchWhenResumed {
            when (origin) {
                DeeplinkManager.Origin.EXTERNAL -> {
                    val certificate = validateAndGetCertificate(certificateCode, certificateFormat)
                    if (certificate != null) {
                        findNavControllerOrNull()?.safeNavigate(
                            WalletContainerFragmentDirections.actionWalletContainerFragmentToConfirmAddWalletCertificateFragment(
                                certificateCode,
                                certificateFormat?.name,
                            )
                        )
                    }
                }
                DeeplinkManager.Origin.UNIVERSAL,
                null -> {
                    processCodeValue(certificateCode, certificateFormat)
                }
            }
        }
    }

    private fun setupResultListener() {
        setFragmentResultListener(WalletQRCodeFragment.SCANNED_CODE_RESULT_KEY) { _, bundle ->
            val scannedData = bundle.getString(WalletQRCodeFragment.SCANNED_CODE_BUNDLE_KEY)
            scannedData?.let { data ->
                if (URLUtil.isValidUrl(data)) {
                    try {
                        val certificateData = WalletManager.extractCertificateDataFromUrl(data)
                        handleRawCode(
                            certificateData.first,
                            certificateData.second,
                            null,
                        )
                    } catch (e: WalletCertificateMalformedException) {
                        lifecycleScope.launch {
                            handleCertificateError(e, null)
                        }
                    }
                } else {
                    handleRawCode(data, null, null)
                }
            }
        }

        setFragmentResultListener(CONFIRM_ADD_CODE_RESULT_KEY) { _, bundle ->
            val code = bundle.getString(CONFIRM_ADD_CODE_BUNDLE_KEY_CODE)
            val certificateFormat = bundle.getString(CONFIRM_ADD_CODE_BUNDLE_KEY_FORMAT)?.let {
                safeEnumValueOf<WalletCertificateType.Format>(it)
            }
            val confirm = bundle.getBoolean(CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM)

            if (confirm && code != null) {
                lifecycleScope.launch {
                    processCodeValue(code, certificateFormat)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWalletContainerBinding.inflate(inflater, container, false)
        binding.walletBottomSheetButton.setOnClickListener {
            findNavControllerOrNull()
                ?.safeNavigate(WalletContainerFragmentDirections.actionWalletContainerFragmentToWalletQRCodeFragment())
        }
        return binding.root
    }

    override fun refreshScreen() {
        appCompatActivity?.supportActionBar?.title = strings["walletController.title"]
        binding.walletBottomSheetButton.text = strings["walletController.addCertificate"]
    }

    private suspend fun validateAndGetCertificate(
        certificateCode: String,
        certificateFormat: WalletCertificateType.Format?
    ): WalletCertificate? {
        return try {
            WalletManager.verifyAndGetCertificateCodeValue(
                robertManager.configuration,
                certificateCode,
                dccCertificatesManager.certificates,
                certificateFormat,
            )
        } catch (e: Exception) {
            handleCertificateError(e, certificateCode)
            null
        }
    }

    private suspend fun processCodeValue(certificateCode: String, certificateFormat: WalletCertificateType.Format?) {
        val certificate = validateAndGetCertificate(certificateCode, certificateFormat)
        if (certificate != null) {
            val dcc = (certificate as? EuropeanCertificate)
            showDuplicateOrBlacklistedWarningIfNeeded(
                isDuplicate = WalletManager.walletCertificateLiveData.value?.any { it.value == certificate.value } == true,
                isBlacklisted = viewModel.isBlacklisted(certificate),
            ) {
                showAlertForeignDccIfNeeded(dcc) {
                    lifecycleScope.launch {
                        processCertificate(certificate)
                    }
                }
            }
        }
    }

    private suspend fun processCertificate(certificate: WalletCertificate) {
        try {
            viewModel.saveCertificate(certificate)

            val vaccination = (certificate as? EuropeanCertificate)?.greenCertificate?.vaccinations?.lastOrNull()
            if (vaccination != null && vaccination.doseNumber >= vaccination.totalSeriesOfDoses) {
                findNavControllerOrNull()?.safeNavigate(
                    WalletContainerFragmentDirections.actionWalletContainerFragmentToVaccineCompletionFragment(
                        certificate.value
                    )
                )
            } else {
                strings["walletController.addCertificate.addSucceeded"]?.let {
                    (activity as? MainActivity)?.showSnackBar(it)
                }
            }
        } catch (e: Exception) {
            handleCertificateError(e, certificate.type.code)
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
                WalletContainerFragmentDirections.actionWalletContainerFragmentToWalletCertificateErrorFragment(
                    certificateType,
                    certificateError,
                )
            )
        } else {
            showUnknownErrorAlert(null)
        }
    }

    private fun showDuplicateOrBlacklistedWarningIfNeeded(
        isDuplicate: Boolean,
        isBlacklisted: Boolean,
        onContinue: () -> Unit
    ) {
        if (isDuplicate || isBlacklisted) {
            val messages = mutableListOf<String>()
            if (isDuplicate) {
                messages += strings["walletController.alert.duplicatedCertificate.subtitle"].orEmpty()
            }
            if (isBlacklisted) {
                messages += strings["wallet.blacklist.warning"].orEmpty()
            }

            val confirm = if (isDuplicate) {
                strings["walletController.alert.duplicatedCertificate.confirm"]
            } else {
                strings["walletController.addForeignCertificate.alert.add"]
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(strings["common.warning"])
                .setMessage(messages.joinToString("\n\n"))
                .setPositiveButton(confirm) { _, _ ->
                    onContinue()
                }
                .setNegativeButton(strings["common.cancel"], null)
                .show()
        } else {
            onContinue()
        }
    }

    private fun showAlertForeignDccIfNeeded(
        dcc: EuropeanCertificate?,
        onContinue: () -> Unit,
    ) {
        if (dcc != null && !dcc.greenCertificate.isFrench) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(strings["common.warning"])
                .setMessage(strings["walletController.addForeignCertificate.alert.message"])
                .setPositiveButton(strings["walletController.addForeignCertificate.alert.add"]) { _, _ ->
                    onContinue()
                }
                .setNegativeButton(strings["common.cancel"], null)
                .show()
        } else {
            onContinue()
        }
    }

    companion object {
        const val CONFIRM_ADD_CODE_RESULT_KEY: String = "CONFIRM_ADD_CODE_RESULT_KEY"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_CODE: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_CODE"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_FORMAT: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_FORMAT"
    }
}
