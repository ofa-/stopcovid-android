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
import android.content.Context
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.framework.local.datasource.SecureFileEphemeralBluetoothIdentifierDataSource
import com.lunabeestudio.framework.local.datasource.SecureFileLocalProximityDataSource
import com.lunabeestudio.framework.local.datasource.SecureKeystoreDataSource
import com.lunabeestudio.framework.remote.datasource.ServiceDataSource
import com.lunabeestudio.framework.sharedcrypto.BouncyCastleCryptoDataSource
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.RobertManagerImpl
import com.lunabeestudio.stopcovid.BuildConfig.APP_MAINTENANCE_URL
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.manager.AppMaintenanceManager
import com.lunabeestudio.stopcovid.manager.ConfigDataSource
import com.lunabeestudio.stopcovid.manager.PrivacyManager
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.service.ProximityService
import com.lunabeestudio.stopcovid.worker.AtRiskNotificationWorker
import timber.log.Timber
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class StopCovid : Application(), LifecycleObserver, RobertApplication {

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
            ConfigDataSource
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

    override fun getAppContext(): Context = this

    companion object {
        private const val LOCAL_PROXIMITY_DIR = "local_proximity"
    }
}
