/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.analytics.manager

import android.content.Context
import android.os.Build
import androidx.core.util.AtomicFile
import androidx.lifecycle.LifecycleObserver
import com.lunabeestudio.analytics.extension.deleteAnalyticsAfterNextStatus
import com.lunabeestudio.analytics.extension.installationUUID
import com.lunabeestudio.analytics.extension.isOptIn
import com.lunabeestudio.analytics.extension.sendAnalytics
import com.lunabeestudio.analytics.extension.proximityActiveDuration
import com.lunabeestudio.analytics.extension.proximityStartTime
import com.lunabeestudio.analytics.extension.statusSuccessCount
import com.lunabeestudio.analytics.extension.toAPI
import com.lunabeestudio.analytics.extension.toDomain
import com.lunabeestudio.analytics.extension.toProto
import com.lunabeestudio.analytics.model.AnalyticsResult
import com.lunabeestudio.analytics.model.AnalyticsServiceName
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.analytics.model.AppInfos
import com.lunabeestudio.analytics.model.ErrorEventName
import com.lunabeestudio.analytics.model.HealthEventName
import com.lunabeestudio.analytics.model.HealthInfos
import com.lunabeestudio.analytics.model.TimestampedEvent
import com.lunabeestudio.analytics.network.AnalyticsServerManager
import com.lunabeestudio.analytics.network.model.SendAppAnalyticsRQ
import com.lunabeestudio.analytics.network.model.SendHealthAnalyticsRQ
import com.lunabeestudio.analytics.proto.ProtoStorage
import com.lunabeestudio.analytics.proxy.AnalyticsInfosProvider
import com.lunabeestudio.analytics.proxy.AnalyticsRobertManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class AnalyticsManager(okHttpClient: OkHttpClient, context: Context) : LifecycleObserver {

    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
    private val analyticsServerManager = AnalyticsServerManager(okHttpClient)
    private val filesDir: File = context.filesDir

    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)

    private val reportAppEventMtx = Mutex()
    private val reportHealthEventMtx = Mutex()
    private val reportErrorEventMtx = Mutex()

    init {
        sharedPreferences.apply {
            if (installationUUID == null) {
                installationUUID = UUID.randomUUID().toString()
            }
        }
    }

    fun isOptIn(): Boolean = sharedPreferences.isOptIn

    fun setIsOptIn(isOptIn: Boolean) {
        sharedPreferences.isOptIn = isOptIn
    }

    fun requestDeleteAnalytics() {
        reportAppEvent(AppEventName.e17)
        sharedPreferences.deleteAnalyticsAfterNextStatus = true
    }

    fun register() {
        sharedPreferences.installationUUID = UUID.randomUUID().toString()
    }

    fun unregister() {
        sharedPreferences.apply {
            installationUUID = null
            proximityStartTime = null
            proximityActiveDuration = 0L
            statusSuccessCount = 0
        }
        reset()
    }

    fun proximityDidStart() {
        sharedPreferences.proximityStartTime = System.currentTimeMillis()
    }

    fun proximityDidStop() {
        sharedPreferences.apply {
            proximityActiveDuration = getProximityActiveDuration()
            proximityStartTime = null
        }
    }

    fun statusDidSucceed() {
        reportAppEvent(AppEventName.e16)
        sharedPreferences.apply {
            statusSuccessCount += 1
        }
    }

    private fun getProximityActiveDuration(): Long {
        val oldDuration = sharedPreferences.proximityActiveDuration
        val addedDuration = System.currentTimeMillis() - (sharedPreferences.proximityStartTime ?: System.currentTimeMillis())
        return oldDuration + addedDuration
    }

    suspend fun sendAnalytics(
        robertManager: AnalyticsRobertManager,
        analyticsInfosProvider: AnalyticsInfosProvider,
        token: String
    ) {
        robertManager.configuration.isAnalyticsOn = sharedPreferences.sendAnalytics
        if (robertManager.configuration.isAnalyticsOn && sharedPreferences.isOptIn) {
            val receivedHelloMessagesCount = robertManager.getLocalProximityCount()
            sendAppAnalytics(analyticsInfosProvider, token, receivedHelloMessagesCount)
            delay(Random.nextLong(ANALYTICS_REPORT_MIN_DELAY, ANALYTICS_REPORT_MAX_DELAY))
            sendHealthAnalytics(robertManager, analyticsInfosProvider, token)
        } else {
            reset()
        }
        if (sharedPreferences.deleteAnalyticsAfterNextStatus) {
            sendDeleteAnalytics(analyticsInfosProvider, token)
        }
    }

    private suspend fun sendAppAnalytics(
        analyticsInfosProvider: AnalyticsInfosProvider,
        token: String,
        receivedHelloMessagesCount: Int
    ) {
        val appInfos = getAppInfos(analyticsInfosProvider, receivedHelloMessagesCount)
        val appEvents = getAppEvents()
        val appErrors = getErrors()
        val sendAnalyticsRQ = SendAppAnalyticsRQ(
            installationUuid = sharedPreferences.installationUUID ?: UUID.randomUUID().toString(),
            infos = appInfos,
            events = appEvents.toAPI(),
            errors = appErrors.toAPI()
        )
        withContext(Dispatchers.IO) {
            val result = analyticsServerManager.sendAnalytics(
                analyticsInfosProvider.getBaseUrl(),
                analyticsInfosProvider.getApiVersion(),
                token,
                sendAnalyticsRQ
            )
            when (result) {
                is AnalyticsResult.Success -> {
                    resetAppEvents()
                    resetErrors()
                }
                is AnalyticsResult.Failure -> {
                    Timber.e(result.error)
                    if ((result.error as? HttpException)?.code() == 413) {
                        resetAppEvents()
                        resetErrors()
                    }
                }
            }
        }
    }

    private suspend fun sendHealthAnalytics(
        robertManager: AnalyticsRobertManager,
        analyticsInfosProvider: AnalyticsInfosProvider,
        token: String
    ) {
        val healthEvents = getHealthEvents()
        val healthInfos = getHealthInfos(robertManager, analyticsInfosProvider)
        val sendAnalyticsRQ = SendHealthAnalyticsRQ(
            installationUuid = UUID.randomUUID().toString(),
            infos = healthInfos,
            events = healthEvents.toAPI(),
            errors = emptyList()
        )
        withContext(Dispatchers.IO) {
            val result = analyticsServerManager.sendAnalytics(
                analyticsInfosProvider.getBaseUrl(),
                analyticsInfosProvider.getApiVersion(),
                token,
                sendAnalyticsRQ,
            )
            when (result) {
                is AnalyticsResult.Success -> {
                    withContext(Dispatchers.Main) {
                        resetHealthEvents()
                    }
                }
                is AnalyticsResult.Failure -> {
                    Timber.e(result.error)
                    if ((result.error as? HttpException)?.code() == 413) {
                        resetHealthEvents()
                    }
                }
            }
        }
    }

    private suspend fun sendDeleteAnalytics(
        analyticsInfosProvider: AnalyticsInfosProvider,
        token: String
    ) {
        withContext(Dispatchers.IO) {
            sharedPreferences.installationUUID?.let { installationUUID ->
                val apiVersion = analyticsInfosProvider.getApiVersion()
                val result = analyticsServerManager.deleteAnalytics(
                    analyticsInfosProvider.getBaseUrl(),
                    apiVersion,
                    token,
                    installationUUID,
                )
                when (result) {
                    is AnalyticsResult.Success -> {
                        withContext(Dispatchers.Main) {
                            sharedPreferences.deleteAnalyticsAfterNextStatus = false
                        }
                    }
                    is AnalyticsResult.Failure -> {
                        val error = result.error
                        Timber.e(error)
                        if (error is HttpException) {
                            reportWSError(
                                AnalyticsServiceName.ANALYTICS,
                                apiVersion,
                                error.code(),
                                error.message()
                            )
                        } else if (error?.isNoInternetException() == false) {
                            reportWSError(
                                AnalyticsServiceName.ANALYTICS,
                                apiVersion,
                                (error as? HttpException)?.code() ?: 0,
                                error.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun Exception.isNoInternetException(): Boolean = this is SocketTimeoutException
        || this is IOException
        || this is UnknownHostException

    fun reset() {
        resetAppEvents()
        resetHealthEvents()
        resetErrors()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Synchronized
    fun reportAppEvent(eventName: AppEventName, desc: String? = null) {
        if (sharedPreferences.isOptIn) {
            CoroutineScope(Dispatchers.IO).launch {
                reportSynchronizedAppEvent(eventName, desc)
            }
        }
    }

    private suspend fun reportSynchronizedAppEvent(eventName: AppEventName, desc: String?) {
        reportAppEventMtx.withLock {
            val timestampedEventList = getAppEvents().toMutableList()
            val timestamp = dateFormat.format(currentRoundedHourInstant())
            timestampedEventList += TimestampedEvent(eventName.name, timestamp, desc ?: "")
            val file = File(File(filesDir, FOLDER_NAME), FILE_NAME_APP_EVENTS)
            writeTimestampedEventProtoToFile(file, timestampedEventList.toProto())
        }
    }

    fun reportHealthEvent(eventName: HealthEventName, desc: String? = null) {
        if (sharedPreferences.isOptIn) {
            CoroutineScope(Dispatchers.IO).launch {
                reportSynchronizedHealthEvent(eventName, desc)
            }
        }
    }

    private suspend fun reportSynchronizedHealthEvent(eventName: HealthEventName, desc: String? = null) {
        reportHealthEventMtx.withLock {
            val timestampedEventList = getHealthEvents().toMutableList()
            timestampedEventList += TimestampedEvent(eventName.name, dateFormat.format(currentRoundedHourInstant()), desc ?: "")
            val file = File(File(filesDir, FOLDER_NAME), FILE_NAME_HEALTH_EVENTS)
            writeTimestampedEventProtoToFile(file, timestampedEventList.toProto())
        }
    }

    fun reportWSError(wsName: String, wsVersion: String, errorCode: Int, desc: String? = null) {
        if (sharedPreferences.isOptIn) {
            if (desc?.contains("No address associated with hostname") != true) {
                val name = "ERR-${wsName.uppercase(Locale.getDefault())}-${wsVersion.uppercase(Locale.getDefault())}-$errorCode"
                CoroutineScope(Dispatchers.IO).launch {
                    reportSynchronizedErrorEvent(name)
                }
            }
        }
    }

    fun reportErrorEvent(errorEventName: ErrorEventName) {
        if (sharedPreferences.isOptIn) {
            CoroutineScope(Dispatchers.IO).launch {
                reportSynchronizedErrorEvent(errorEventName.name)
            }
        }
    }

    private suspend fun reportSynchronizedErrorEvent(errorName: String) {
        reportErrorEventMtx.withLock {
            val timestampedEventList = getErrors().toMutableList()
            timestampedEventList += TimestampedEvent(errorName, dateFormat.format(currentRoundedHourInstant()), "")
            val file = File(File(filesDir, FOLDER_NAME), FILE_NAME_APP_ERRORS)
            writeTimestampedEventProtoToFile(file, timestampedEventList.toProto())
        }
    }

    private fun writeTimestampedEventProtoToFile(file: File, timestampedEventProtoList: ProtoStorage.TimestampedEventProtoList) {
        executeActionOnAtomicFile {
            try {
                val atomicFile = AtomicFile(file)
                val fileOutputStream = atomicFile.startWrite()
                timestampedEventProtoList.writeTo(fileOutputStream)
                atomicFile.finishWrite(fileOutputStream)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private suspend fun getAppInfos(
        infosProvider: AnalyticsInfosProvider,
        receivedHelloMessagesCount: Int
    ): AppInfos {
        return AppInfos(
            type = 0,
            os = "Android",
            deviceModel = Build.MODEL,
            osVersion = Build.VERSION.SDK_INT.toString(),
            appVersion = infosProvider.getAppVersion(),
            appBuild = infosProvider.getAppBuild(),
            receivedHelloMessagesCount = receivedHelloMessagesCount,
            placesCount = infosProvider.getPlacesCount(),
            formsCount = infosProvider.getFormsCount(),
            certificatesCount = infosProvider.getCertificatesCount(),
            statusSuccessCount = sharedPreferences.statusSuccessCount,
            userHasAZipcode = infosProvider.userHaveAZipCode(),
        )
    }

    fun getHealthInfos(
        robertManager: AnalyticsRobertManager,
        infosProvider: AnalyticsInfosProvider
    ): HealthInfos {
        return HealthInfos(
            type = 1,
            os = "Android",
            secondsTracingActivated = getProximityActiveDuration() / 1000L,
            riskLevel = robertManager.atRiskStatus?.riskLevel,
            dateSample = infosProvider.getDateSample()?.roundTimeToMidday()?.formatEpoch(),
            dateFirstSymptoms = infosProvider.getDateFirstSymptom()?.roundTimeToMidday()?.formatEpoch(),
            dateLastContactNotification = infosProvider.getDateLastContactNotification()?.roundTimeToMidday()?.formatEpoch(),
        )
    }

    private fun Long.formatEpoch() = dateFormat.format(Instant.ofEpochMilli(this))

    private fun Long.roundTimeToMidday(): Long {
        val calendar: Calendar = Calendar.getInstance()
        calendar.timeInMillis = this
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        return calendar.timeInMillis
    }

    private suspend fun getAppEvents(): List<TimestampedEvent> {
        val file = File(File(filesDir, FOLDER_NAME), FILE_NAME_APP_EVENTS)
        return getTimestampedEventFromFile(file)
    }

    private suspend fun getHealthEvents(): List<TimestampedEvent> {
        val file = File(File(filesDir, FOLDER_NAME), FILE_NAME_HEALTH_EVENTS)
        return getTimestampedEventFromFile(file)
    }

    private suspend fun getErrors(): List<TimestampedEvent> {
        val file = File(File(filesDir, FOLDER_NAME), FILE_NAME_APP_ERRORS)
        return getTimestampedEventFromFile(file)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun getTimestampedEventFromFile(file: File): List<TimestampedEvent> {
        return withContext(Dispatchers.IO) {
            executeActionOnAtomicFile {
                if (file.exists()) {
                    val atomicFile = AtomicFile(file)
                    atomicFile.openRead().use { inputStream ->
                        try {
                            ProtoStorage.TimestampedEventProtoList.parseFrom(inputStream).toDomain()
                        } catch (e: Exception) {
                            Timber.e(e)
                            emptyList()
                        }
                    }
                } else {
                    emptyList()
                }
            }
        }
    }

    private fun resetAppEvents() {
        executeActionOnAtomicFile {
            AtomicFile(File(File(filesDir, FOLDER_NAME), FILE_NAME_APP_EVENTS)).delete()
        }
    }

    private fun resetHealthEvents() {
        executeActionOnAtomicFile {
            AtomicFile(File(File(filesDir, FOLDER_NAME), FILE_NAME_HEALTH_EVENTS)).delete()
        }
    }

    private fun resetErrors() {
        executeActionOnAtomicFile {
            AtomicFile(File(File(filesDir, FOLDER_NAME), FILE_NAME_APP_ERRORS)).delete()
        }
    }

    private fun currentRoundedHourInstant(): Instant {
        return LocalDateTime.now()
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toInstant(ZoneId.systemDefault().rules.getOffset(Instant.now()))
    }

    @Synchronized
    private fun <T> executeActionOnAtomicFile(action: () -> T): T {
        return action()
    }

    companion object {
        private const val FOLDER_NAME: String = "TacAnalytics"
        private const val FILE_NAME_APP_EVENTS: String = "app_events"
        private const val FILE_NAME_APP_ERRORS: String = "app_errors"
        private const val FILE_NAME_HEALTH_EVENTS: String = "heath_events"
        private const val SHARED_PREFS_NAME: String = "TacAnalytics"
        private const val ANALYTICS_REPORT_MIN_DELAY: Long = 500L
        private const val ANALYTICS_REPORT_MAX_DELAY: Long = 2000L
    }
}
