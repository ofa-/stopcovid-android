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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.framework.local.datasource.SecureFileEphemeralBluetoothIdentifierDataSource
import com.lunabeestudio.framework.local.datasource.SecureFileLocalProximityDataSource
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.framework.manager.LocalProximityFilterImpl
import com.lunabeestudio.framework.remote.datasource.ServiceDataSource
import com.lunabeestudio.framework.sharedcrypto.BouncyCastleCryptoDataSource
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.RobertManagerImpl
import com.lunabeestudio.stopcovid.BuildConfig.APP_MAINTENANCE_URL
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.manager.AppMaintenanceManager
import com.lunabeestudio.stopcovid.manager.ConfigDataSource
import com.lunabeestudio.stopcovid.manager.PrivacyManager
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.service.ProximityService
import com.lunabeestudio.stopcovid.worker.AtRiskNotificationWorker
import com.lunabeestudio.stopcovid.worker.MaintenanceWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class StopCovid : Application(), LifecycleObserver, RobertApplication {

    var isAppInForeground: Boolean = false

    private val cryptoManager: LocalCryptoManager by lazy {
        LocalCryptoManager(this)
    }

    override val robertManager: RobertManager by lazy {
        RobertManagerImpl(
            this,
            SecureFileEphemeralBluetoothIdentifierDataSource(this, cryptoManager),
            SecureKeystoreDataSource(this, cryptoManager),
            SecureFileLocalProximityDataSource(File(filesDir, LOCAL_PROXIMITY_DIR), cryptoManager),
            ServiceDataSource(this),
            BouncyCastleCryptoDataSource(),
            ConfigDataSource,
            LocalProximityFilterImpl()
        )
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
        startAppMaintenanceWorker(false)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onAppResume() {
        isAppInForeground = true
        PrivacyManager.appForeground(this)
        StringsManager.appForeground(this)
        AppMaintenanceManager.checkForMaintenanceUpgrade(this)
        refreshProximityService()
        refreshStatusIfNeeded()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onAppPause() {
        isAppInForeground = false
    }

    override fun refreshProximityService() {
        if (robertManager.isProximityActive && ProximityManager.isPhoneSetup(this)) {
            ProximityService.start(this)
        } else {
            ProximityService.stop(this)
        }
    }

    override fun atRiskDetected() {
        val minHour = robertManager().atRiskMinHourContactNotif
        val maxHour = robertManager().atRiskMaxHourContactNotif

        val currentCal = Calendar.getInstance()
        val hours = currentCal.get(Calendar.HOUR_OF_DAY)

        val targetCalendar = Calendar.getInstance()
        targetCalendar.set(Calendar.MINUTE, 0)

        val statusWorkRequest = if (hours in minHour..maxHour) {
            OneTimeWorkRequestBuilder<AtRiskNotificationWorker>().build()
        } else {
            if (hours > maxHour) {
                targetCalendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            targetCalendar.set(Calendar.HOUR_OF_DAY, minHour)
            val minTime = targetCalendar.time.time
            targetCalendar.set(Calendar.HOUR_OF_DAY, maxHour)
            val maxTime = targetCalendar.time.time

            val currentTime = currentCal.time.time
            val randomTime = Random.nextLong(maxOf(currentTime, minTime), maxOf(currentTime, maxTime))
            val delay = (randomTime - currentTime).coerceAtLeast(0)

            Timber.d("Delay notification of ${delay / 1000}sec (trigger at ${Date(randomTime)})")

            OneTimeWorkRequestBuilder<AtRiskNotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
        }

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(Constants.WorkerNames.NOTIFICATION, ExistingWorkPolicy.KEEP, statusWorkRequest)
    }

    override fun sendClockNotAlignedNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (StringsManager.getStrings().isEmpty()) {
            StringsManager.init(applicationContext)
        }
        val strings = StringsManager.getStrings()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.TIME.channelId,
                strings["notification.channel.error.title"] ?: "Erreur",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this,
            UiConstants.Notification.TIME.channelId
        )
            .setContentTitle(strings["common.error.clockNotAligned.title"])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSmallIcon(com.lunabeestudio.stopcovid.coreui.R.drawable.ic_notification_bar)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(strings["common.error.clockNotAligned.message"]))
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(UiConstants.Notification.TIME.notificationId, notification)
    }

    fun cancelClockNotAlignedNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(UiConstants.Notification.TIME.notificationId)
    }

    fun sendUpgradeNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (StringsManager.getStrings().isEmpty()) {
            StringsManager.init(applicationContext)
        }
        val strings = StringsManager.getStrings()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.UPGRADE.channelId,
                strings["notification.channel.upgrade.title"] ?: "Upgrade",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this,
            UiConstants.Notification.UPGRADE.channelId
        )
            .setContentTitle(strings["notification.upgrade.title"])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setAutoCancel(true)
            .setSmallIcon(com.lunabeestudio.stopcovid.coreui.R.drawable.ic_notification_bar)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(strings["notification.upgrade.message"]))
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(UiConstants.Notification.UPGRADE.notificationId, notification)
    }

    fun startAppMaintenanceWorker(now: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val timeChangedWorkRequest = PeriodicWorkRequestBuilder<MaintenanceWorker>(6L, TimeUnit.HOURS)
            .setConstraints(constraints)
            .apply {
                if (!now) {
                    setInitialDelay(6L, TimeUnit.HOURS)
                }
            }
            .build()
        val policy = if (now) {
            ExistingPeriodicWorkPolicy.REPLACE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(Constants.WorkerNames.TIME_CHANGED, policy, timeChangedWorkRequest)
    }

    fun refreshStatusIfNeeded() {
        if (System.currentTimeMillis() - (robertManager.atRiskLastRefresh
                ?: 0L) > TimeUnit.HOURS.toMillis(robertManager.checkStatusFrequencyHour.toLong())) {
            CoroutineScope(Dispatchers.IO).launch {
                robertManager.updateStatus(this@StopCovid)
            }
        }
    }

    override fun getAppContext(): Context = this

    companion object {
        private const val LOCAL_PROXIMITY_DIR = "local_proximity"
    }
}
