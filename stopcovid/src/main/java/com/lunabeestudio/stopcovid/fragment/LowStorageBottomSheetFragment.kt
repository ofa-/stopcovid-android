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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lunabeestudio.stopcovid.coreui.LocalizedApplication
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.databinding.BottomSheetFragmentLowStorageBinding

class LowStorageBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFragmentLowStorageBinding

    private val strings: LocalizedStrings
        get() = (activity?.application as? LocalizedApplication)?.localizedStrings ?: emptyMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetFragmentLowStorageBinding.inflate(inflater, container, false)
        binding.setTexts()
        binding.setClickListeners()

        return binding.root
    }

    private fun BottomSheetFragmentLowStorageBinding.setTexts() {
        explanationTextView.text = strings["storageAlertScreen.storageAlert.description"]
        okButton.text = strings["common.ok"]
    }

    private fun BottomSheetFragmentLowStorageBinding.setClickListeners() {
        okButton.setOnClickListener {
            dismiss()
        }
    }
}