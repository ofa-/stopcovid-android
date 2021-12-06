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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentWalletFullscreen2ddocBinding
import com.lunabeestudio.stopcovid.extension.fullDescription
import com.lunabeestudio.stopcovid.extension.fullNameUppercase
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.FrenchCertificate
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory

class WalletFullscreen2DdocFragment : BaseFragment() {

    private val args: WalletFullscreen2DdocFragmentArgs by navArgs()

    private val viewModel: WalletViewModel by navGraphViewModels(R.id.nav_wallet) {
        WalletViewModelFactory(
            requireContext().robertManager(),
            injectionContainer.blacklistDCCManager,
            injectionContainer.blacklist2DDOCManager,
            injectionContainer.walletRepository,
            injectionContainer.generateActivityPassUseCase,
            injectionContainer.getSmartWalletCertificateUseCase,
        )
    }

    private var frenchCertificate: FrenchCertificate? = null

    private val barcodeEncoder = BarcodeEncoder()
    private val qrCodeSize by lazy {
        R.dimen.qr_code_fullscreen_size.toDimensSize(requireContext()).toInt()
    }

    private lateinit var binding: FragmentWalletFullscreen2ddocBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWalletFullscreen2ddocBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appCompatActivity?.supportActionBar?.title = strings["walletController.title"]
        viewModel.certificates.asLiveData(timeoutInMs = 0).map { certificates ->
            certificates
                ?.filterIsInstance<FrenchCertificate>()
                ?.firstOrNull { it.id == args.id }
        }
            .observe(viewLifecycleOwner) { frenchCertificate ->
                this.frenchCertificate = frenchCertificate
                refreshScreen()
            }

        binding.formatTextView.text = Constants.QrCode.FORMAT_2D_DOC

        binding.shareButton.text = strings["common.share"]
        binding.shareButton.setOnClickListener {
            showCertificateSharingBottomSheet()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fullscreen_qr_code_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.qr_code_menu_share -> {
                showCertificateSharingBottomSheet()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun refreshScreen() {
        val frenchCertificate = frenchCertificate ?: return

        binding.barcodeSecuredView.bitmap =
            barcodeEncoder.encodeBitmap(
                frenchCertificate.value,
                BarcodeFormat.DATA_MATRIX,
                qrCodeSize,
                qrCodeSize
            )
        binding.detailsTextView.text = frenchCertificate.fullNameUppercase()
        binding.sha256TextView.text = frenchCertificate.sha256
    }

    fun showCertificateSharingBottomSheet() {
        val activityBinding = (activity as? MainActivity)?.binding ?: return
        val text = frenchCertificate?.fullDescription(strings, injectionContainer.robertManager.configuration)
        ShareManager.setupCertificateSharingBottomSheet(this, text) {
            binding.barcodeSecuredView.runUnsecured {
                ShareManager.getShareCaptureUri(activityBinding, ShareManager.certificateScreenshotFilename)
            }
        }
        findNavControllerOrNull()?.safeNavigate(
            WalletFullscreen2DdocFragmentDirections.actionFullscreenQRCodeFragmentToCertificateSharingBottomSheetFragment()
        )
    }
}