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

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.openAppSettings
import com.lunabeestudio.stopcovid.coreui.extension.showPermissionSettingsDialog
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.mikepenz.fastadapter.GenericItem

class OnBoardingProximityFragment : OnBoardingFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }
    private var permissionResultLauncher: ActivityResultLauncher<Array<String>>? = null
    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null

    override fun getTitleKey(): String = "onboarding.proximityController.title"
    override fun getButtonTitleKey(): String = "onboarding.proximityController.allowProximity"
    override fun getOnButtonClick(): () -> Unit = {
        if (!ProximityManager.isProximityGranted(requireContext())) {
            if (ProximityManager.isLocationRequired()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["common.permissionsNeeded"])
                    .setMessage(strings["onboarding.proximityController.allowProximity.warning"])
                    .setPositiveButton(strings["common.understand"]) { _, _ ->
                        permissionResultLauncher?.launch(ProximityManager.getManifestProximityPermissions())
                    }
                    .show()
            } else {
                permissionResultLauncher?.launch(ProximityManager.getManifestProximityPermissions())
            }
        } else if (ProximityManager.hasFeatureBLE(requireContext(), robertManager) && !ProximityManager.isBluetoothOn(
                requireContext(),
                robertManager
            )
        ) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activityResultLauncher?.launch(enableBtIntent)
        } else {
            startNextController()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionResultLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->
            if (isGranted.all { it.value }) {
                if (ProximityManager.hasFeatureBLE(requireContext(), robertManager) && !ProximityManager.isBluetoothOn(
                        requireContext(),
                        robertManager
                    )
                ) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    activityResultLauncher?.launch(enableBtIntent)
                } else {
                    startNextController()
                }
            } else if (ProximityManager.getManifestProximityPermissions()
                .any { permission -> !shouldShowRequestPermissionRationale(permission) }
            ) {
                context?.showPermissionSettingsDialog(
                    strings = strings,
                    messageKey = ProximityManager.getProximityPermissionExplanationKey(),
                    positiveKey = "common.settings",
                    neutralKey = "common.readMore",
                    cancelable = true,
                    positiveAction = {
                        openAppSettings()
                    },
                    neutralAction = {
                        strings["common.privacyPolicy"]?.openInExternalBrowser(requireContext())
                    },
                    negativeAction = null
                )
            }
        }
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startNextController()
            }
        }
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.proximity
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["onboarding.proximityController.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += captionItem {
            text = strings["onboarding.proximityController.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }

        return items
    }

    private fun startNextController() {
        context?.let { context ->
            if (ProximityManager.hasFeatureBLE(context, robertManager)) {
                if (!ProximityManager.isBatteryOptimizationOff(requireContext())) {
                    findNavControllerOrNull()?.safeNavigate(
                        OnBoardingProximityFragmentDirections.actionOnBoardingProximityFragmentToOnBoardingBatteryFragment()
                    )
                } else {
                    findNavControllerOrNull()?.safeNavigate(
                        OnBoardingProximityFragmentDirections.actionOnBoardingProximityFragmentToOnBoardingNotificationFragment()
                    )
                }
            } else {
                findNavControllerOrNull()
                    ?.safeNavigate(OnBoardingProximityFragmentDirections.actionOnBoardingProximityFragmentToOnBoardingNoBleFragment())
            }
        }
    }
}
