/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.journeyapps.barcodescanner.BarcodeResult
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentQrCodeBinding

class QRCodeFragment : BaseFragment() {

    private var binding: FragmentQrCodeBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentQrCodeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onResume() {
        super.onResume()
        binding?.qrCodeReaderView?.resume()
        binding?.qrCodeReaderView?.decodeContinuous { result: BarcodeResult? ->
            binding?.qrCodeReaderView?.stopDecoding()
            result?.text?.let { code ->
                findNavController().navigate(QRCodeFragmentDirections.actionQrCodeFragmentToSymptomsOriginFragment(code))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding?.qrCodeReaderView?.pause()
        binding?.qrCodeReaderView?.stopDecoding()
    }

    override fun refreshScreen() {
        binding?.title?.text = strings["scanCodeController.explanation"]
    }
}