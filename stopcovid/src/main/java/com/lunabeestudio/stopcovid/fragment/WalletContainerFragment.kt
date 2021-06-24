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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.robert.extension.safeEnumValueOf
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentWalletContainerBinding
import com.lunabeestudio.stopcovid.extension.dccCertificatesManager
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showUnknownErrorAlert
import com.lunabeestudio.stopcovid.extension.walletCertificateError
import com.lunabeestudio.stopcovid.manager.DeeplinkManager
import com.lunabeestudio.stopcovid.manager.WalletManager
import com.lunabeestudio.stopcovid.model.WalletCertificate
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

    private var confirmationAsked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!robertManager.configuration.displaySanitaryCertificatesWallet) {
            findNavControllerOrNull()?.navigateUp()
        }

        confirmationAsked = savedInstanceState?.getBoolean(CONFIRMATION_ASKED_KEY) == true

        val certificateCode = args.code
        val certificateFormat = args.certificateFormat?.let { WalletCertificateType.Format.fromValue(it) }

        if (certificateCode != null && !confirmationAsked && args.origin == DeeplinkManager.Origin.EXTERNAL) {
            lifecycleScope.launch {
                if (checkCodeValue(certificateCode, certificateFormat)) {
                    findNavControllerOrNull()?.safeNavigate(
                        WalletContainerFragmentDirections.actionWalletContainerFragmentToConfirmAddWalletCertificateFragment(
                            certificateCode,
                            certificateFormat?.name,
                        )
                    )
                    confirmationAsked = true
                }
            }
        } else if (certificateCode != null && args.origin == DeeplinkManager.Origin.UNIVERSAL) {
            lifecycleScope.launch {
                processCodeValue(certificateCode, certificateFormat)
            }
        }

        setupResultListener()
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
                ?.safeNavigate(WalletContainerFragmentDirections.actionProximityFragmentToWalletQRCodeFragment())
        }
        return binding.root
    }

    override fun refreshScreen() {
        appCompatActivity?.supportActionBar?.title = strings["walletController.title"]
        binding.walletBottomSheetButton.text = strings["walletController.addCertificate"]
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(CONFIRMATION_ASKED_KEY, confirmationAsked)
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

    private suspend fun checkCodeValue(certificateCode: String, certificateFormat: WalletCertificateType.Format?): Boolean {
        return try {
            WalletManager.verifyCertificateCodeValue(
                robertManager.configuration,
                certificateCode,
                dccCertificatesManager.certificates,
                certificateFormat,
            )
            true
        } catch (e: Exception) {
            handleCertificateError(e, certificateCode)
            false
        }
    }

    private suspend fun processCodeValue(certificateCode: String, certificateFormat: WalletCertificateType.Format?) {
        try {
            WalletManager.processCertificateCode(
                robertManager,
                keystoreDataSource,
                certificateCode,
                dccCertificatesManager.certificates,
                certificateFormat,
            )
            context?.let { context ->
                AnalyticsManager.reportAppEvent(context, AppEventName.e13, null)
                strings["walletController.addCertificate.addSucceeded"]?.let {
                    (activity as? MainActivity)?.showSnackBar(it)
                }
            }
        } catch (e: Exception) {
            handleCertificateError(e, certificateCode)
        }
    }

    private suspend fun handleCertificateError(error: Exception, certificateCode: String?) {
        val certificateType = certificateCode?.let { WalletCertificate.getTypeFromValue(it) } ?: WalletCertificateType.SANITARY
        handleCertificateError(error, certificateType)
    }

    private fun handleCertificateError(error: Exception, certificateType: WalletCertificateType) {
        val certificateError = error.walletCertificateError()
        if (certificateError != null) {
            findNavControllerOrNull()?.safeNavigate(
                WalletContainerFragmentDirections.actionWalletQRCodeFragmentToWalletCertificateErrorFragment(
                    certificateType,
                    certificateError,
                )
            )
        } else {
            showUnknownErrorAlert(null)
        }
    }

    companion object {
        const val CONFIRM_ADD_CODE_RESULT_KEY: String = "CONFIRM_ADD_CODE_RESULT_KEY"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_CODE: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_CODE"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_FORMAT: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_FORMAT"

        const val CONFIRMATION_ASKED_KEY: String = "confirmationAsked"
    }
}
