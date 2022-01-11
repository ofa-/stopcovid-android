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
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.lunabeestudio.domain.model.TacResult
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentWalletFullscreenLegacyDccBinding
import com.lunabeestudio.stopcovid.extension.collectWithLifecycle
import com.lunabeestudio.stopcovid.extension.fullDescription
import com.lunabeestudio.stopcovid.extension.fullNameUppercase
import com.lunabeestudio.stopcovid.extension.fullScreenBorderDescription
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.isFrench
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.stringKey
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.EuropeanCertificate
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import kotlinx.coroutines.flow.map

class WalletFullscreenLegacyDccFragment : BaseFragment() {

    private val args: WalletFullscreenLegacyDccFragmentArgs by navArgs()

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
        binding = FragmentWalletFullscreenLegacyDccBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appCompatActivity?.supportActionBar?.title = strings["walletController.title"]
        viewModel.certificates.map { certificates ->
            certificates.mapData { list ->
                list
                    ?.filterIsInstance<EuropeanCertificate>()
                    ?.firstOrNull { it.id == args.id }
            }
        }.collectWithLifecycle(viewLifecycleOwner) { result ->
            val mainActivity = activity as? MainActivity
            when (result) {
                is TacResult.Failure -> {
                    mainActivity?.showProgress(false)
                    strings["walletFullscreenController.error.certificateNotFound"]?.let { mainActivity?.showErrorSnackBar(it) }
                    findNavControllerOrNull()?.popBackStack()
                }
                is TacResult.Loading -> mainActivity?.showProgress(true)
                is TacResult.Success -> {
                    mainActivity?.showProgress(false)
                    this@WalletFullscreenLegacyDccFragment.europeanCertificate = result.successData
                    refreshScreen()
                }
            }
        }
        binding.detailsTextSwitcher.setInAnimation(view.context, R.anim.fade_in)
        binding.detailsTextSwitcher.setOutAnimation(view.context, R.anim.fade_out)
        binding.explanationTextSwitcher.setInAnimation(view.context, R.anim.fade_in)
        binding.explanationTextSwitcher.setOutAnimation(view.context, R.anim.fade_out)
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
            R.id.qr_code_menu_more -> {
                showQrCodeMoreActionBottomSheet()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showCertificateSharingBottomSheet() {
        val text = europeanCertificate?.fullDescription(strings, injectionContainer.robertManager.configuration, context)
        ShareManager.setupCertificateSharingBottomSheet(this, text) {
            binding.barcodeSecuredView.runUnsecured {
                binding.let { ShareManager.getShareCaptureUri(it, ShareManager.certificateScreenshotFilename) }
            }
        }
        findNavControllerOrNull()?.safeNavigate(
            WalletFullscreenLegacyDccFragmentDirections.actionLegacyFullscreenDccFragmentToCertificateSharingBottomSheetFragment()
        )
    }

    private fun showQrCodeMoreActionBottomSheet() {
        setFragmentResultListener(QrCodeMoreActionBottomSheetFragment.MORE_ACTION_RESULT_KEY) { _, bundle ->
            if (bundle.getBoolean(QrCodeMoreActionBottomSheetFragment.MORE_ACTION_BUNDLE_KEY_SHARE_REQUESTED, false)) {
                findNavControllerOrNull()?.addOnDestinationChangedListener(
                    object : NavController.OnDestinationChangedListener {
                        override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
                            if (controller.currentDestination?.id == R.id.legacyFullscreenDccFragment) {
                                showCertificateSharingBottomSheet()
                                controller.removeOnDestinationChangedListener(this)
                            }
                        }
                    })
            }
        }

        findNavControllerOrNull()?.safeNavigate(
            WalletFullscreenLegacyDccFragmentDirections.actionLegacyFullscreenDccFragmentToQrCodeMoreActionBottomSheetFragment(
                showShare = true,
                showBrightness = true
            )
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
                europeanCertificate.fullScreenBorderDescription(
                    strings = strings,
                    configuration = injectionContainer.robertManager.configuration
                )
            )
            explanationTextSwitcher.setCurrentText("")
            explanationTextSwitcher.setText(europeanCertificate.sha256)
            headerTextView.setTextOrHide(strings["europeanCertificate.fullscreen.${europeanCertificate.type.stringKey}.border.warning"])
        } else {
            detailsTextSwitcher.setCurrentText("")
            detailsTextSwitcher.setText(europeanCertificate.fullNameUppercase())
            explanationTextSwitcher.setCurrentText("")
            explanationTextSwitcher.setText(strings["europeanCertificate.fullscreen.type.minimum.footer"])
            headerTextView.isVisible = false
        }
    }
}
