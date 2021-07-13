/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.map
import androidx.navigation.fragment.navArgs
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.databinding.FragmentFullscreenDccBinding
import com.lunabeestudio.stopcovid.extension.dccCertificatesManager
import com.lunabeestudio.stopcovid.extension.formatDccText
import com.lunabeestudio.stopcovid.extension.fullName
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

class FullscreenDccFragment : ForceLightFragment(R.layout.fragment_fullscreen_dcc) {

    private val args: FullscreenDccFragmentArgs by navArgs()

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val keystoreDataSource by lazy {
        requireContext().secureKeystoreDataSource()
    }

    private val dccCertificatesManager by lazy {
        requireContext().dccCertificatesManager()
    }

    private val viewModel: WalletViewModel by viewModels(
        {
            findParentFragmentByType<WalletContainerFragment>() ?: requireParentFragment()
        },
        {
            WalletViewModelFactory(robertManager, keystoreDataSource, dccCertificatesManager)
        }
    )

    private val barcodeEncoder = BarcodeEncoder()
    private val qrCodeSize by lazy {
        R.dimen.qr_code_fullscreen_size.toDimensSize(requireContext()).toInt()
    }

    private var europeanCertificate: EuropeanCertificate? = null

    private var binding: FragmentFullscreenDccBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appCompatActivity?.supportActionBar?.title = strings["walletController.title"]
        binding = FragmentFullscreenDccBinding.bind(view)
        viewModel.certificates
            .map { certificates ->
                certificates
                    ?.filterIsInstance<EuropeanCertificate>()
                    ?.firstOrNull { it.keyCertificateId == args.keyCertificateId }
            }
            .observe(viewLifecycleOwner) { europeanCertificate ->
                this.europeanCertificate = europeanCertificate
                refreshScreen()
            }
    }

    override fun refreshScreen() {
        val europeanCertificate = europeanCertificate ?: return

        binding?.apply {
            val minLines = strings["europeanCertificate.fullscreen.englishDescription.${europeanCertificate.type.code}"]
                ?.count { it == '\n' }
                ?.plus(1)
            minLines?.let(detailsTextView::setMinLines)
            showMoreSwitch.text = strings["europeanCertificate.fullscreen.type.border.switch"]
            showMoreSwitch.setOnCheckedChangeListener { _, isChecked ->
                refreshDetails(isChecked, europeanCertificate)
            }
            explanationTextView.text = strings["europeanCertificate.fullscreen.type.minimum.footer"]
            refreshDetails(showMoreSwitch.isChecked, europeanCertificate)
            barcodeImageView.setImageBitmap(
                barcodeEncoder.encodeBitmap(
                    europeanCertificate.value,
                    BarcodeFormat.QR_CODE,
                    qrCodeSize,
                    qrCodeSize
                )
            )
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun FragmentFullscreenDccBinding.refreshDetails(
        isChecked: Boolean,
        europeanCertificate: EuropeanCertificate
    ) {
        detailsTextView.text = if (isChecked) {
            europeanCertificate.formatDccText(
                strings["europeanCertificate.fullscreen.englishDescription.${europeanCertificate.type.code}"],
                strings,
                SimpleDateFormat("d MMM yyyy", Locale.ENGLISH),
                SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH),
            )
        } else {
            europeanCertificate.fullName()
        }
        explanationTextView.isVisible = !isChecked
    }
}