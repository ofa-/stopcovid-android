/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.model.Info
import com.lunabeestudio.stopcovid.network.LBMaintenanceHttpClient
import okhttp3.OkHttpClient
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Main class of this lib. The singleton manager which do the work to block the app or not
 */
object AppMaintenanceManager {

    private var jsonUrl: String? = null
    private var buildNumber: Long = 0L
    private lateinit var sharedPrefs: SharedPreferences

    @DrawableRes
    var maintenanceIconRes: Int = 0
    var upgradeIconRes: Int = 0
    var infoFreeCompletion: (() -> Unit)? = null
    var infoBlockedCompletion: ((info: Info) -> Unit)? = null

    val shouldDisplayUpdateAvailable: Boolean
        get() {
            val info = getMaintenanceJson()?.let { Info(JSONObject(it)) }
            return info?.minInfoBuildNumber?.let { buildNumber < it } ?: false
        }

    /**
     * Should always be called first !
     * Prefer call this in the onCreate of your App class
     * @param context : The context used to verify the version code of the application
     * @param maintenanceIconRes : The icon display on the blocking screen when maintenance mode is active
     * @param upgradeIconRes : The icon display on the blocking screen when upgrade mode is active
     * @param jsonUrl : The url to call to get the maintenance json. If [jsonUrl] is null, it will be generate with default Lunabee repositories for maintenance json files
     * @param infoFreeCompletion : Called when the server doesn't block the app
     * @param infoBlockedCompletion : Called when the server does block the app
     */
    fun initialize(
        context: Context,
        @DrawableRes maintenanceIconRes: Int,
        @DrawableRes upgradeIconRes: Int,
        jsonUrl: String,
        infoFreeCompletion: (() -> Unit)? = null,
        infoBlockedCompletion: ((info: Info) -> Unit)? = null
    ) {
        AppMaintenanceManager.jsonUrl = jsonUrl
        buildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
        }
        sharedPrefs = context.getSharedPreferences(
            SHARED_PREFS_NAME, 0
        )
        AppMaintenanceManager.maintenanceIconRes = maintenanceIconRes
        AppMaintenanceManager.upgradeIconRes = upgradeIconRes
        AppMaintenanceManager.infoFreeCompletion = infoFreeCompletion
        AppMaintenanceManager.infoBlockedCompletion = infoBlockedCompletion
    }

    /**
     * Call this to check if the app needs to be blocked or not
     */
    suspend fun checkForMaintenanceUpgrade(context: Context, okHttpClient: OkHttpClient) {
        when {
            shouldRefresh(context) -> {
                updateCheckForMaintenanceUpgrade(
                    context,
                    okHttpClient,
                    null,
                    null
                )
            }
            else -> {
                useLastResult(
                    context,
                    null,
                    null
                )
            }
        }
    }

    /**
     * This is called by the blocking activity to "retry" in case of maintenance
     */
    internal suspend fun updateCheckForMaintenanceUpgrade(
        context: Context,
        okHttpClient: OkHttpClient,
        appIsFreeCompletion: (() -> Unit)?,
        appIsBlockedCompletion: ((info: Info) -> Unit)?
    ) {
        LBMaintenanceHttpClient.get(
            context,
            jsonUrl!!,
            okHttpClient,
            { result ->
                try {
                    val info = Info(JSONObject(result))
                    saveMaintenanceJson(result)
                    showAppMaintenanceActivityIfNeeded(
                        context,
                        info,
                        appIsFreeCompletion,
                        appIsBlockedCompletion
                    )
                    saveLastRefresh(context)
                } catch (e: Exception) {
                    // In case of a malformed JSON we don't safe it and use the last one instead
                    useLastResult(
                        context,
                        appIsFreeCompletion,
                        appIsBlockedCompletion
                    )
                }
            },
            { e ->
                useLastResult(
                    context,
                    appIsFreeCompletion,
                    appIsBlockedCompletion
                )
                Timber.e(e)
            }
        )
    }

    private suspend fun useLastResult(
        context: Context,
        appIsFreeCompletion: (() -> Unit)?,
        appIsBlockedCompletion: ((info: Info) -> Unit)?
    ) {
        val lastResult = retrieveLastMaintenanceJson()
        lastResult?.let {
            showAppMaintenanceActivityIfNeeded(
                context,
                Info(JSONObject(lastResult)),
                appIsFreeCompletion,
                appIsBlockedCompletion
            )
        }
    }

    /**
     * Construct the maintenanceInfo object and block the app if needed in showing the LBAppMaintenanceActivity
     */
    private suspend fun showAppMaintenanceActivityIfNeeded(
        context: Context,
        info: Info,
        appIsFreeCompletion: (() -> Unit)?,
        appIsBlockedCompletion: ((Info) -> Unit)?
    ) {
        if (info.isActive == true && (info.minRequiredBuildNumber ?: 0) > buildNumber) {
            startAppMaintenanceActivity(context, info)
            appIsBlockedCompletion?.invoke(info)
            context.sendBroadcast(Intent(Constants.Notification.APP_IN_MAINTENANCE))
            infoBlockedCompletion?.invoke(info)
        } else {
            appIsFreeCompletion?.invoke()
            infoFreeCompletion?.invoke()
        }
    }

    /**
     * Launch the LBAppMaintenanceActivity with attributes to display
     */
    private suspend fun startAppMaintenanceActivity(
        context: Context,
        info: Info
    ) {
        if ((context.applicationContext as StopCovid).isAppInForeground) {
            context.startActivity(
                Intent(
                    context,
                    com.lunabeestudio.stopcovid.activity.AppMaintenanceActivity::class.java
                ).apply {
                    putExtra(
                        com.lunabeestudio.stopcovid.activity.AppMaintenanceActivity.EXTRA_INFO,
                        Gson().toJson(info)
                    )
                }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            context.sendBroadcast(Intent(Constants.Notification.APP_IN_MAINTENANCE))
        } else if (info.isActive == true && info.mode == Info.Mode.UPGRADE) {
            (context.applicationContext as StopCovid).sendUpgradeNotification()
        }
    }

    /* SHARED PREFS */

    /**
     * Save the json gotten on the server
     */
    private fun saveMaintenanceJson(jsonString: String) {
        sharedPrefs.edit()
            .putString(JSON_STRING_SHARED_PREFS_KEY, jsonString)
            .apply()
    }

    private fun getMaintenanceJson(): String? {
        return sharedPrefs.getString(JSON_STRING_SHARED_PREFS_KEY, null)
    }

    /**
     * Get the last json saved if server is inaccessible
     */
    private fun retrieveLastMaintenanceJson(): String? {
        return sharedPrefs.getString(JSON_STRING_SHARED_PREFS_KEY, null)
    }

    private fun shouldRefresh(context: Context): Boolean {
        return abs(
            System.currentTimeMillis() - PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(Constants.SharedPrefs.LAST_MAINTENANCE_REFRESH, 0L)
        ) > TimeUnit.MINUTES.toMillis(5L)
    }

    private fun saveLastRefresh(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putLong(Constants.SharedPrefs.LAST_MAINTENANCE_REFRESH, System.currentTimeMillis())
        }
    }

    private const val SHARED_PREFS_NAME: String = "AppMaintenanceManagerPrefNames"
    private const val JSON_STRING_SHARED_PREFS_KEY: String = "json.string.shared.prefs.key"
}
