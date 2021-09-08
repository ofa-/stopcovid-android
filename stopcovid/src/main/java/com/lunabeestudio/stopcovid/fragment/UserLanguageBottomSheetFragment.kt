/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lunabeestudio.stopcovid.coreui.LocalizedApplication
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.extension.userLanguage
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.databinding.FragmentUserLanguageBottomSheetBinding
import com.lunabeestudio.stopcovid.databinding.WidgetLightButtonBinding
import com.lunabeestudio.stopcovid.extension.flaggedCountry

class UserLanguageBottomSheetFragment : BottomSheetDialogFragment() {

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val strings: LocalizedStrings
        get() = (activity?.application as? LocalizedApplication)?.localizedStrings ?: emptyMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentUserLanguageBottomSheetBinding.inflate(inflater, container, false)

        binding.userLanguageBottomSheetTitle.text = strings["onboarding.userLanguageBottomSheet.title"]

        binding.cancelButton.apply {
            text = strings["common.cancel"]
            setOnClickListener {
                setFragmentResult(USER_LANGUAGE_RESULT_KEY, bundleOf(USER_LANGUAGE_SET_BUNDLE_KEY to false))
                dismiss()
            }
        }

        UiConstants.SUPPORTED_LOCALES.forEachIndexed { idx, locale ->
            val button = WidgetLightButtonBinding.inflate(inflater, binding.root, false)
            button.root.text = locale.flaggedCountry.safeEmojiSpanify()
            button.root.setOnClickListener {
                sharedPrefs.userLanguage = locale.language
                setFragmentResult(USER_LANGUAGE_RESULT_KEY, bundleOf(USER_LANGUAGE_SET_BUNDLE_KEY to true))
                dismiss()
            }
            binding.root.addView(button.root, idx + 1)
        }

        return binding.root
    }

    companion object {
        const val USER_LANGUAGE_RESULT_KEY: String = "USER_LANGUAGE_BOTTOM_SHEET_FRAGMENT.USER_LANGUAGE_RESULT_KEY"
        const val USER_LANGUAGE_SET_BUNDLE_KEY: String = "USER_LANGUAGE_BOTTOM_SHEET_FRAGMENT.USER_LANGUAGE_SET_BUNDLE_KEY"
    }
}