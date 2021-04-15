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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.navigation.fragment.navArgs
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentFullscreenQrcodeBinding
import com.lunabeestudio.stopcovid.extension.setTextOrHide

class FullscreenQRCodeFragment : BaseFragment() {

    private val args: FullscreenQRCodeFragmentArgs by navArgs()

    private val barcodeEncoder = BarcodeEncoder()
    private val qrCodeSize by lazy {
        R.dimen.qr_code_fullscreen_size.toDimensSize(requireContext()).toInt()
    }

    private var binding: FragmentFullscreenQrcodeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFullscreenQrcodeBinding.inflate(inflater, container, false)
        appCompatActivity?.supportActionBar?.title = null
        return binding?.root
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val params: WindowManager.LayoutParams? = activity?.window?.attributes
        params?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        activity?.window?.attributes = params
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val params: WindowManager.LayoutParams? = activity?.window?.attributes
        params?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        activity?.window?.attributes = params
    }

    override fun refreshScreen() {
        binding?.imageView?.setImageBitmap(
            barcodeEncoder.encodeBitmap(
                args.qrCodeValue,
                args.qrCodeFormat,
                qrCodeSize,
                qrCodeSize
            )
        )
        binding?.formatTextView?.setTextOrHide(formatText())
        binding?.textView?.text = args.qrCodeValueDisplayed
    }

    private fun formatText(): String? {
        return when (args.qrCodeFormat) {
            BarcodeFormat.DATA_MATRIX -> Constants.QrCode.FORMAT_2D_DOC
            else -> null
        }
    }
}