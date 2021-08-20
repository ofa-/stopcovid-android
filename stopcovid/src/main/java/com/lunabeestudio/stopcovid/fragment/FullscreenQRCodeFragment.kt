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
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.databinding.FragmentFullscreenQrcodeBinding

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import kotlin.math.max
import kotlin.math.min

class FullscreenQRCodeFragment : ForceLightFragment(R.layout.fragment_fullscreen_qrcode) {

    private val args: FullscreenQRCodeFragmentArgs by navArgs()

    private val barcodeEncoder = BarcodeEncoder()
    private val qrCodeSize by lazy {
        R.dimen.qr_code_fullscreen_size.toDimensSize(requireContext()).toInt()
    }

    private var binding: FragmentFullscreenQrcodeBinding? = null

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFullscreenQrcodeBinding.bind(view)

        scaleGestureDetector = ScaleGestureDetector(requireActivity(), ScaleListener())
        binding?.root?.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            scaleGestureDetector.onTouchEvent(motionEvent)
        })
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
       override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
          scaleFactor *= scaleGestureDetector.scaleFactor
          scaleFactor = max(0.1f, min(scaleFactor, 10.0f))
          binding?.imageView?.scaleX = scaleFactor
          binding?.imageView?.scaleY = scaleFactor
          return true
       }
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
        binding?.topSpace?.isVisible = args.qrCodeFormat == BarcodeFormat.DATA_MATRIX
        binding?.formatTextView?.setTextOrHide(formatText())
        binding?.textView?.text = ""
        binding?.sha256TextView?.setTextOrHide("")
    }

    private fun formatText(): String? {
        return when (args.qrCodeFormat) {
            BarcodeFormat.DATA_MATRIX -> Constants.QrCode.FORMAT_2D_DOC
            else -> null
        }
    }
}
