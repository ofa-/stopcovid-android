/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/20/5 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.setImageFileIfValid
import java.io.File

class TestDocumentExplanationFragment : DocumentExplanationFragment() {

    override fun refreshScreen() {
        appCompatActivity?.supportActionBar?.title = strings["documentExplanationController.testCertificate.title"]
        binding.explanationTextView.text = strings["documentExplanationController.testCertificate.explanation"]
        val testCertificateFull = File(requireContext().filesDir, ConfigConstant.Wallet.TEST_CERTIFICATE_FULL_FILE)
        if (!binding.documentPhotoView.setImageFileIfValid(testCertificateFull)) {
            binding.documentPhotoView.setImageResource(R.drawable.test_certificate_full)
        }
    }
}