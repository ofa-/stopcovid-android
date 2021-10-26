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
import com.lunabeestudio.stopcovid.databinding.BottomSheetFragmentHealthDateTimeInfoBinding

class HealthDateTimeInfoBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFragmentHealthDateTimeInfoBinding

    private val strings: LocalizedStrings
        get() = (activity?.application as? LocalizedApplication)?.localizedStrings ?: emptyMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetFragmentHealthDateTimeInfoBinding.inflate(inflater, container, false)
        binding.setTexts()
        binding.setClickListeners()

        return binding.root
    }

    private fun BottomSheetFragmentHealthDateTimeInfoBinding.setTexts() {
        titleTextView.text = strings["myHealthController.riskMoreInfoAlert.title"]
        bodyTextView.text = strings["myHealthController.riskMoreInfoAlert.message"]
        okButton.text = strings["common.ok"]
    }

    private fun BottomSheetFragmentHealthDateTimeInfoBinding.setClickListeners() {
        okButton.setOnClickListener {
            dismiss()
        }
    }
}