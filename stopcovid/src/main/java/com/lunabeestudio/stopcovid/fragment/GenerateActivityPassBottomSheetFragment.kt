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
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.databinding.BottomSheetFragmentGenerateActivityPassBinding
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser

class GenerateActivityPassBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFragmentGenerateActivityPassBinding

    private val strings: LocalizedStrings
        get() = (activity?.application as? LocalizedApplication)?.localizedStrings ?: emptyMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetFragmentGenerateActivityPassBinding.inflate(inflater, container, false)
        binding.setTexts()
        binding.setClickListeners()

        return binding.root
    }

    private fun BottomSheetFragmentGenerateActivityPassBinding.setTexts() {
        explanationTextView.text = strings["activityPassParametersController.explanations"]
        actionTitleTextView.text = strings["activityPassParametersController.doYouConfirm"]
        confirmButton.text = strings["common.confirm"]
        tosButton.text = strings["activityPassParametersController.button.readCGU"]
        cancelButton.text = strings["common.cancel"]
    }

    private fun BottomSheetFragmentGenerateActivityPassBinding.setClickListeners() {
        confirmButton.setOnClickListener {
            setFragmentResult(
                CONFIRM_GENERATE_ACTIVITY_PASS_RESULT_KEY,
                bundleOf(
                    CONFIRM_GENERATE_ACTIVITY_PASS_BUNDLE_KEY_CONFIRM to true
                )
            )
            dismiss()
        }

        tosButton.setOnClickListener {
            strings["activityPassParametersController.cguUrl"]?.openInExternalBrowser(requireContext())
        }

        cancelButton.setOnClickListener {
            setFragmentResult(
                CONFIRM_GENERATE_ACTIVITY_PASS_RESULT_KEY,
                bundleOf(
                    CONFIRM_GENERATE_ACTIVITY_PASS_BUNDLE_KEY_CONFIRM to false
                )
            )
            dismiss()
        }
    }

    companion object {
        const val CONFIRM_GENERATE_ACTIVITY_PASS_RESULT_KEY: String = "CONFIRM_GENERATE_ACTIVITY_PASS_RESULT_KEY"
        const val CONFIRM_GENERATE_ACTIVITY_PASS_BUNDLE_KEY_CONFIRM: String = "CONFIRM_GENERATE_ACTIVITY_PASS_BUNDLE_KEY_CONFIRM"
    }
}