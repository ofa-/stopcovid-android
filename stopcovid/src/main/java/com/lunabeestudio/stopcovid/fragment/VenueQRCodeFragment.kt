/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.lunabeestudio.domain.model.VenueQrType
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.extension.isVenueOnBoardingDone
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showInvalidCodeAlert
import com.lunabeestudio.stopcovid.manager.VenuesManager
import com.lunabeestudio.stopcovid.model.CaptchaNextFragment

class VenueQRCodeFragment : QRCodeFragment() {

    private val args: VenueQRCodeFragmentArgs by navArgs()

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isReadyToStartScanFlow = false

        val venueFullPath = args.venueFullPath
            ?: args.venueStaticPath?.let { "${VenueQrType.STATIC.value}/$it" }
            ?: args.venueDynamicPath?.let { "${VenueQrType.DYNAMIC.value}/$it" }

        when {
            !robertManager.displayRecordVenues || robertManager.isSick -> findNavControllerOrNull()?.navigateUp()
            !robertManager.isRegistered -> findNavControllerOrNull()?.safeNavigate(
                VenueQRCodeFragmentDirections.actionVenueQrCodeFragmentToCaptchaFragment(
                    CaptchaNextFragment.Venue,
                    venueFullPath = venueFullPath
                )
            )
            venueFullPath != null -> findNavControllerOrNull()?.safeNavigate(
                VenueQRCodeFragmentDirections.actionVenueQrCodeFragmentToConfirmVenueQrCodeFragment(venueFullPath)
            )
            !sharedPrefs.isVenueOnBoardingDone -> findNavControllerOrNull()?.safeNavigate(
                VenueQRCodeFragmentDirections.actionVenueQrCodeFragmentToVenueOnBoardingFragment(venueFullPath = venueFullPath)
            )
            else -> isReadyToStartScanFlow = true
        }
    }

    override fun getTitleKey(): String = "venueFlashCodeController.title"
    override fun getExplanationKey(): String = "venueFlashCodeController.explanation"

    override fun onCodeScanned(code: String) {
        val venueType = VenuesManager.processVenueUrl(
            robertManager = robertManager,
            secureKeystoreDataSource = requireContext().secureKeystoreDataSource(),
            code
        )
        handleVenueType(venueType)
    }

    private fun handleVenueType(venueType: String?) {
        if (venueType == null) {
            context?.showInvalidCodeAlert(strings)
            findNavControllerOrNull()?.navigateUp()
        } else {
            findNavControllerOrNull()
                ?.safeNavigate(VenueQRCodeFragmentDirections.actionVenueQrCodeFragmentToVenueConfirmationFragment(venueType))
        }
    }
}