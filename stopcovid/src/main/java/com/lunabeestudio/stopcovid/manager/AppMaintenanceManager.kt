/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
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
import com.lunabeestudio.stopcovid.model.Info
import com.lunabeestudio.stopcovid.network.LBMaintenanceHttpClient
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

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
    var isActivityOpened: Boolean = false
    var infoFreeCompletion: (() -> Unit)? = null
    var infoBlockedCompletion: ((info: Info) -> Unit)? = null

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
    fun init(context: Context,
        @DrawableRes maintenanceIconRes: Int,
        @DrawableRes upgradeIconRes: Int,
        jsonUrl: String,
        infoFreeCompletion: (() -> Unit)? = null,
        infoBlockedCompletion: ((info: Info) -> Unit)? = null) {
        AppMaintenanceManager.jsonUrl = jsonUrl
        buildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
        }
        sharedPrefs = context.getSharedPreferences(
            SHARED_PREFS_NAME, 0)
        AppMaintenanceManager.maintenanceIconRes = maintenanceIconRes
        AppMaintenanceManager.upgradeIconRes = upgradeIconRes
        AppMaintenanceManager.infoFreeCompletion = infoFreeCompletion
        AppMaintenanceManager.infoBlockedCompletion = infoBlockedCompletion
    }

    /**
     * Call this to check if the app needs to be blocked or not
     */
    fun checkForMaintenanceUpgrade(context: Context) {
        when {
            isActivityOpened -> {
                return
            }
            shouldRefresh(context) -> {
                updateCheckForMaintenanceUpgrade(context,
                    null,
                    null)
            }
            else -> {
                useLastResult(context,
                    null,
                    null)
            }
        }
    }

    /**
     * This is called by the blocking activity to "retry" in case of maintenance
     */
    internal fun updateCheckForMaintenanceUpgrade(context: Context,
        appIsFreeCompletion: (() -> Unit)?,
        appIsBlockedCompletion: ((info: Info) -> Unit)?) {
        LBMaintenanceHttpClient.get(context,
            jsonUrl!!,
            { result ->
                try {
                    val info = Info(JSONObject(result))
                    saveMaintenanceJson(result)
                    showAppMaintenanceActivityIfNeeded(context,
                        info,
                        appIsFreeCompletion,
                        appIsBlockedCompletion)
                    saveLastRefresh(context)
                } catch (e: Exception) {
                    // In case of a malformed JSON we don't safe it and use the last one instead
                    useLastResult(context,
                        appIsFreeCompletion,
                        appIsBlockedCompletion)
                }
            },
            { e ->
                useLastResult(context,
                    appIsFreeCompletion,
                    appIsBlockedCompletion)
                Timber.e(e)
            })
    }

    private fun useLastResult(context: Context,
        appIsFreeCompletion: (() -> Unit)?,
        appIsBlockedCompletion: ((info: Info) -> Unit)?) {
        val lastResult = retrieveLastMaintenanceJson()
        lastResult?.let {
            showAppMaintenanceActivityIfNeeded(
                context,
                Info(JSONObject(lastResult)),
                appIsFreeCompletion,
                appIsBlockedCompletion)
        }
    }

    /**
     * Construct the maintenanceInfo object and block the app if needed in showing the LBAppMaintenanceActivity
     */
    private fun showAppMaintenanceActivityIfNeeded(context: Context,
        info: Info,
        appIsFreeCompletion: (() -> Unit)?,
        appIsBlockedCompletion: ((Info) -> Unit)?) {
        if (info.isActive == true && (info.minRequiredBuildNumber
                ?: 0) > buildNumber) {
            if (isActivityOpened) {
                appIsBlockedCompletion?.invoke(info)
            } else {
                startAppMaintenanceActivity(context, info)
            }
            infoBlockedCompletion?.invoke(info)
        } else {
            appIsFreeCompletion?.invoke()
            infoFreeCompletion?.invoke()
        }
    }

    /**
     * Launch the LBAppMaintenanceActivity with attributes to display
     */
    private fun startAppMaintenanceActivity(context: Context,
        info: Info) {
        context.startActivity(
            Intent(context, com.lunabeestudio.stopcovid.activity.AppMaintenanceActivity::class.java).apply {
                putExtra(com.lunabeestudio.stopcovid.activity.AppMaintenanceActivity.EXTRA_INFO, Gson().toJson(info))
            }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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

    /**
     * Get the last json saved if server is inaccessible
     */
    private fun retrieveLastMaintenanceJson(): String? {
        return sharedPrefs.getString(JSON_STRING_SHARED_PREFS_KEY, null)
    }

    private fun shouldRefresh(context: Context): Boolean {
        return System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5L) > PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(Constants.SharedPrefs.LAST_MAINTENANCE_REFRESH, 0L)
    }

    private fun saveLastRefresh(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putLong(Constants.SharedPrefs.LAST_MAINTENANCE_REFRESH, System.currentTimeMillis())
        }
    }

    private const val SHARED_PREFS_NAME: String = "AppMaintenanceManagerPrefNames"
    private const val JSON_STRING_SHARED_PREFS_KEY: String = "json.string.shared.prefs.key"
}