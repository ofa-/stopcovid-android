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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentFullscreenAttestationBinding
import com.lunabeestudio.stopcovid.extension.safeNavigate

class FullscreenAttestationFragment : BaseFragment() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fullscreen_attestation_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.qr_code_menu_more -> {
                showQrCodeMoreActionBottomSheet()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFullscreenAttestationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

    private fun showQrCodeMoreActionBottomSheet() {
        findNavControllerOrNull()?.safeNavigate(
            FullscreenAttestationFragmentDirections.actionFullscreenAttestationFragmentToQrCodeMoreActionBottomSheetFragment(
                showShare = false,
                showBrightness = true
            )
        )
    }
}
