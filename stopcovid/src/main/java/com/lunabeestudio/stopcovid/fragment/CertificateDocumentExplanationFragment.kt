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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.setImageFileIfValid
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentDocumentExplanationBinding
import com.lunabeestudio.stopcovid.extension.certificateDrawable
import com.lunabeestudio.stopcovid.extension.certificateFilename
import com.lunabeestudio.stopcovid.extension.stringKey
import java.io.File

class CertificateDocumentExplanationFragment : BaseFragment() {

    private lateinit var binding: FragmentDocumentExplanationBinding

    private val args by navArgs<CertificateDocumentExplanationFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDocumentExplanationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun refreshScreen() {
        appCompatActivity?.supportActionBar?.title = strings["documentExplanationController.${args.certificateType.stringKey}.title"]
        binding.explanationTextView.text = strings["documentExplanationController.${args.certificateType.stringKey}.explanation"]
        showDocument()
    }

    private fun showDocument() {
        val certificateFilename = args.certificateType.certificateFilename ?: return
        val certificateFull = File(requireContext().filesDir, certificateFilename)
        if (!binding.documentPhotoView.setImageFileIfValid(certificateFull)) {
            val certificateDrawable = args.certificateType.certificateDrawable ?: return
            binding.documentPhotoView.setImageResource(certificateDrawable)
        }
    }
}