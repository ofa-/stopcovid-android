/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/1/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.databinding.FragmentWalletFullscreenActivityPassBinding
import com.lunabeestudio.stopcovid.extension.fullName
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.isExpired
import com.lunabeestudio.stopcovid.extension.navGraphWalletViewModels
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class WalletFullscreenActivityPassFragment : ForceLightFragment(R.layout.fragment_wallet_fullscreen_activity_pass) {

    private val barcodeEncoder = BarcodeEncoder()
    private val qrCodeSize by lazy {
        R.dimen.qr_code_fullscreen_size.toDimensSize(requireContext()).toInt()
    }

    private val viewModel by navGraphWalletViewModels<WalletFullscreenPagerFragment> {
        WalletViewModelFactory(
            requireContext().robertManager(),
            blacklistDCCManager,
            blacklist2DDOCManager,
            injectionContainer.walletRepository,
            injectionContainer.generateActivityPassUseCase,
        )
    }

    private lateinit var binding: FragmentWalletFullscreenActivityPassBinding
    private var activityPass: EuropeanCertificate? = null

    private val timeUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (activityPass?.isExpired != false) {
                refreshCertificate()
            } else {
                activityPass?.let {
                    setValidityTime(binding.validityTimeChip, it)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        context?.registerReceiver(timeUpdateReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(timeUpdateReceiver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentWalletFullscreenActivityPassBinding.bind(view)

        binding.shareButton.text = strings["common.share"]
        binding.shareButton.setOnClickListener {
            findParentFragmentByType<WalletFullscreenPagerFragment>()?.showCertificateSharingBottomSheet(
                binding.barcodeSecuredView,
                activityPass
            )
        }

        refreshCertificate()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (menu.findItem(R.id.qr_code_menu_share) == null) {
            inflater.inflate(R.menu.fullscreen_qr_code_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.qr_code_menu_share -> {
                findParentFragmentByType<WalletFullscreenPagerFragment>()?.showCertificateSharingBottomSheet(
                    binding.barcodeSecuredView,
                    activityPass
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshCertificate() {
        val rootCertificateId = arguments?.getString(ROOT_CERTIFICATE_ID_ARG_KEY)
        if (rootCertificateId != null) {
            lifecycleScope.launch {
                activityPass = viewModel.getNotExpiredActivityPass(rootCertificateId)
                if (activityPass != null) {
                    refreshScreen()
                } else {
                    findParentFragmentByType<WalletFullscreenPagerFragment>()?.refreshPager()
                }
            }
        } else {
            Timber.e("ROOT_CERTIFICATE_ID_ARG_KEY must not be null")
            findParentFragmentByType<WalletFullscreenPagerFragment>()?.refreshPager()
        }
    }

    override fun refreshScreen() {
        val europeanCertificate = this.activityPass ?: return
        binding.apply {
            barcodeSecuredView.bitmap =
                barcodeEncoder.encodeBitmap(
                    europeanCertificate.value,
                    BarcodeFormat.QR_CODE,
                    qrCodeSize,
                    qrCodeSize
                )

            certificateDetailsTextView.text = europeanCertificate.fullName()
            setValidityTime(validityTimeChip, europeanCertificate)

            explanationTextView.text = strings["europeanCertificate.fullscreen.type.minimum.footer"]
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun setValidityTime(validityChip: Chip, europeanCertificate: EuropeanCertificate) {
        val now = Duration.milliseconds(System.currentTimeMillis())
        val expireAt = Duration.milliseconds(europeanCertificate.expirationTime)
        val timeSpan = expireAt - now
        val timeString = when {
            timeSpan < Duration.minutes(1) -> strings["activityPass.fullscreen.validFor.timeFormat.lessThanAMinute"]
            timeSpan < Duration.hours(1) -> stringsFormat(
                "activityPass.fullscreen.validFor.timeFormat.minutes",
                timeSpan.inWholeMinutes + 1, // avoid "1min left"
            )
            else -> {
                val hours = timeSpan.inWholeHours
                stringsFormat(
                    "activityPass.fullscreen.validFor.timeFormat.hoursMinutes",
                    hours,
                    (timeSpan - Duration.hours(hours)).inWholeMinutes,
                )
            }
        }
        validityChip.text = stringsFormat("activityPass.fullscreen.validFor", timeString)
    }

    companion object {
        private const val ROOT_CERTIFICATE_ID_ARG_KEY = "ROOT_CERTIFICATE_ID_ARG_KEY"

        fun newInstance(id: String): WalletFullscreenActivityPassFragment {
            return WalletFullscreenActivityPassFragment().apply {
                arguments = bundleOf(
                    ROOT_CERTIFICATE_ID_ARG_KEY to id,
                )
            }
        }
    }
}