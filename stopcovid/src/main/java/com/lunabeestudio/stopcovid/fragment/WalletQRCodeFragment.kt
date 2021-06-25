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

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull

class WalletQRCodeFragment : QRCodeFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isReadyToStartScanFlow = true
    }

    override fun getTitleKey(): String = "flashWalletCodeController.title"
    override fun getExplanationKey(): String = "flashWalletCodeController.explanation"

    override fun onCodeScanned(code: String) {
        setFragmentResult(
            SCANNED_CODE_RESULT_KEY,
            bundleOf(SCANNED_CODE_BUNDLE_KEY to code)
        )
        findNavControllerOrNull()?.popBackStack()
    }

    companion object {
        const val SCANNED_CODE_RESULT_KEY: String = "WALLET_QR_CODE_FRAGMENT.SCANNED_CODE_RESULT_KEY"
        const val SCANNED_CODE_BUNDLE_KEY: String = "WALLET_QR_CODE_FRAGMENT.SCANNED_CODE_BUNDLE_KEY"
    }
}