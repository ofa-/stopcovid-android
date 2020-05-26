/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavDeepLinkBuilder
import com.lunabeestudio.framework.local.datasource.SecureFileEphemeralBluetoothIdentifierDataSource
import com.lunabeestudio.framework.local.datasource.SecureFileLocalProximityDataSource
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.framework.remote.datasource.ServiceDataSource
import com.lunabeestudio.framework.utils.CryptoManager
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.RobertManagerImpl
import com.lunabeestudio.stopcovid.BuildConfig.APP_MAINTENANCE_URL
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.BuildConfig
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.manager.AppMaintenanceManager
import com.lunabeestudio.stopcovid.manager.PrivacyManager
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.service.ProximityService
import timber.log.Timber
import java.io.File

class StopCovid : Application(), LifecycleObserver, RobertApplication {

    private val cryptoManager: CryptoManager by lazy {
        CryptoManager(this)
    }

    override val robertManager: RobertManager by lazy {
        RobertManagerImpl(
            this,
            SecureFileEphemeralBluetoothIdentifierDataSource(this, cryptoManager),
            SecureKeystoreDataSource(this, cryptoManager),
            SecureFileLocalProximityDataSource(File(filesDir, LOCAL_PROXIMITY_DIR), cryptoManager),
            ServiceDataSource(this)
        )
    }

    private val strings: HashMap<String, String> by lazy {
        StringsManager.getStrings()
    }

    init {
        System.setProperty("kotlinx.coroutines.debug", if (BuildConfig.DEBUG) "on" else "off")
    }

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        PrivacyManager.init(this)
        StringsManager.init(this)
        AppMaintenanceManager.init(this,
            R.drawable.maintenance,
            R.drawable.maintenance,
            APP_MAINTENANCE_URL)
        val config = BundledEmojiCompatConfig(this)
        EmojiCompat.init(config)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onAppResume() {
        PrivacyManager.appForeground(this)
        StringsManager.appForeground(this)
        AppMaintenanceManager.checkForMaintenanceUpgrade(this)
        refreshProximityService()
    }

    override fun refreshProximityService() {
        if (robertManager.isProximityActive && ProximityManager.isPhoneSetup(this)) {
            ProximityService.start(this)
        } else {
            ProximityService.stop(this)
        }
    }

    override fun atRiskDetected() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.AT_RISK.channelId,
                strings["notification.channel.atRisk.title"] ?: "At risk",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = NavDeepLinkBuilder(this)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_main)
            .setDestination(R.id.informationFragment)
            .createPendingIntent()

        val notification = NotificationCompat.Builder(this,
            UiConstants.Notification.AT_RISK.channelId
        )
            .setContentTitle(strings["notification.atRisk.title"])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_notification_bar)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(strings["notification.atRisk.message"]))
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(UiConstants.Notification.AT_RISK.notificationId, notification)
    }

    override fun getAppContext(): Context = this

    companion object {
        private const val LOCAL_PROXIMITY_DIR = "local_proximity"
    }
}
