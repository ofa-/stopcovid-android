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

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.databinding.FragmentWalletFullscreenBorderBinding
import com.lunabeestudio.stopcovid.extension.formatDccText
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.isFrench
import com.lunabeestudio.stopcovid.extension.navGraphWalletViewModels
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.stringKey
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

class WalletFullscreenBorderFragment : ForceLightFragment(R.layout.fragment_wallet_fullscreen_border) {

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

    private lateinit var binding: FragmentWalletFullscreenBorderBinding
    private var europeanCertificate: EuropeanCertificate? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentWalletFullscreenBorderBinding.bind(view)
        viewModel.certificates.asLiveData(timeoutInMs = 0)
            .map { certificates ->
                certificates
                    ?.filterIsInstance<EuropeanCertificate>()
                    ?.firstOrNull { it.id == arguments?.getString(CERTIFICATE_ID_ARG_KEY) }
            }
            .observe(viewLifecycleOwner) { europeanCertificate ->
                this.europeanCertificate = europeanCertificate
                refreshScreen()
            }
        binding.showMoreSwitch
            .setOnCheckedChangeListener { _, isChecked ->
                refreshDetails(isChecked)
            }
    }

    override fun refreshScreen() {
        val europeanCertificate = this.europeanCertificate ?: return
        binding.apply {
            headerTextView.setTextOrHide(strings["europeanCertificate.fullscreen.${europeanCertificate.type.stringKey}.border.warning"])

            logosImageView.isVisible = europeanCertificate.greenCertificate.isFrench == true

            certificateBarcodeImageView.setImageBitmap(
                barcodeEncoder.encodeBitmap(
                    europeanCertificate.value,
                    BarcodeFormat.QR_CODE,
                    qrCodeSize,
                    qrCodeSize
                )
            )

            certificateDetailsTextView.text = europeanCertificate.formatDccText(
                strings["europeanCertificate.fullscreen.englishDescription.${europeanCertificate.type.code}"],
                strings,
                SimpleDateFormat("d MMM yyyy", Locale.ENGLISH),
                SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH),
            )

            certificateHashTextView.text = europeanCertificate.sha256

            refreshDetails(showMoreSwitch.isChecked)
        }
    }

    private fun refreshDetails(showIt: Boolean) {
        if (showIt) {
            binding.certificateDetailsTextView.setVisibility(View.VISIBLE)
            binding.logosImageView.setVisibility(View.GONE)
        } else {
            binding.certificateDetailsTextView.setVisibility(View.GONE)
            binding.logosImageView.setVisibility(View.VISIBLE)
        }
    }

    companion object {
        private const val CERTIFICATE_ID_ARG_KEY = "CERTIFICATE_ID_ARG_KEY"

        fun newInstance(id: String): WalletFullscreenBorderFragment {
            return WalletFullscreenBorderFragment().apply {
                arguments = bundleOf(
                    CERTIFICATE_ID_ARG_KEY to id,
                )
            }
        }
    }
}