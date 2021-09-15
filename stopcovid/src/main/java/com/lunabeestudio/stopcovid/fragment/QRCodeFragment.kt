/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.ui.NavigationUI
import com.journeyapps.barcodescanner.BarcodeResult
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.openAppSettings
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.extension.showPermissionRationale
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentQrCodeBinding
import com.lunabeestudio.stopcovid.extension.emitDefaultKonfetti
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser

abstract class QRCodeFragment : BaseFragment() {

    private var permissionResultLauncher: ActivityResultLauncher<String>? = null
    abstract fun getTitleKey(): String
    abstract val explanationKey: String
    abstract val footerKey: String?
    abstract fun onFooterClick()
    abstract fun onCodeScanned(code: String)
    protected var isReadyToStartScanFlow: Boolean = true

    protected var binding: FragmentQrCodeBinding? = null
    private var showingRationale: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                resumeQrCodeReader()
            } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showingRationale = true
                context?.showPermissionRationale(
                    strings = strings,
                    messageKey = "common.needCameraAccessToScan",
                    positiveKey = "common.settings",
                    neutralKey = "common.readMore",
                    cancelable = false,
                    positiveAction = {
                        openAppSettings()
                        showingRationale = false
                    },
                    neutralAction = {
                        strings["common.privacyPolicy"]?.openInExternalBrowser(requireContext())
                        showingRationale = false
                    },
                    negativeAction = {
                        findNavControllerOrNull()?.navigateUp()
                        showingRationale = false
                    }
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentQrCodeBinding.inflate(inflater, container, false)
        setupToolbar()
        return binding?.root
    }

    private fun setupToolbar() {
        // remove shadow
        binding?.appBarLayout?.outlineProvider = null
        // replace previous activity action bar
        appCompatActivity?.setSupportActionBar(binding?.toolbar)
        // show back arrow
        appCompatActivity?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // no title
        appCompatActivity?.supportActionBar?.title = null

        binding?.toolbar?.let { toolbar ->
            findNavControllerOrNull()?.let { navController ->
                NavigationUI.setupWithNavController(
                    toolbar,
                    navController
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isReadyToStartScanFlow) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                if (!showingRationale) {
                    showingRationale = true
                    context?.showPermissionRationale(
                        strings = strings,
                        messageKey = "common.needCameraAccessToScan",
                        positiveKey = "common.ok",
                        neutralKey = "common.readMore",
                        cancelable = false,
                        positiveAction = {
                            permissionResultLauncher?.launch(Manifest.permission.CAMERA)
                            showingRationale = false
                        },
                        neutralAction = {
                            strings["common.privacyPolicy"]?.openInExternalBrowser(requireContext())
                            showingRationale = false
                        },
                        negativeAction = {
                            findNavControllerOrNull()?.navigateUp()
                            showingRationale = false
                        }
                    )
                }
            } else {
                resumeQrCodeReader()
            }
        }
    }

    fun resumeQrCodeReader() {
        binding?.qrCodeReaderView?.resume()
        binding?.qrCodeReaderView?.decodeContinuous { result: BarcodeResult? ->
            result?.text?.let { code ->
                if (code == ConfigConstant.Store.TAC_WEBSITE || code == ConfigConstant.Store.STOPCOVID_WEBSITE) {
                    if (binding?.konfettiView?.isActive() == false) {
                        binding?.konfettiView?.emitDefaultKonfetti(binding)
                    }
                } else {
                    binding?.qrCodeReaderView?.stopDecoding()
                    onCodeScanned(code)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding?.qrCodeReaderView?.pause()
        binding?.qrCodeReaderView?.stopDecoding()
    }

    override fun refreshScreen() {
        binding?.title?.setTextOrHide(strings[explanationKey])
        binding?.footer?.setTextOrHide(footerKey?.let(strings::get).takeIf { !it.isNullOrBlank() })
        binding?.footer?.setOnClickListener { onFooterClick() }
    }

    protected val analyticsManager: AnalyticsManager by lazy(LazyThreadSafetyMode.NONE) {
        injectionContainer.analyticsManager
    }
}