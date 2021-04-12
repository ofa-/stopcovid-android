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
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.BarcodeResult
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.openAppSettings
import com.lunabeestudio.stopcovid.coreui.extension.showPermissionRationale
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentQrCodeBinding
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser

abstract class QRCodeFragment : BaseFragment() {

    abstract fun getTitleKey(): String
    abstract fun getExplanationKey(): String
    abstract fun onCodeScanned(code: String)
    protected var isReadyToStartScanFlow: Boolean = true

    protected var binding: FragmentQrCodeBinding? = null
    private var showingRationale: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentQrCodeBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onResume() {
        super.onResume()
        if (isReadyToStartScanFlow) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                if (!showingRationale) {
                    showingRationale = true
                    context?.showPermissionRationale(
                        strings = strings,
                        messageKey = "common.needCameraAccessToScan",
                        positiveKey = "common.ok",
                        neutralKey = "common.readMore",
                        cancelable = false,
                        positiveAction = {
                            requestPermissions(
                                arrayOf(Manifest.permission.CAMERA),
                                UiConstants.Permissions.CAMERA.ordinal
                            )
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
            binding?.qrCodeReaderView?.stopDecoding()
            result?.text?.let { code ->
                onCodeScanned(code)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding?.qrCodeReaderView?.pause()
        binding?.qrCodeReaderView?.stopDecoding()
    }

    override fun refreshScreen() {
        appCompatActivity?.supportActionBar?.title = strings[getTitleKey()]
        binding?.title?.text = strings[getExplanationKey()]
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == UiConstants.Permissions.CAMERA.ordinal) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
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
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}