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

import android.net.Uri
import android.webkit.URLUtil
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.extension.isReportCodeValid
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.showInvalidCodeAlert
import com.lunabeestudio.stopcovid.extension.splitUrlFragment

class ReportQRCodeFragment : QRCodeFragment() {

    override fun getTitleKey(): String = "declareController.title"
    override val explanationKey: String = "scanCodeController.explanation"
    override val footerKey: String? = null
    override fun onFooterClick() {}

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    override fun onCodeScanned(code: String) {
        when {
            code.isReportCodeValid() -> findNavControllerOrNull()?.safeNavigate(
                ReportQRCodeFragmentDirections.actionReportQrCodeFragmentToSymptomsOriginFragment(code)
            )
            URLUtil.isValidUrl(code) -> checkComboUrl(code)
            else -> showInvalidCodeAlert()
        }
    }

    private fun checkComboUrl(code: String) {
        val splitData = Uri.parse(code).fragment?.splitUrlFragment()
        val certificateValue = splitData?.getOrNull(0)
        val reportCode = splitData?.getOrNull(1)

        when {
            certificateValue != null && reportCode != null && robertManager.configuration.displaySanitaryCertificatesWallet ->
                findNavControllerOrNull()?.safeNavigate(
                    ReportQRCodeFragmentDirections.actionReportQrCodeFragmentToPositiveTestStepsFragment(certificateValue, reportCode)
                )
            certificateValue != null && reportCode != null && !robertManager.configuration.displaySanitaryCertificatesWallet ->
                findNavControllerOrNull()?.safeNavigate(
                    ReportQRCodeFragmentDirections.actionReportQrCodeFragmentToSymptomsOriginFragment(reportCode)
                )
            else -> showInvalidCodeAlert()
        }
    }

    private fun showInvalidCodeAlert() {
        context?.showInvalidCodeAlert(strings) {
            resumeQrCodeReader()
        }
    }
}