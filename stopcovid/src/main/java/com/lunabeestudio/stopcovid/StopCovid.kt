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
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.analytics.model.HealthEventName
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeMs
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.EnvConstant
import com.lunabeestudio.stopcovid.coreui.LocalizedApplication
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.coreui.utils.ImmutablePendingIntentCompat
import com.lunabeestudio.stopcovid.extension.alertRiskLevelChanged
import com.lunabeestudio.stopcovid.extension.googleReviewShown
import com.lunabeestudio.stopcovid.extension.hasChosenPostalCode
import com.lunabeestudio.stopcovid.extension.hideRiskStatus
import com.lunabeestudio.stopcovid.extension.isObsolete
import com.lunabeestudio.stopcovid.extension.lastVersionCode
import com.lunabeestudio.stopcovid.extension.lastVersionName
import com.lunabeestudio.stopcovid.extension.lowStorageAlertShown
import com.lunabeestudio.stopcovid.extension.ratingPopInShown
import com.lunabeestudio.stopcovid.extension.ratingsKeyFiguresOpening
import com.lunabeestudio.stopcovid.manager.AppMaintenanceManager
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.model.DeviceSetup
import com.lunabeestudio.stopcovid.service.ProximityService
import com.lunabeestudio.stopcovid.widgetshomescreen.ProximityWidget
import com.lunabeestudio.stopcovid.worker.ActivateReminderNotificationWorker
import com.lunabeestudio.stopcovid.worker.AtRiskNotificationWorker
import com.lunabeestudio.stopcovid.worker.Blacklist2DDOCWorker
import com.lunabeestudio.stopcovid.worker.BlacklistDCCWorker
import com.lunabeestudio.stopcovid.worker.DccLightRenewCleanWorker
import com.lunabeestudio.stopcovid.worker.IsolationReminderNotificationWorker
import com.lunabeestudio.stopcovid.worker.MaintenanceWorker
import fr.bipi.tressence.file.FileLoggerTree
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Cache
import timber.log.Timber
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class StopCovid : Application(), RobertApplication, LocalizedApplication {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable)
    }
    private val appCoroutineScope by lazy { CoroutineScope(Dispatchers.Main + SupervisorJob() + coroutineExceptionHandler) }

    lateinit var injectionContainer: InjectionContainer

    override val localizedStrings: LocalizedStrings
        get() = injectionContainer.stringsManager.strings
    override val liveLocalizedStrings: LiveData<Event<LocalizedStrings>>
        get() = injectionContainer.stringsManager.liveStrings

    override suspend fun initializeStrings(): Unit = injectionContainer.stringsManager.initialize(this)

    override var isAppInForeground: Boolean = false

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override val robertManager: RobertManager
        get() = injectionContainer.robertManager

    private var firstResume = false

    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == UiConstants.SharedPrefs.USER_LANGUAGE) {
            appCoroutineScope.launch {
                injectionContainer.stringsManager.onAppForeground(this@StopCovid)
                injectionContainer.privacyManager.onAppForeground(this@StopCovid)
                injectionContainer.linksManager.onAppForeground(this@StopCovid)
                injectionContainer.moreKeyFiguresManager.onAppForeground(this@StopCovid)
            }
        }
    }

    private val appObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            isAppInForeground = true
            refreshData()
            injectionContainer.analyticsManager.reportAppEvent(AppEventName.e3)
            refreshProximityService()
            try {
                refreshStatusIfNeeded()
            } catch (e: Exception) {
                Timber.e(e)
            }
            deleteOldAttestations()
            appCoroutineScope.launch {
                injectionContainer.venueRepository.clearExpired(robertManager)
            }
            appCoroutineScope.launch {
                robertManager.cleaReportIfNeeded(this@StopCovid, false)
            }

            if (robertManager.configuration.displayActivityPass) {
                DccLightRenewCleanWorker.startDccLightCleanAndRenewWorker(this@StopCovid)
            }
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            isAppInForeground = false
        }
    }

    init {
        System.setProperty("kotlinx.coroutines.debug", if (BuildConfig.DEBUG) "on" else "off")
    }

    override fun onCreate() {
        super.onCreate()

        initializeInjectionContainer()

        ProcessLifecycleOwner.get().lifecycle.addObserver(appObserver)
        firstResume = true

        setupTimber()

        // Make sure data is cleaned before initializeData
        if (sharedPrefs.lastVersionCode < BuildConfig.VERSION_CODE) {
            clearData()
        }
        if (sharedPrefs.lastVersionName != BuildConfig.VERSION_NAME) {
            sharedPrefs.ratingsKeyFiguresOpening = 0
            sharedPrefs.googleReviewShown = false
            sharedPrefs.lowStorageAlertShown = false
        }
        if (!isSameMajorRelease()) {
            sharedPrefs.ratingPopInShown = false
        }

        initializeData()

        val config = BundledEmojiCompatConfig(this)
        EmojiCompat.init(config)

        setupAppMaintenance()

        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        sharedPrefs.lastVersionCode = BuildConfig.VERSION_CODE
        sharedPrefs.lastVersionName = BuildConfig.VERSION_NAME
    }

    fun initializeInjectionContainer() {
        injectionContainer = InjectionContainer(this, appCoroutineScope)
    }

    private fun setupTimber() {
        Timber.plant(WarnTree())
        injectionContainer.logsDir.mkdir()
        Timber.plant(
            FileLoggerTree.Builder()
                .withFileName("logs_%g.log")
                .withDir(injectionContainer.logsDir)
                .withSizeLimit(1024 * 1024 * 2)
                .withFileLimit(2)
                .withMinPriority(Log.VERBOSE)
                .appendToFile(true)
                .build()
        )
        Timber.plant(
            FileLoggerTree.Builder()
                .withFileName("error_logs_%g.log")
                .withDir(injectionContainer.logsDir)
                .withSizeLimit(1024 * 1024 * 2)
                .withFileLimit(2)
                .withMinPriority(Log.WARN)
                .appendToFile(true)
                .build()
        )
    }

    private suspend fun migrateAttestationsIfNeeded() {
        injectionContainer.attestationRepository.migrateAttestationsIfNeeded(
            robertManager,
            injectionContainer.secureKeystoreDataSource,
            injectionContainer.stringsManager.strings
        )
    }

    private fun clearData() {
        injectionContainer.moreKeyFiguresManager.clearLocal(this)
        injectionContainer.linksManager.clearLocal(this)
        injectionContainer.privacyManager.clearLocal(this)
        injectionContainer.stringsManager.clearLocal(this)
        injectionContainer.configManager.clearLocal(this)
        injectionContainer.calibrationManager.clearLocal(this)
        injectionContainer.formManager.clearLocal(this)
        injectionContainer.secureKeystoreDataSource.configuration = injectionContainer.secureKeystoreDataSource.configuration?.apply {
            version = 0
        }
        injectionContainer.secureKeystoreDataSource.calibration = injectionContainer.secureKeystoreDataSource.calibration?.apply {
            version = 0
        }
        val okHttpCacheDir = File(cacheDir, ServerManager.OKHTTP_CACHE_FILENAME)
        if (okHttpCacheDir.exists()) {
            try {
                @Suppress("BlockingMethodInNonBlockingContext")
                Cache(okHttpCacheDir, ServerManager.OKHTTP_MAX_CACHE_SIZE_BYTES).delete()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private fun initializeData() {
        appCoroutineScope.launch {
            injectionContainer.moreKeyFiguresManager.initialize(this@StopCovid)
        }
        appCoroutineScope.launch {
            injectionContainer.linksManager.initialize(this@StopCovid)
        }
        appCoroutineScope.launch {
            injectionContainer.privacyManager.initialize(this@StopCovid)
        }
        appCoroutineScope.launch {
            injectionContainer.stringsManager.initialize(this@StopCovid)
            migrateAttestationsIfNeeded()
        }
        appCoroutineScope.launch {
            injectionContainer.infoCenterManager.initialize(this@StopCovid)
        }
        appCoroutineScope.launch {
            injectionContainer.formManager.initialize(this@StopCovid)
        }
        appCoroutineScope.launch {
            injectionContainer.vaccinationCenterManager.initialize(this@StopCovid, sharedPrefs)
        }
        appCoroutineScope.launch {
            injectionContainer.keyFiguresManager.initialize(this@StopCovid)
        }
        appCoroutineScope.launch {
            injectionContainer.risksLevelManager.initialize(this@StopCovid)
        }

        runBlocking {
            injectionContainer.dccCertificatesManager.initialize(this@StopCovid)
        }
    }

    private fun setupAppMaintenance() {
        AppMaintenanceManager.initialize(
            this@StopCovid,
            R.drawable.maintenance,
            R.drawable.maintenance,
            ConfigConstant.Maintenance.URL
        )
        startAppMaintenanceWorker(false)
    }

    @OptIn(ExperimentalTime::class)
    private fun refreshData() {

        // Launch blacklist in parallel independently as it can take a long time (paging + room insertion)
        BlacklistDCCWorker.start(this)
        Blacklist2DDOCWorker.start(this)

        appCoroutineScope.launch(Dispatchers.IO) {
            if (firstResume) {
                delay(1.seconds) // Add some delay to let the main activity start
            }
            firstResume = false

            AppMaintenanceManager.checkForMaintenanceUpgrade(this@StopCovid, injectionContainer.serverManager.okHttpClient)
            injectionContainer.moreKeyFiguresManager.onAppForeground(this@StopCovid)
            injectionContainer.linksManager.onAppForeground(this@StopCovid)
            injectionContainer.privacyManager.onAppForeground(this@StopCovid)
            injectionContainer.stringsManager.onAppForeground(this@StopCovid)
            injectionContainer.infoCenterManager.refreshIfNeeded(this@StopCovid)
            injectionContainer.keyFiguresManager.onAppForeground(this@StopCovid)
            injectionContainer.risksLevelManager.onAppForeground(this@StopCovid)
            injectionContainer.formManager.onAppForeground(this@StopCovid)
            injectionContainer.vaccinationCenterManager.onAppForeground(this@StopCovid, sharedPrefs)
            injectionContainer.certificatesDocumentsManager.onAppForeground(this@StopCovid)
            injectionContainer.dccCertificatesManager.onAppForeground(this@StopCovid)

            try {
                robertManager.refreshConfig(this@StopCovid)
            } catch (e: Exception) {
                Timber.e(e)
            }

            injectionContainer.serverManager.okHttpClient.connectionPool.evictAll()
        }
    }

    override fun refreshProximityService() {
        val isProximityActive = try {
            robertManager.isProximityActive
        } catch (e: Exception) {
            // On some device Keychain might not be ready and crash the app
            Timber.e(e)
            false
        }
        if (isProximityActive && ProximityManager.getDeviceSetup(this, robertManager) == DeviceSetup.BLE) {
            ProximityService.start(this)
        } else {
            ProximityService.stop(this)
        }
    }

    override fun notifyAtRiskLevelChange(prevRiskLevel: Float) {
        injectionContainer.risksLevelManager.getCurrentLevel(robertManager.atRiskStatus?.riskLevel)?.let { riskLevel ->
            injectionContainer.analyticsManager.reportHealthEvent(
                HealthEventName.eh2,
                "$prevRiskLevel|riskLevel.riskLevel"
            )
            val inputData = Data.Builder()
                .putString(AtRiskNotificationWorker.INPUT_DATA_TITLE_KEY, riskLevel.labels.notifTitle)
                .putString(AtRiskNotificationWorker.INPUT_DATA_MESSAGE_KEY, riskLevel.labels.notifBody)
                .build()
            sendAtRiskNotification(
                OneTimeWorkRequestBuilder<AtRiskNotificationWorker>().setInputData(inputData)
            )
        }
    }

    override fun atRiskLevelChange(prevRiskLevel: Float) {
        injectionContainer.risksLevelManager.getCurrentLevel(robertManager.atRiskStatus?.riskLevel)?.let { riskLevel ->
            injectionContainer.analyticsManager.reportHealthEvent(HealthEventName.eh3, "$prevRiskLevel|${riskLevel.riskLevel}")
        }
        sharedPrefs.alertRiskLevelChanged = true
        sharedPrefs.hideRiskStatus = false
        ProximityWidget.updateWidget(applicationContext)
    }

    private fun sendAtRiskNotification(oneTimeWorkRequestBuilder: OneTimeWorkRequest.Builder) {
        val minHour = robertManager.configuration.minHourContactNotif
        val maxHour = robertManager.configuration.maxHourContactNotif

        val currentCal = Calendar.getInstance()
        val hours = currentCal.get(Calendar.HOUR_OF_DAY)

        val targetCalendar = Calendar.getInstance()
        targetCalendar.set(Calendar.MINUTE, 0)

        val statusWorkRequest = if (hours in minHour..maxHour) {
            oneTimeWorkRequestBuilder.build()
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

            oneTimeWorkRequestBuilder
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
        }

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(Constants.WorkerNames.AT_RISK_NOTIFICATION, ExistingWorkPolicy.REPLACE, statusWorkRequest)
    }

    override suspend fun sendClockNotAlignedNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (injectionContainer.stringsManager.strings.isEmpty()) {
            injectionContainer.stringsManager.initialize(applicationContext)
        }
        val strings = injectionContainer.stringsManager.strings

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.TIME.channelId,
                strings["notification.channel.error.title"] ?: "Erreur",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = ImmutablePendingIntentCompat.getActivity(
            this, 0,
            notificationIntent
        )
        val notification = NotificationCompat.Builder(
            this,
            UiConstants.Notification.TIME.channelId
        )
            .setContentTitle(strings["common.error.clockNotAligned.title"])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
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
            injectionContainer.infoCenterManager.refreshIfNeeded(this@StopCovid)
        }
    }

    fun cancelClockNotAlignedNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(UiConstants.Notification.TIME.notificationId)
    }

    fun setActivateReminder(inHour: Int) {
        val reminderWorker = OneTimeWorkRequestBuilder<ActivateReminderNotificationWorker>()
            .setInitialDelay(inHour.toLong(), TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(Constants.WorkerNames.ACTIVATE_REMINDER, ExistingWorkPolicy.KEEP, reminderWorker)
    }

    fun cancelActivateReminder() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(UiConstants.Notification.ACTIVATE_REMINDER.notificationId)
        WorkManager.getInstance(applicationContext)
            .cancelUniqueWork(Constants.WorkerNames.ACTIVATE_REMINDER)
    }

    fun setIsolationReminder(date: Date) {
        val minHour = robertManager.configuration.minHourContactNotif
        val maxHour = robertManager.configuration.maxHourContactNotif

        val targetCalendar = Calendar.getInstance()
        targetCalendar.time = date
        targetCalendar.set(Calendar.MINUTE, 0)
        targetCalendar.set(Calendar.HOUR_OF_DAY, minHour)
        val minTime = targetCalendar.time.time
        targetCalendar.set(Calendar.HOUR_OF_DAY, maxHour)
        val maxTime = targetCalendar.time.time

        val currentTime = System.currentTimeMillis()
        val randomTime = Random.nextLong(minTime, maxTime)
        val delay = (randomTime - currentTime).coerceAtLeast(0)

        Timber.v("Delay notification of ${delay / 1000}sec (trigger at ${Date(randomTime)})")

        val reminderWorker = OneTimeWorkRequestBuilder<IsolationReminderNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(Constants.WorkerNames.ISOLATION_REMINDER, ExistingWorkPolicy.KEEP, reminderWorker)
    }

    fun cancelIsolationReminder() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(UiConstants.Notification.ISOLATION_REMINDER.notificationId)
        WorkManager.getInstance(applicationContext)
            .cancelUniqueWork(Constants.WorkerNames.ISOLATION_REMINDER)
    }

    suspend fun sendUpgradeNotification() {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (injectionContainer.stringsManager.strings.isEmpty()) {
            injectionContainer.stringsManager.initialize(applicationContext)
        }
        val strings = injectionContainer.stringsManager.strings

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.UPGRADE.channelId,
                strings["notification.channel.upgrade.title"] ?: "Upgrade",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = ImmutablePendingIntentCompat.getActivity(
            this, 0,
            notificationIntent
        )
        val notification = NotificationCompat.Builder(
            this,
            UiConstants.Notification.UPGRADE.channelId
        )
            .setContentTitle(strings["notification.upgrade.title"])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
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

    private fun deleteOldAttestations() {
        appCoroutineScope.launch {
            injectionContainer.secureKeystoreDataSource.attestations().data?.filter { attestation ->
                attestation.isObsolete(robertManager.configuration)
            }?.forEach { attestation ->
                injectionContainer.secureKeystoreDataSource.deleteAttestation(attestation.id)
            }
        }
    }

    private fun refreshStatusIfNeeded() {
        val elapsedTimeSinceRefresh = (System.currentTimeMillis() - (robertManager.atRiskLastRefresh ?: 0L)).milliseconds
        val checkStatusFrequency = robertManager.configuration.checkStatusFrequencyHour.toDouble().hours
        if (robertManager.isRegistered && elapsedTimeSinceRefresh > checkStatusFrequency) {
            appCoroutineScope.launch {
                robertManager.updateStatus(this@StopCovid)
            }
        }
    }

    override fun getAppContext(): Context = this

    private fun isSameMajorRelease(): Boolean {
        val lastImportantRelease = sharedPrefs.lastVersionName
            ?.split(".")
            ?.slice(0 until Constants.Build.NB_DIGIT_MAJOR_RELEASE)
            ?.joinToString(".")
        val currentImportantRelease = BuildConfig.VERSION_NAME
            .split(".")
            .slice(0 until Constants.Build.NB_DIGIT_MAJOR_RELEASE)
            .joinToString(".")
        return lastImportantRelease == currentImportantRelease
    }

    override fun getBaseUrl(): String {
        return EnvConstant.Prod.analyticsBaseUrl
    }

    override fun getApiVersion(): String {
        return robertManager.configuration.analyticsApiVersion
    }

    override fun getAppVersion(): String = BuildConfig.VERSION_NAME

    override fun getAppBuild(): Int = BuildConfig.VERSION_CODE

    override suspend fun getPlacesCount(): Int = injectionContainer.secureKeystoreDataSource.venuesQrCode().data?.size ?: 0

    override suspend fun getFormsCount(): Int = injectionContainer.secureKeystoreDataSource.attestations().data?.size ?: 0

    override suspend fun getCertificatesCount(): Int = injectionContainer.secureKeystoreDataSource.certificateCountFlow.firstOrNull() ?: 0

    override fun userHaveAZipCode(): Boolean = sharedPrefs.hasChosenPostalCode

    override fun getDateSample(): Long? = robertManager.reportPositiveTestDate

    override fun getDateFirstSymptom(): Long? = robertManager.reportSymptomsStartDate

    override fun getDateLastContactNotification(): Long? = robertManager.atRiskStatus?.ntpLastContactS?.ntpTimeSToUnixTimeMs()
}
