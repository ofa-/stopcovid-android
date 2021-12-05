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

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.LocalizedApplication
import com.lunabeestudio.stopcovid.coreui.extension.fetchSystemColor
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.databinding.BottomSheetFragmentMoreActionBinding
import com.lunabeestudio.stopcovid.extension.enableAutoFullscreenBrightness

class QrCodeMoreActionBottomSheetFragment : BottomSheetDialogFragment() {

    private val args by navArgs<QrCodeMoreActionBottomSheetFragmentArgs>()

    private lateinit var binding: BottomSheetFragmentMoreActionBinding

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val bundleResult = Bundle()

    private val strings: LocalizedStrings
        get() = (activity?.application as? LocalizedApplication)?.localizedStrings ?: emptyMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetFragmentMoreActionBinding.inflate(inflater, container, false)

        if (args.showBrightness) {
            binding.brightnessSwitch.setup()
            binding.brightnessLayout.setOnClickListener {
                binding.brightnessSwitch.toggle()
                sharedPreferences.enableAutoFullscreenBrightness = binding.brightnessSwitch.isChecked
                binding.brightnessSwitch.refresh()
            }
            binding.brightnessSwitchCaption.text = strings["common.settings.fullBrightnessSwitch.subtitle"]
        } else {
            binding.actionLayout.removeView(binding.brightnessLayout)
        }

            binding.actionLayout.removeView(binding.shareButton)

        return binding.root
    }

    private fun SwitchMaterial.setup() {
        isChecked = sharedPreferences.enableAutoFullscreenBrightness
        refresh()
        binding.brightnessSwitchCaption.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = (compoundDrawablesRelative[0]?.intrinsicWidth ?: 0) + compoundDrawablePadding
        }
    }

    private fun SwitchMaterial.refresh() {
        @DrawableRes
        val drawableRes: Int

        if (isChecked) {
            text = strings["common.settings.fullBrightnessSwitch.switch.on"]
            drawableRes = R.drawable.ic_brightness_on
        } else {
            text = strings["common.settings.fullBrightnessSwitch.switch.off"]
            drawableRes = R.drawable.ic_brightness_off
        }

        val drawable = ContextCompat.getDrawable(requireContext(), drawableRes)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            drawable?.setTint(android.R.attr.colorPrimary.fetchSystemColor(requireContext()))
        }

        setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
    }

    private fun TextView.setup() {
        text = strings["common.share"]
        setOnClickListener {
            bundleResult.putBoolean(MORE_ACTION_BUNDLE_KEY_SHARE_REQUESTED, true)
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        setFragmentResult(MORE_ACTION_RESULT_KEY, bundleResult)
    }

    companion object {
        const val MORE_ACTION_RESULT_KEY: String = "MORE_ACTION_RESULT_KEY"
        const val MORE_ACTION_BUNDLE_KEY_SHARE_REQUESTED: String = "MORE_ACTION_BUNDLE_KEY_SHARE_REQUESTED"
    }
}
