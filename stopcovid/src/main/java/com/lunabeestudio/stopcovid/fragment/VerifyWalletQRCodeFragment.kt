/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.extension.dccCertificatesManager
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.showUnknownErrorAlert
import com.lunabeestudio.stopcovid.extension.walletRepository
import com.lunabeestudio.stopcovid.model.WalletCertificateInvalidSignatureException
import com.lunabeestudio.stopcovid.model.WalletCertificateMalformedException
import com.lunabeestudio.stopcovid.model.WalletCertificateNoKeyException
import kotlinx.coroutines.launch
import timber.log.Timber

class VerifyWalletQRCodeFragment : QRCodeFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val dccCertificatesManager by lazy {
        requireContext().dccCertificatesManager()
    }

    private val WalletManager by lazy {
        requireContext().walletRepository()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isReadyToStartScanFlow = true
    }

    override fun getTitleKey(): String = "flashDataMatrixCodeController.title"
    override val explanationKey: String = "flashDataMatrixCodeController.explanation"
    override val footerKey: String? = null
    override fun onFooterClick() {}

    override fun onCodeScanned(code: String) {
        lifecycleScope.launch {
            try {
                WalletManager.verifyAndGetCertificateCodeValue(
                    robertManager.configuration,
                    code,
                    dccCertificatesManager.certificates,
                    null,
                )
                findNavControllerOrNull()?.safeNavigate(
                    VerifyWalletQRCodeFragmentDirections.actionVerifyWalletQRCodeFragmentToVerifyWalletResultFragment(
                        code
                    )
                )
            } catch (e: Exception) {
                catchWalletException(e) {
                    resumeQrCodeReader()
                }
            }
        }
    }

    private fun catchWalletException(e: Exception, listener: DialogInterface.OnDismissListener?) {
        Timber.e(e)
        when (e) {
            is WalletCertificateInvalidSignatureException -> showInvalidCertificateSignatureAlert(listener)
            is WalletCertificateMalformedException -> showMalformedCertificateAlert(listener)
            is WalletCertificateNoKeyException -> showInvalidCertificateSignatureAlert(listener)
            else -> showUnknownErrorAlert(listener)
        }
    }

    private fun showMalformedCertificateAlert(listener: DialogInterface.OnDismissListener?) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(strings["wallet.proof.error.1.title"])
            .setMessage(strings["wallet.proof.error.1.message"])
            .setPositiveButton(strings["common.ok"], null)
            .setOnDismissListener(listener)
            .show()
    }

    private fun showInvalidCertificateSignatureAlert(listener: DialogInterface.OnDismissListener?) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(strings["wallet.proof.error.2.title"])
            .setMessage(strings["wallet.proof.error.2.message"])
            .setPositiveButton(strings["common.ok"], null)
            .setOnDismissListener(listener)
            .show()
    }
}
