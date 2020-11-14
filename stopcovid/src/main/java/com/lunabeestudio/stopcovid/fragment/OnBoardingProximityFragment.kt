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
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.view.Gravity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.openAppSettings
import com.lunabeestudio.stopcovid.coreui.extension.showPermissionRationale
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.setAdvertisementAvailable
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class OnBoardingProximityFragment : OnBoardingFragment() {

    override fun getTitleKey(): String = "onboarding.proximityController.title"
    override fun getButtonTitleKey(): String = "onboarding.proximityController.allowProximity"
    override fun getOnButtonClick(): () -> Unit = {
        if (ContextCompat.checkSelfPermission(requireContext(), ProximityManager.getManifestLocationPermission())
            != PackageManager.PERMISSION_GRANTED) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(strings["common.permissionsNeeded"])
                .setMessage(strings["onboarding.proximityController.allowProximity.warning"])
                .setPositiveButton(strings["common.understand"]) { _, _ ->
                    requestPermissions(
                        arrayOf(ProximityManager.getManifestLocationPermission()),
                        UiConstants.Permissions.LOCATION.ordinal
                    )
                }
                .show()
        } else if (ProximityManager.hasFeatureBLE(requireContext()) && !ProximityManager.isBluetoothOn(requireContext())) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, UiConstants.Activity.BLUETOOTH.ordinal)
        } else {
            startNextController()
        }
    }

    override fun getItems(): List<GenericItem> {
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == UiConstants.Permissions.LOCATION.ordinal) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (ProximityManager.hasFeatureBLE(requireContext()) && !ProximityManager.isBluetoothOn(requireContext())) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, UiConstants.Activity.BLUETOOTH.ordinal)
                } else {
                    startNextController()
                }
            } else if (!shouldShowRequestPermissionRationale(ProximityManager.getManifestLocationPermission())) {
                context?.showPermissionRationale(strings, "common.needLocalisationAccessToScan", "common.settings") {
                    openAppSettings()
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == UiConstants.Activity.BLUETOOTH.ordinal) {
            if (resultCode == Activity.RESULT_OK) {
                startNextController()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun startNextController() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            getActivityBinding().blockingProgressBar.show()
            val success = isAdvertisementAvailable()
            getActivityBinding().blockingProgressBar.hide()

            PreferenceManager.getDefaultSharedPreferences(context).setAdvertisementAvailable(success)

            if (success) {
                if (!ProximityManager.isBatteryOptimizationOff(requireContext())) {
                    findNavController()
                        .safeNavigate(OnBoardingProximityFragmentDirections.actionOnBoardingProximityFragmentToOnBoardingBatteryFragment())
                } else {
                    findNavController()
                        .safeNavigate(OnBoardingProximityFragmentDirections.actionOnBoardingProximityFragmentToOnBoardingNotificationFragment())
                }
            } else {

                findNavController()
                    .safeNavigate(OnBoardingProximityFragmentDirections.actionOnBoardingProximityFragmentToOnBoardingNoBleFragment())
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun isAdvertisementAvailable(): Boolean {
        return withTimeoutOrNull(ADVERTISEMENT_AVAILABLE_TIMEOUT) {
            suspendCancellableCoroutine { continuation ->
                val bluetoothLeAdvertiser = BluetoothAdapter.getDefaultAdapter()?.bluetoothLeAdvertiser
                if (bluetoothLeAdvertiser != null) {
                    bluetoothLeAdvertiser.startAdvertising(
                        buildAdvertiseSettings(),
                        buildAdvertiseData(),
                        object : AdvertiseCallback() {
                            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                                bluetoothLeAdvertiser.stopAdvertising(this)
                                continuation.resume(true)
                            }

                            override fun onStartFailure(errorCode: Int) {
                                bluetoothLeAdvertiser.stopAdvertising(this)
                                Timber.e("Advertisement not supported (Error code : $errorCode)")
                                continuation.resume(errorCode != ADVERTISE_FAILED_FEATURE_UNSUPPORTED)
                            }
                        }
                    )
                } else {
                    continuation.resume(false)
                }
            }
        } ?: false
    }

    private fun buildAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setTimeout(0)
            .build()
    }

    private fun buildAdvertiseData(): AdvertiseData {
        return AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(UUID.randomUUID()))
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()
    }

    companion object {
        @OptIn(ExperimentalTime::class)
        private val ADVERTISEMENT_AVAILABLE_TIMEOUT = 8.seconds
    }
}
