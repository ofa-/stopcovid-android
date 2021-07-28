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

import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.extension.isReportCodeValid
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.showInvalidCodeAlert

class ReportQRCodeFragment : QRCodeFragment() {

    override fun getTitleKey(): String = "declareController.title"
    override val explanationKey: String = "scanCodeController.explanation"
    override val footerKey: String? = null
    override fun onFooterClick() {}

    override fun onCodeScanned(code: String) {
        if (!code.isReportCodeValid()) {
            context?.showInvalidCodeAlert(strings) {
                resumeQrCodeReader()
            }
        } else {
            findNavControllerOrNull()?.safeNavigate(ReportQRCodeFragmentDirections.actionReportQrCodeFragmentToSymptomsOriginFragment(code))
        }
    }
}