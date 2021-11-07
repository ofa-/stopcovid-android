/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/4/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lunabeestudio.stopcovid.coreui.LocalizedApplication
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.databinding.BottomSheetFragmentActionConfirmBinding

class CertificateSharingBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFragmentActionConfirmBinding

    private val strings: LocalizedStrings
        get() = (activity?.application as? LocalizedApplication)?.localizedStrings ?: emptyMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetFragmentActionConfirmBinding.inflate(inflater, container, false)
        binding.setTexts()
        binding.setClickListeners()

        return binding.root
    }

    private fun BottomSheetFragmentActionConfirmBinding.setTexts() {
        titleTextView.text = strings["certificateSharingController.title"]
        bodyTextView.text = strings["certificateSharingController.message"]
        okButton.setTextOrHide(strings["common.confirm"])
        cancelButton.setTextOrHide(strings["common.cancel"])
    }

    private fun BottomSheetFragmentActionConfirmBinding.setClickListeners() {
        okButton.setOnClickListener {
            setFragmentResult(
                CERTIFICATE_SHARING_RESULT_KEY,
                bundleOf(CERTIFICATE_SHARING_BUNDLE_KEY_CONFIRM to true),
            )
            dismiss()
        }
        cancelButton.setOnClickListener {
            setFragmentResult(
                CERTIFICATE_SHARING_RESULT_KEY,
                bundleOf(CERTIFICATE_SHARING_BUNDLE_KEY_CONFIRM to false),
            )
            dismiss()
        }
    }

    companion object {
        const val CERTIFICATE_SHARING_RESULT_KEY: String = "CERTIFICATE_SHARING_RESULT_KEY"
        const val CERTIFICATE_SHARING_BUNDLE_KEY_CONFIRM: String = "CERTIFICATE_SHARING_BUNDLE_KEY_CONFIRM"
    }
}