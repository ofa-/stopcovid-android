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
import androidx.preference.PreferenceManager
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.extension.catchWalletException
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.manager.WalletManager

class VerifyWalletQRCodeFragment : QRCodeFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isReadyToStartScanFlow = true
    }

    override fun getTitleKey(): String = "flashDataMatrixCodeController.title"
    override fun getExplanationKey(): String = "flashDataMatrixCodeController.explanation"

    override fun onCodeScanned(code: String) {
        try {
            WalletManager.verifyCertificateCodeValue(sharedPreferences, robertManager.configuration, code)
            findNavControllerOrNull()?.safeNavigate(VerifyWalletQRCodeFragmentDirections.actionVerifyWalletQRCodeFragmentToVerifyWalletResultFragment(
                code))
        } catch (e: Exception) {
            catchWalletException(e) { _, _ ->
                resumeQrCodeReader()
            }
        }
    }
}