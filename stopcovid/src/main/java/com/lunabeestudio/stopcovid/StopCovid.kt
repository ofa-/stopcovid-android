/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
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
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.manager.ConfigManager
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.extension.lastVersionCode
import com.lunabeestudio.stopcovid.manager.AppMaintenanceManager
import com.lunabeestudio.stopcovid.manager.ConfigDataSource
import com.lunabeestudio.stopcovid.manager.FormManager
import com.lunabeestudio.stopcovid.manager.InfoCenterManager
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.PrivacyManager
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.model.DeviceSetup
import com.lunabeestudio.stopcovid.service.ProximityService
import com.lunabeestudio.stopcovid.worker.AtRiskNotificationWorker
import com.lunabeestudio.stopcovid.worker.MaintenanceWorker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.hours
import kotlin.time.milliseconds

class StopCovid : Application(), LifecycleObserver, RobertApplication {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable)
    }

    private val appCoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob() + coroutineExceptionHandler)

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    var isAppInForeground: Boolean = false

    private val cryptoManager: LocalCryptoManager by lazy {
        LocalCryptoManager(this)
    }

    val secureKeystoreDataSource: SecureKeystoreDataSource by lazy {
        SecureKeystoreDataSource(this, cryptoManager)
    }

    override val robertManager: RobertManager by lazy {
        RobertManagerImpl(
            this,
            SecureFileEphemeralBluetoothIdentifierDataSource(this, cryptoManager),
            secureKeystoreDataSource,
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

        if (sharedPrefs.lastVersionCode < BuildConfig.VERSION_CODE) {
            PrivacyManager.clearLocal(this)
            StringsManager.clearLocal(this)
            ConfigManager.clearLocal(this)
            FormManager.clearLocal(this)
        }

        appCoroutineScope.launch {
            PrivacyManager.initialize(this@StopCovid)
        }
        appCoroutineScope.launch {
            StringsManager.initialize(this@StopCovid)
        }
        appCoroutineScope.launch {
            InfoCenterManager.initialize(this@StopCovid)
        }
        appCoroutineScope.launch {
            KeyFiguresManager.initialize(this@StopCovid)
        }
        appCoroutineScope.launch {
            FormManager.initialize(this@StopCovid)
        }
        appCoroutineScope.launch {
            AppMaintenanceManager.initialize(
                this@StopCovid,
                R.drawable.maintenance,
                R.drawable.maintenance,
                BuildConfig.APP_MAINTENANCE_URL
            )
        }
        appCoroutineScope.launch {
            robertManager.refreshConfig(this@StopCovid)
        }

        val config = BundledEmojiCompatConfig(this)
        EmojiCompat.init(config)
        startAppMaintenanceWorker(false)

        File("${cacheDir.absolutePath}/shared_images").deleteRecursively()
        sharedPrefs.lastVersionCode = BuildConfig.VERSION_CODE
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onAppResume() {
        isAppInForeground = true

        appCoroutineScope.launch {
            AppMaintenanceManager.checkForMaintenanceUpgrade(this@StopCovid)
        }
        appCoroutineScope.launch {
            PrivacyManager.onAppForeground(this@StopCovid)
        }
        appCoroutineScope.launch {
            StringsManager.onAppForeground(this@StopCovid)
        }
        appCoroutineScope.launch {
            InfoCenterManager.onAppForeground(this@StopCovid)
        }
        appCoroutineScope.launch {
            KeyFiguresManager.onAppForeground(this@StopCovid)
        }
        appCoroutineScope.launch {
            FormManager.onAppForeground(this@StopCovid)
        }

        refreshProximityService()
        refreshStatusIfNeeded()
        deleteOldAttestations()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onAppPause() {
        isAppInForeground = false
    }

    override fun refreshProximityService() {
        if (robertManager.isProximityActive && ProximityManager.getDeviceSetup(this) == DeviceSetup.BLE) {
            ProximityService.start(this)
        } else {
            ProximityService.stop(this)
        }
    }

    override fun atRiskDetected() {
        val minHour = robertManager.atRiskMinHourContactNotif
        val maxHour = robertManager.atRiskMaxHourContactNotif

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

            Timber.v("Delay notification of ${delay / 1000}sec (trigger at ${Date(randomTime)})")

            OneTimeWorkRequestBuilder<AtRiskNotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
        }

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(Constants.WorkerNames.NOTIFICATION, ExistingWorkPolicy.KEEP, statusWorkRequest)
    }

    override suspend fun sendClockNotAlignedNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (StringsManager.strings.isEmpty()) {
            StringsManager.initialize(applicationContext)
        }
        val strings = StringsManager.strings

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
        val notification = NotificationCompat.Builder(
            this,
            UiConstants.Notification.TIME.channelId
        )
            .setContentTitle(strings["common.error.clockNotAligned.title"])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSmallIcon(com.lunabeestudio.stopcovid.coreui.R.drawable.ic_notification_bar)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(strings["common.error.clockNotAligned.message"])
            )
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(UiConstants.Notification.TIME.notificationId, notification)
    }

    override fun refreshInfoCenter() {
        appCoroutineScope.launch {
            InfoCenterManager.onAppForeground(this@StopCovid)
        }
    }

    fun cancelClockNotAlignedNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(UiConstants.Notification.TIME.notificationId)
    }

    suspend fun sendUpgradeNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (StringsManager.strings.isEmpty()) {
            StringsManager.initialize(applicationContext)
        }
        val strings = StringsManager.strings

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
        val notification = NotificationCompat.Builder(
            this,
            UiConstants.Notification.UPGRADE.channelId
        )
            .setContentTitle(strings["notification.upgrade.title"])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setAutoCancel(true)
            .setSmallIcon(com.lunabeestudio.stopcovid.coreui.R.drawable.ic_notification_bar)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(strings["notification.upgrade.message"])
            )
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

    @OptIn(ExperimentalTime::class)
    private fun deleteOldAttestations() {
        val expiredMilliSeconds = System.currentTimeMillis() - Duration.convert(robertManager.qrCodeDeletionHours.toDouble(),
            DurationUnit.HOURS,
            DurationUnit.MILLISECONDS)
        secureKeystoreDataSource.attestations = secureKeystoreDataSource.attestations?.filter { attestation ->
            (attestation["datetime"]?.value?.toLongOrNull() ?: 0L) > expiredMilliSeconds
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun refreshStatusIfNeeded() {
        val elapsedTimeSinceRefresh = (System.currentTimeMillis() - (robertManager.atRiskLastRefresh ?: 0L)).milliseconds
        val checkStatusFrequency = robertManager.checkStatusFrequencyHour.toDouble().hours
        if (robertManager.isRegistered && elapsedTimeSinceRefresh > checkStatusFrequency) {
            appCoroutineScope.launch {
                robertManager.updateStatus(this@StopCovid)
            }
        }
    }

    override fun getAppContext(): Context = this

    companion object {
        private const val LOCAL_PROXIMITY_DIR = "local_proximity"
    }
}
