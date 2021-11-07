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

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.databinding.FragmentFullscreenAttestationBinding

class FullscreenAttestationFragment : ForceLightFragment(R.layout.fragment_fullscreen_attestation) {

    private val args: FullscreenAttestationFragmentArgs by navArgs()

    private val barcodeEncoder = BarcodeEncoder()
    private val qrCodeSize by lazy {
        R.dimen.qr_code_fullscreen_size.toDimensSize(requireContext()).toInt()
    }

    private lateinit var binding: FragmentFullscreenAttestationBinding

    override fun onZoom(scaleFactor: Float) {
        binding.imageView.scaleX = scaleFactor
        binding.imageView.scaleY = scaleFactor
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFullscreenAttestationBinding.bind(view)
        appCompatActivity?.supportActionBar?.title = strings["attestationsController.title"]
    }

    override fun refreshScreen() {
        binding.imageView.setImageBitmap(
            barcodeEncoder.encodeBitmap(
                args.qrCodeValue,
                BarcodeFormat.QR_CODE,
                qrCodeSize,
                qrCodeSize
            )
        )
        binding.textView.text = args.qrCodeValueDisplayed
    }
}