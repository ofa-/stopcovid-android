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

import android.net.Uri
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
import com.lunabeestudio.stopcovid.extension.walletCertificateError
import com.lunabeestudio.stopcovid.manager.DeeplinkManager
import com.lunabeestudio.stopcovid.manager.WalletManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.model.WalletCertificate
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
        WalletViewModelFactory(robertManager, keystoreDataSource)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!robertManager.configuration.displaySanitaryCertificatesWallet) {
            findNavControllerOrNull()?.navigateUp()
        }

        if (savedInstanceState == null) { // do not replay navigation on config change
            val certificateCode = args.code
            val certificateFormat = args.certificateFormat?.let { WalletCertificateType.Format.fromValue(it) }
            if (certificateCode != null) {
                handleArgumentsDeeplink(certificateCode, certificateFormat)
            }
        }

        setupResultListener()
    }

    private fun handleArgumentsDeeplink(certificateCode: String, certificateFormat: WalletCertificateType.Format?) {
        lifecycleScope.launchWhenResumed {

            when (args.origin) {
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
                DeeplinkManager.Origin.UNIVERSAL -> {
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
                    lifecycleScope.launch {
                        processUrlValue(data)
                    }
                } else {
                    lifecycleScope.launch {
                        processCodeValue(data, null)
                    }
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

    private suspend fun processUrlValue(url: String) {
        try {
            val certificateCode = WalletManager.extractCertificateCodeFromUrl(url)
            val certificateFormat = Uri.parse(url).lastPathSegment?.let { WalletCertificateType.Format.fromValue(it) }
            processCodeValue(certificateCode, certificateFormat)
        } catch (e: Exception) {
            handleCertificateError(e, null)
        }
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
            val onContinueProcess: (() -> Unit) = {
                lifecycleScope.launch {
                    processCertificate(certificate)
                }
            }

            val dcc = (certificate as? EuropeanCertificate)
            if (dcc != null && !dcc.greenCertificate.isFrench) {
                showAlertForeignDcc(onContinueProcess)
            } else {
                onContinueProcess()
            }
        }
    }

    private suspend fun processCertificate(certificate: WalletCertificate) {
        try {
            viewModel.saveCertificate(requireContext(), certificate)

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

    private fun showAlertForeignDcc(
        onContinue: (() -> Unit),
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(strings["common.warning"])
            .setMessage(strings["walletController.addForeignCertificate.alert.message"])
            .setPositiveButton(strings["walletController.addForeignCertificate.alert.add"]) { _, _ ->
                onContinue()
            }
            .setNegativeButton(strings["common.cancel"], null)
            .show()
    }

    companion object {
        const val CONFIRM_ADD_CODE_RESULT_KEY: String = "CONFIRM_ADD_CODE_RESULT_KEY"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_CODE: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_CODE"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_FORMAT: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_FORMAT"
    }
}
