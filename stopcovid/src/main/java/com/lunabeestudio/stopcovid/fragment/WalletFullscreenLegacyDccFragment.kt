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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.databinding.FragmentWalletFullscreenLegacyDccBinding
import com.lunabeestudio.stopcovid.extension.formatDccText
import com.lunabeestudio.stopcovid.extension.fullDescription
import com.lunabeestudio.stopcovid.extension.fullName
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.isFrench
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.stringKey
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

class WalletFullscreenLegacyDccFragment : ForceLightFragment(R.layout.fragment_wallet_fullscreen_legacy_dcc) {

    private val args: WalletFullscreenLegacyDccFragmentArgs by navArgs()

    private val viewModel: WalletViewModel by navGraphViewModels(R.id.nav_wallet) {
        WalletViewModelFactory(
            requireContext().robertManager(),
            injectionContainer.blacklistDCCManager,
            injectionContainer.blacklist2DDOCManager,
            injectionContainer.walletRepository,
            injectionContainer.generateActivityPassUseCase,
        )
    }

    private val barcodeEncoder = BarcodeEncoder()
    private val qrCodeSize by lazy {
        R.dimen.qr_code_fullscreen_size.toDimensSize(requireContext()).toInt()
    }

    private var europeanCertificate: EuropeanCertificate? = null

    private lateinit var binding: FragmentWalletFullscreenLegacyDccBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentWalletFullscreenLegacyDccBinding.bind(rootView)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appCompatActivity?.supportActionBar?.title = strings["walletController.title"]
        viewModel.certificates.asLiveData(timeoutInMs = 0).map { certificates ->
            certificates
                ?.filterIsInstance<EuropeanCertificate>()
                ?.firstOrNull { it.id == args.id }
        }
            .observe(viewLifecycleOwner) { europeanCertificate ->
                this.europeanCertificate = europeanCertificate
                refreshScreen()
            }
        binding.detailsTextSwitcher.setInAnimation(view.context, R.anim.fade_in)
        binding.detailsTextSwitcher.setOutAnimation(view.context, R.anim.fade_out)
        binding.explanationTextSwitcher.setInAnimation(view.context, R.anim.fade_in)
        binding.explanationTextSwitcher.setOutAnimation(view.context, R.anim.fade_out)

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

    private fun showCertificateSharingBottomSheet() {
        val text = europeanCertificate?.fullDescription(strings, injectionContainer.robertManager.configuration)
        ShareManager.setupCertificateSharingBottomSheet(this, text) {
            binding.barcodeSecuredView.runUnsecured {
                binding.let { ShareManager.getShareCaptureUri(it, ShareManager.certificateScreenshotFilename) }
            }
        }
        findNavControllerOrNull()?.safeNavigate(
            WalletFullscreenLegacyDccFragmentDirections.actionLegacyFullscreenDccFragmentToCertificateSharingBottomSheetFragment()
        )
    }

    override fun refreshScreen() {
        val europeanCertificate = europeanCertificate ?: return

        binding.apply {
            logosImageView.isVisible = europeanCertificate.greenCertificate.isFrench
            showMoreSwitch.text = strings["europeanCertificate.fullscreen.type.border.switch"]
            showMoreSwitch.setOnCheckedChangeListener { _, isChecked ->
                refreshDetails(isChecked, europeanCertificate)
            }
            refreshDetails(showMoreSwitch.isChecked, europeanCertificate)
            barcodeSecuredView.bitmap =
                barcodeEncoder.encodeBitmap(
                    europeanCertificate.value,
                    BarcodeFormat.QR_CODE,
                    qrCodeSize,
                    qrCodeSize
                )
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun FragmentWalletFullscreenLegacyDccBinding.refreshDetails(
        isBorder: Boolean,
        europeanCertificate: EuropeanCertificate
    ) {
        if (isBorder) {
            detailsTextSwitcher.setCurrentText("")
            detailsTextSwitcher.setText(
                europeanCertificate.formatDccText(
                    strings["europeanCertificate.fullscreen.englishDescription.${europeanCertificate.type.code}"],
                    strings,
                    SimpleDateFormat("d MMM yyyy", Locale.ENGLISH),
                    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH),
                )
            )
            explanationTextSwitcher.setCurrentText("")
            explanationTextSwitcher.setText("")
            headerTextView.setTextOrHide("")
        } else {
            detailsTextSwitcher.setCurrentText("")
            detailsTextSwitcher.setText("")
            explanationTextSwitcher.setCurrentText("")
            explanationTextSwitcher.setText("")
            headerTextView.isVisible = false
        }
    }
}
