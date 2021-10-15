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

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.preference.PreferenceManager
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.extension.hasUsedUniversalQrScan
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.safeNavigate

class UniversalQrScanFragment : QRCodeDccFragment() {

    override fun getTitleKey(): String = "universalQrScanController.title"
    override val explanationKey: String = "universalQrScanController.explanation"
    override val footerKey: String = "universalQrScanController.footer.text"

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!sharedPrefs.hasUsedUniversalQrScan) {
            isReadyToStartScanFlow = false
            sharedPrefs.hasUsedUniversalQrScan = true
            findNavControllerOrNull()?.safeNavigate(
                UniversalQrScanFragmentDirections.actionUniversalQrScanFragmentToUniversalQrScanExplanationsFragment()
            )
        }
    }

    override fun onCodeScanned(code: String) {
        setFragmentResult(
            SCANNED_CODE_RESULT_KEY,
            bundleOf(SCANNED_CODE_BUNDLE_KEY to code)
        )
        findNavControllerOrNull()?.popBackStack()
    }

    override fun onFooterClick() {
        strings["universalQrScanController.footer.link.android"]?.openInExternalBrowser(requireContext())
        findNavControllerOrNull()?.popBackStack()
    }

    companion object {
        const val SCANNED_CODE_RESULT_KEY: String = "UNIVERSAL_QR_SCAN_FRAGMENT.SCANNED_CODE_RESULT_KEY"
        const val SCANNED_CODE_BUNDLE_KEY: String = "UNIVERSAL_QR_SCAN_FRAGMENT.SCANNED_CODE_BUNDLE_KEY"
    }
}