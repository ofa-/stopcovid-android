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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ShareCompat
import androidx.core.view.isGone
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.databinding.FragmentVaccinationActionsBottomSheetBinding
import com.lunabeestudio.stopcovid.extension.location
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.setTextOrHide

class VaccinationActionsBottomSheetFragment : BottomSheetDialogFragment() {

    val args: VaccinationActionsBottomSheetFragmentArgs by navArgs()

    private val strings: LocalizedStrings = StringsManager.strings

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentVaccinationActionsBottomSheetBinding.inflate(inflater, container, false)

        binding.titleTextView.setTextOrHide(args.vaccinationCenter.name)

        binding.callButton.apply {
            text = strings.stringsFormat(
                "vaccinationController.vaccinationCenter.actionSheet.alert.call",
                args.vaccinationCenter.phone
            )
            setOnClickListener {
                context?.let {
                    args.vaccinationCenter.phone?.callPhone(it)
                }
                dismiss()
            }
        }
        binding.callButton.isGone = args.vaccinationCenter.phone.isNullOrBlank()

        binding.bookButton.apply {
            text = strings["vaccinationController.vaccinationCenter.actionSheet.alert.website"]
            setOnClickListener {
                context?.let {
                    args.vaccinationCenter.url?.openInExternalBrowser(it)
                }
                dismiss()
            }
        }
        binding.bookButton.isGone = args.vaccinationCenter.url.isNullOrBlank()

        binding.findButton.apply {
            text = strings["vaccinationController.vaccinationCenter.actionSheet.alert.mapLocation"]
            setOnClickListener {
                context?.let {
                    val geoUri = "http://maps.google.com/maps?q=loc:${args.vaccinationCenter.latitude},${args.vaccinationCenter.longitude} (${args.vaccinationCenter.name})"
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                    if (mapIntent.resolveActivity(it.packageManager) != null) {
                        it.startActivity(mapIntent)
                    }
                }
                dismiss()
            }
        }
        binding.findButton.isGone = args.vaccinationCenter.location == null

        binding.shareButton.apply {
            text = strings["vaccinationController.vaccinationCenter.actionSheet.alert.sharing"]
            setOnClickListener {
                val sharingTexts = arrayListOf<String>()
                sharingTexts += listOf(
                    args.vaccinationCenter.name,
                    args.vaccinationCenter.streetNumber,
                    args.vaccinationCenter.streetName,
                    args.vaccinationCenter.postalCode,
                    args.vaccinationCenter.locality
                ).filter { it.isNotBlank() }.joinToString()
                if (!args.vaccinationCenter.phone.isNullOrBlank()) {
                    strings.stringsFormat(
                        "vaccinationController.vaccinationCenter.actionSheet.alert.sharing.text.tel",
                        args.vaccinationCenter.phone
                    )?.let { formattedPhone ->
                        sharingTexts += formattedPhone
                    }
                }
                if (!args.vaccinationCenter.url.isNullOrBlank()) {
                    strings.stringsFormat(
                        "vaccinationController.vaccinationCenter.actionSheet.alert.sharing.text.url",
                        args.vaccinationCenter.url
                    )?.let { formattedPhone ->
                        sharingTexts += formattedPhone
                    }
                }
                ShareCompat.IntentBuilder(requireActivity())
                    .setType("text/plain")
                    .setText(sharingTexts.joinToString(". "))
                    .startChooser()
                dismiss()
            }
        }

        binding.cancelButton.apply {
            text = strings["common.cancel"]
            setOnClickListener {
                dismiss()
            }
        }
        return binding.root
    }
}