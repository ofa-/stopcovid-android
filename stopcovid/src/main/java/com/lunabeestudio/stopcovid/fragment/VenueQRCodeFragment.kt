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

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.isVenueOnBoardingDone
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.showExpiredCodeAlert
import com.lunabeestudio.stopcovid.extension.showInvalidCodeAlert
import com.lunabeestudio.stopcovid.extension.showUnknownErrorAlert
import com.lunabeestudio.stopcovid.manager.DeeplinkManager
import com.lunabeestudio.stopcovid.model.CaptchaNextFragment
import com.lunabeestudio.stopcovid.model.VenueExpiredException
import com.lunabeestudio.stopcovid.model.VenueInvalidFormatException
import com.lunabeestudio.stopcovid.viewmodel.VenueQrCodeViewModel
import com.lunabeestudio.stopcovid.viewmodel.VenueQrCodeViewModelFactory
import timber.log.Timber

class VenueQRCodeFragment : QRCodeFragment() {

    override fun getTitleKey(): String = "venueFlashCodeController.title"
    override val explanationKey: String = "venueFlashCodeController.explanation"
    override val footerKey: String? = null
    override fun onFooterClick() {}

    private val args: VenueQRCodeFragmentArgs by navArgs()

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: VenueQrCodeViewModel by viewModels {
        VenueQrCodeViewModelFactory(robertManager, requireContext().secureKeystoreDataSource(), injectionContainer.venueRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModelObserver()

        isReadyToStartScanFlow = false

        val venueContent = args.venueContent
        val venueVersion = args.venueVersion
        val venueTime = args.venueTime
        val origin = args.origin

        when {
            !robertManager.isRegistered -> findNavControllerOrNull()?.safeNavigate(
                VenueQRCodeFragmentDirections.actionVenueQrCodeFragmentToCaptchaFragment(
                    CaptchaNextFragment.Venue,
                    args.toBundle()
                )
            )
            venueContent != null && venueVersion != null && origin == DeeplinkManager.Origin.UNIVERSAL -> {
                viewModel.processVenue(venueContent, venueVersion, venueTime)
            }
            venueContent != null && venueVersion != null -> findNavControllerOrNull()?.safeNavigate(
                VenueQRCodeFragmentDirections.actionVenueQrCodeFragmentToConfirmVenueQrCodeFragment(
                    venueContent,
                    venueVersion.toInt(),
                    venueTime
                )
            )
            !sharedPrefs.isVenueOnBoardingDone -> findNavControllerOrNull()?.safeNavigate(
                VenueQRCodeFragmentDirections.actionVenueQrCodeFragmentToVenueOnBoardingFragment(args.toBundle())
            )
            else -> isReadyToStartScanFlow = true
        }
        setHasOptionsMenu(true)
    }

    private fun initViewModelObserver() {
        viewModel.venueProcessed.observe(viewLifecycleOwner) {
            navigateAfterVenueProcess()
        }
        viewModel.exception.observe(viewLifecycleOwner) { e ->
            catchVenueException(e) {
                resumeQrCodeReader()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.text_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.item_text).title = strings["common.moreInfo"]
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.item_text) {
            findNavControllerOrNull()?.safeNavigate(
                VenueQRCodeFragmentDirections.actionVenueQrCodeFragmentToVenueMoreInfoFragment()
            )
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onCodeScanned(code: String) {
        viewModel.processVenueUrl(code)
    }

    private fun navigateAfterVenueProcess() {
        analyticsManager.reportAppEvent(requireContext(), AppEventName.e14, null)
        findNavControllerOrNull()
            ?.safeNavigate(VenueQRCodeFragmentDirections.actionVenueQrCodeFragmentToVenueConfirmationFragment())
    }

    private fun catchVenueException(e: Exception, listener: DialogInterface.OnDismissListener) {
        Timber.e(e)
        when (e) {
            is VenueExpiredException -> context?.showExpiredCodeAlert(strings, listener)
            is VenueInvalidFormatException -> context?.showInvalidCodeAlert(strings, listener)
            else -> showUnknownErrorAlert(listener)
        }
    }
}
