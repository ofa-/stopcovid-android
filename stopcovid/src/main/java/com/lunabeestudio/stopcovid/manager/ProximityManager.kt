/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.openAppSettings

object ProximityManager {

    fun isProximityOn(context: Context, robertManager: RobertManager): Boolean =
        robertManager.isProximityActive && isPhoneSetup(context)

    fun isPhoneSetup(context: Context): Boolean = isNotificationOn(context)
        && isLocalisationGranted(context)
        && hasFeatureBLE(context)
        && isBluetoothOn(context)
        && isBatteryOptimizationOn(context)

    fun hasFeatureBLE(context: Context): Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    private fun isNotificationOn(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun isLocalisationGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, getManifestLocationPermission()) == PackageManager.PERMISSION_GRANTED

    fun getManifestLocationPermission(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Manifest.permission.ACCESS_FINE_LOCATION
    } else {
        Manifest.permission.ACCESS_COARSE_LOCATION
    }

    fun isBluetoothOn(context: Context): Boolean = hasFeatureBLE(context) && BluetoothAdapter.getDefaultAdapter()?.isEnabled != false

    fun isBatteryOptimizationOn(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M
            || pm?.isIgnoringBatteryOptimizations(context.packageName) == true
            || !hasActivityToResolveIgnoreBatteryOptimization(context)
    }

    fun getErrorClickListener(fragment: Fragment, activateProximity: () -> Unit): View.OnClickListener? = when {
        !hasFeatureBLE(fragment.requireContext()) -> null
        !isNotificationOn(fragment.requireContext()) || !isLocalisationGranted(fragment.requireContext()) -> View.OnClickListener {
            fragment.openAppSettings()
        }
        hasFeatureBLE(fragment.requireContext()) && !isBluetoothOn(fragment.requireContext()) -> View.OnClickListener {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            fragment.startActivity(enableBtIntent)
        }
        !isBatteryOptimizationOn(fragment.requireContext()) -> View.OnClickListener {
            requestIgnoreBatteryOptimization(fragment)
        }
        else -> View.OnClickListener {
            activateProximity()
        }
    }

    private val powerManagerIntents = arrayOf(
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        Intent().setComponent(ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
        Intent().setComponent(ComponentName("com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
        Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
        Intent().setComponent(ComponentName("com.huawei.systemmanager",
            "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")),
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

    @SuppressLint("BatteryLife", "InlinedApi")
    fun requestIgnoreBatteryOptimization(fragment: Fragment) {
        val systemIntent = Intent()
        systemIntent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        systemIntent.data = Uri.parse("package:${fragment.requireActivity().packageName}")
        val powerIntents = arrayListOf(systemIntent)
        powerIntents.addAll(powerManagerIntents)
        for (intent in powerIntents) {
            val resolveInfo = intent.resolveActivityInfo(fragment.requireContext().packageManager, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo?.exported == true) {
                fragment.startActivityForResult(intent, UiConstants.Activity.BATTERY.ordinal)
                break
            }
        }
    }

    @SuppressLint("BatteryLife", "InlinedApi")
    private fun hasActivityToResolveIgnoreBatteryOptimization(context: Context): Boolean {
        val systemIntent = Intent()
        systemIntent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        systemIntent.data = Uri.parse("package:${context.packageName}")
        val powerIntents = arrayListOf(systemIntent)
        powerIntents.addAll(powerManagerIntents)
        for (intent in powerIntents) {
            val resolveInfo = intent.resolveActivityInfo(context.packageManager, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolveInfo?.exported == true) {
                return true
            }
        }
        return false
    }

    fun getErrorText(fragment: Fragment, robertManager: RobertManager, strings: Map<String, String>): String? = when {
        !hasFeatureBLE(fragment.requireContext()) -> strings["proximityController.error.noBLE"]
        !isNotificationOn(fragment.requireContext())
            && !isBluetoothOn(fragment.requireContext())
            && !isLocalisationGranted(fragment.requireContext()) ->
            strings["proximityController.error.noNotificationsOrBluetoothOrLocalisation"]
        !isBluetoothOn(fragment.requireContext()) && !isLocalisationGranted(fragment.requireContext()) ->
            strings["proximityController.error.noBluetoothOrLocalisation"]
        !isNotificationOn(fragment.requireContext()) && !isLocalisationGranted(fragment.requireContext()) ->
            strings["proximityController.error.noNotificationsOrLocalisation"]
        !isNotificationOn(fragment.requireContext()) && !isBluetoothOn(fragment.requireContext()) -> strings["proximityController.error.noNotificationsOrBluetooth"]
        !isNotificationOn(fragment.requireContext()) -> strings["proximityController.error.noNotifications"]
        !isLocalisationGranted(fragment.requireContext()) -> strings["proximityController.error.noLocalisation"]
        !isBluetoothOn(fragment.requireContext()) -> strings["proximityController.error.noBluetooth"]
        !isBatteryOptimizationOn(fragment.requireContext()) -> strings["proximityController.error.noBattery"]
        !robertManager.isProximityActive -> strings["proximityController.error.activateProximity"]
        else -> null
    }
}