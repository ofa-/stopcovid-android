/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.Fragment
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.openAppSettings
import com.lunabeestudio.stopcovid.model.CovidException
import com.lunabeestudio.stopcovid.model.DeviceSetup
import com.lunabeestudio.stopcovid.widgetshomescreen.ProximityWidget

object ProximityManager {

    fun isProximityOn(context: Context, robertManager: RobertManager): Boolean {
        ProximityWidget.updateWidget(context)
        return robertManager.isProximityActive && getDeviceSetup(context, robertManager) == DeviceSetup.BLE
    }

    fun getDeviceSetup(context: Context, robertManager: RobertManager): DeviceSetup = when {
        isNotificationOn(context)
            && isLocalisationGranted(context)
            && hasFeatureBLE(context, robertManager)
            && isBluetoothOn(context, robertManager)
            && isBatteryOptimizationOff(context)
            && isAdvertisingValid(robertManager)
            && !needLocalisationTurnedOn(context) -> DeviceSetup.BLE
        !hasFeatureBLE(context, robertManager) -> DeviceSetup.NO_BLE
        else -> DeviceSetup.NOT_SETUP
    }

    fun hasFeatureBLE(context: Context, robertManager: RobertManager): Boolean {
        val hasBLESystemFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val isDeviceSupported = robertManager.configuration.unsupportedDevices?.contains(Build.MODEL) != true
        return isDeviceSupported && hasBLESystemFeature
    }

    fun isAdvertisingValid(robertManager: RobertManager): Boolean {
        return BluetoothAdapter.getDefaultAdapter()?.bluetoothLeAdvertiser != null
            || robertManager.configuration.allowNoAdvertisingDevice
    }

    fun hasUnstableBluetooth(): Boolean {
        return BluetoothAdapter.getDefaultAdapter()?.bluetoothLeAdvertiser == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.N
    }

    private fun isNotificationOn(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun isLocalisationGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, getManifestLocationPermission()) == PackageManager.PERMISSION_GRANTED

    fun getManifestLocationPermission(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Manifest.permission.ACCESS_FINE_LOCATION
    } else {
        Manifest.permission.ACCESS_COARSE_LOCATION
    }

    private fun needLocalisationTurnedOn(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT == 29 && BluetoothAdapter.getDefaultAdapter()?.isOffloadedScanBatchingSupported != true) {
            !LocationManagerCompat.isLocationEnabled(context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        } else {
            false
        }
    }

    fun isBluetoothOn(context: Context, robertManager: RobertManager): Boolean = hasFeatureBLE(
        context,
        robertManager
    ) && BluetoothAdapter.getDefaultAdapter()?.isEnabled != false

    fun isBatteryOptimizationOff(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
            || pm?.isIgnoringBatteryOptimizations(context.packageName) == true
            || !hasActivityToResolveIgnoreBatteryOptimization(context)
    }

    fun getErrorClickListener(
        fragment: Fragment,
        robertManager: RobertManager,
        activityResultLauncher: ActivityResultLauncher<Intent>?,
        serviceError: CovidException?,
        activateProximity: () -> Unit,
        restartProximity: () -> Unit,
    ): View.OnClickListener? = when {
        !hasFeatureBLE(fragment.requireContext(), robertManager) -> null
        !isNotificationOn(fragment.requireContext()) || !isLocalisationGranted(fragment.requireContext()) -> View.OnClickListener {
            fragment.openAppSettings()
        }
        hasFeatureBLE(fragment.requireContext(), robertManager) && !isBluetoothOn(
            fragment.requireContext(),
            robertManager
        ) -> View.OnClickListener {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            fragment.startActivity(enableBtIntent)
        }
        !isAdvertisingValid(robertManager) -> null
        !isBatteryOptimizationOff(fragment.requireContext()) -> View.OnClickListener {
            requestIgnoreBatteryOptimization(fragment, activityResultLauncher)
        }
        needLocalisationTurnedOn(fragment.requireContext()) -> View.OnClickListener {
            fragment.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        serviceError != null -> View.OnClickListener {
            restartProximity()
        }
        else -> View.OnClickListener {
            activateProximity()
        }
    }

    private val powerManagerIntents = arrayOf(
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        Intent().setComponent(ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        ),
        Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
        Intent().setComponent(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
            )
        ),
        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
        Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
        Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
        Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
        Intent().setComponent(ComponentName("com.htc.pitroad", "com.htc.pitroad.landingpage.activity.LandingPageActivity")),
        Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.MainActivity"))
    )

    @SuppressLint("InlinedApi")
    private fun getIgnoreBatteryOptimizationIntents(context: Context): List<Intent> {
        val miuiIntent = Intent("miui.intent.action.HIDDEN_APPS_CONFIG_ACTIVITY")
        miuiIntent.putExtra("package_name", context.packageName)
        miuiIntent.putExtra("package_label", context.getString(R.string.app_name))
        val systemIntent = Intent()
        systemIntent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        systemIntent.data = Uri.parse("package:${context.packageName}")
        val powerIntents = arrayListOf(miuiIntent, systemIntent)
        powerIntents.addAll(powerManagerIntents)
        return powerIntents
    }

    fun requestIgnoreBatteryOptimization(fragment: Fragment, activityResultLauncher: ActivityResultLauncher<Intent>?) {
        val powerIntents = getIgnoreBatteryOptimizationIntents(fragment.requireContext())
        for (intent in powerIntents) {
            val resolveInfo = intent.resolveActivityInfo(fragment.requireContext().packageManager, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo?.exported == true) {
                activityResultLauncher?.launch(intent)
                break
            }
        }
    }

    private fun hasActivityToResolveIgnoreBatteryOptimization(context: Context): Boolean {
        val powerIntents = getIgnoreBatteryOptimizationIntents(context)
        for (intent in powerIntents) {
            intent.apply {
                putExtra("package_name", context.packageName)
                putExtra("package_label", context.getString(R.string.app_name))
            }
            val resolveInfo = intent.resolveActivityInfo(context.packageManager, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo?.exported == true) {
                return true
            }
        }
        return false
    }

    fun getErrorText(
        fragment: Fragment,
        robertManager: RobertManager,
        serviceError: CovidException?,
        strings: Map<String, String>,
    ): String? = when {
        !hasFeatureBLE(fragment.requireContext(), robertManager) -> strings["proximityController.error.noBLE"]
        !isNotificationOn(fragment.requireContext())
            && !isBluetoothOn(fragment.requireContext(), robertManager)
            && !isLocalisationGranted(fragment.requireContext()) ->
            strings["proximityController.error.noNotificationsOrBluetoothOrLocalisation"]
        !isBluetoothOn(fragment.requireContext(), robertManager) && !isLocalisationGranted(fragment.requireContext()) ->
            strings["proximityController.error.noBluetoothOrLocalisation"]
        !isNotificationOn(fragment.requireContext()) && !isLocalisationGranted(fragment.requireContext()) ->
            strings["proximityController.error.noNotificationsOrLocalisation"]
        !isNotificationOn(fragment.requireContext()) && !isBluetoothOn(
            fragment.requireContext(),
            robertManager
        ) -> strings["proximityController.error.noNotificationsOrBluetooth"]
        !isNotificationOn(fragment.requireContext()) -> strings["proximityController.error.noNotifications"]
        !isLocalisationGranted(fragment.requireContext()) -> strings["proximityController.error.noLocalisation"]
        !isBluetoothOn(fragment.requireContext(), robertManager) -> strings["proximityController.error.noBluetooth"]
        !isAdvertisingValid(robertManager) -> strings["proximityController.error.noAdvertising"]
        !isBatteryOptimizationOff(fragment.requireContext()) -> strings["proximityController.error.noBattery"]
        needLocalisationTurnedOn(fragment.requireContext()) -> strings["proximityController.error.batchLocalisation"]
        !robertManager.isProximityActive -> strings["proximityController.error.activateProximity"]
        serviceError != null -> strings["common.error.bleScanner"]
        else -> null
    }
}