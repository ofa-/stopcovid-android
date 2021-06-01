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
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentWalletContainerBinding
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showUnknownErrorAlert
import com.lunabeestudio.stopcovid.extension.walletCertificateError
import com.lunabeestudio.stopcovid.manager.WalletManager

class WalletContainerFragment : BaseFragment() {

    private val args: WalletContainerFragmentArgs by navArgs()

    private lateinit var binding: FragmentWalletContainerBinding

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val keystoreDataSource by lazy {
        requireContext().secureKeystoreDataSource()
    }

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private var confirmationAsked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!robertManager.configuration.displaySanitaryCertificatesWallet) {
            findNavControllerOrNull()?.navigateUp()
        }

        confirmationAsked = savedInstanceState?.getBoolean(CONFIRMATION_ASKED_KEY) == true

        val certificateCode = args.certificateCode
        if (certificateCode != null && !confirmationAsked) {
            if (checkCodeValue(certificateCode)) {
                findNavControllerOrNull()?.safeNavigate(
                    WalletContainerFragmentDirections.actionWalletContainerFragmentToConfirmAddWalletCertificateFragment(
                        certificateCode
                    )
                )
                confirmationAsked = true
            }
        }

        setFragmentResultListener(SCANNED_CODE_RESULT_KEY) { _, bundle ->
            val url = bundle.getString(SCANNED_CODE_BUNDLE_KEY)
            val certificateType = WalletCertificateType.valueOf(bundle.getString(SCANNED_CERTIFICATE_TYPE_REQUESTED_BUNDLE_KEY)!!)
            url?.let { processUrlValue(it, certificateType) }
        }

        setFragmentResultListener(CONFIRM_ADD_CODE_RESULT_KEY) { _, bundle ->
            val code = bundle.getString(CONFIRM_ADD_CODE_BUNDLE_KEY_CODE)
            val confirm = bundle.getBoolean(CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM)

            if (confirm && code != null) {
                processCodeValue(code)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWalletContainerBinding.inflate(inflater, container, false)
        binding.walletBottomSheetButton.setOnClickListener {
            findNavControllerOrNull()
                ?.navigate(WalletContainerFragmentDirections.actionWalletContainerFragmentToWalletAddCertificateFragment())
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

    private fun processUrlValue(url: String, certificateType: WalletCertificateType) {
        try {
            val certificateCode = WalletManager.extractCertificateCodeFromUrl(url)
            processCodeValue(certificateCode)
        } catch (e: Exception) {
            handleCertificateError(e, certificateType)
        }
    }

    private fun checkCodeValue(certificateCode: String): Boolean {
        return try {
            WalletManager.verifyCertificateCodeValue(sharedPreferences, robertManager.configuration, certificateCode)
            true
        } catch (e: Exception) {
            handleCertificateError(e, certificateCode)
        }
    }

    private fun processCodeValue(certificateCode: String) {
        try {
            WalletManager.processCertificateCode(sharedPreferences, robertManager, keystoreDataSource, certificateCode)
            context?.let { AnalyticsManager.reportAppEvent(it, AppEventName.e13, null) }
        } catch (e: Exception) {
            handleCertificateError(e, certificateCode)
        }
    }

    private fun handleCertificateError(error: Exception, certificateCode: String): Boolean {
        val certificateType = WalletManager.extractCertificateType(certificateCode)
        if (certificateType != null) {
            handleCertificateError(error, certificateType)
        } else {
            showUnknownErrorAlert(null)
        }
        return false
    }

    private fun handleCertificateError(error: Exception, certificateType: WalletCertificateType): Boolean {
        val certificateError = error.walletCertificateError()
        if (certificateError != null) {
            findNavControllerOrNull()?.navigate(
                WalletContainerFragmentDirections.actionWalletQRCodeFragmentToWalletCertificateErrorFragment(
                    certificateType,
                    certificateError,
                )
            )
        } else {
            showUnknownErrorAlert(null)
        }
        return false
    }

    companion object {
        const val SCANNED_CODE_RESULT_KEY: String = "SCANNED_CODE_RESULT_KEY"
        const val SCANNED_CODE_BUNDLE_KEY: String = "SCANNED_CODE_BUNDLE_KEY"
        const val SCANNED_CERTIFICATE_TYPE_REQUESTED_BUNDLE_KEY: String = "SCANNED_CERTIFICATE_TYPE_REQUESTED_BUNDLE_KEY"
        const val CONFIRM_ADD_CODE_RESULT_KEY: String = "CONFIRM_ADD_CODE_RESULT_KEY"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_CONFIRM"
        const val CONFIRM_ADD_CODE_BUNDLE_KEY_CODE: String = "CONFIRM_ADD_CODE_BUNDLE_KEY_CODE"

        const val CONFIRMATION_ASKED_KEY: String = "confirmationAsked"
    }
}
