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

class VaccinDocumentExplanationFragment : DocumentExplanationFragment() {

    override fun refreshScreen() {
        appCompatActivity?.supportActionBar?.title = strings["documentExplanationController.vaccinCertificate.title"]
        binding.explanationTextView.text = strings["documentExplanationController.vaccinCertificate.explanation"]
        val vaccinCertificateFull = File(requireContext().filesDir, ConfigConstant.Wallet.VACCIN_CERTIFICATE_FULL_FILE)
        if (!binding.documentPhotoView.setImageFileIfValid(vaccinCertificateFull)) {
            binding.documentPhotoView.setImageResource(R.drawable.vaccin_certificate_full)
        }
    }
}