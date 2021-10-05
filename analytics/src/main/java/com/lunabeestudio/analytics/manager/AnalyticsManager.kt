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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class AnalyticsManager(okHttpClient: OkHttpClient, context: Context) : LifecycleObserver {

    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
    private val analyticsServerManager = AnalyticsServerManager(okHttpClient)

    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)

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

    fun requestDeleteAnalytics(context: Context) {
        reportAppEvent(context, AppEventName.e17)
        sharedPreferences.deleteAnalyticsAfterNextStatus = true
    }

    fun register() {
        sharedPreferences.installationUUID = UUID.randomUUID().toString()
    }

    fun unregister(context: Context) {
        sharedPreferences.apply {
            installationUUID = null
            proximityStartTime = null
            proximityActiveDuration = 0L
            statusSuccessCount = 0
        }
        reset(context)
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

    fun statusDidSucceed(context: Context) {
        reportAppEvent(context, AppEventName.e16)
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
        context: Context,
        robertManager: AnalyticsRobertManager,
        analyticsInfosProvider: AnalyticsInfosProvider,
        token: String
    ) {
        if (robertManager.configuration.isAnalyticsOn && sharedPreferences.isOptIn) {
            val receivedHelloMessagesCount = robertManager.getLocalProximityCount()
            sendAppAnalytics(context, analyticsInfosProvider, token, receivedHelloMessagesCount)
            delay(Random.nextLong(ANALYTICS_REPORT_MIN_DELAY, ANALYTICS_REPORT_MAX_DELAY))
            sendHealthAnalytics(context, robertManager, analyticsInfosProvider, token)
        } else {
            reset(context)
        }
        if (sharedPreferences.deleteAnalyticsAfterNextStatus) {
            sendDeleteAnalytics(context, analyticsInfosProvider, token)
        }
    }

    private suspend fun sendAppAnalytics(
        context: Context,
        analyticsInfosProvider: AnalyticsInfosProvider,
        token: String,
        receivedHelloMessagesCount: Int
    ) {
        val appInfos = getAppInfos(analyticsInfosProvider, receivedHelloMessagesCount)
        val appEvents = getAppEvents(context)
        val appErrors = getErrors(context.filesDir)
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
                    resetAppEvents(context)
                    resetErrors(context)
                }
                is AnalyticsResult.Failure -> {
                    Timber.e(result.error)
                    if ((result.error as? HttpException)?.code() == 413) {
                        resetAppEvents(context)
                        resetErrors(context)
                    }
                }
            }
        }
    }

    private suspend fun sendHealthAnalytics(
        context: Context,
        robertManager: AnalyticsRobertManager,
        analyticsInfosProvider: AnalyticsInfosProvider,
        token: String
    ) {
        val healthEvents = getHealthEvents(context)
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
                        resetHealthEvents(context)
                    }
                }
                is AnalyticsResult.Failure -> {
                    Timber.e(result.error)
                    if ((result.error as? HttpException)?.code() == 413) {
                        resetHealthEvents(context)
                    }
                }
            }
        }
    }

    private suspend fun sendDeleteAnalytics(
        context: Context,
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
                                context.filesDir,
                                AnalyticsServiceName.ANALYTICS,
                                apiVersion,
                                error.code(),
                                error.message()
                            )
                        } else if (error?.isNoInternetException() == false) {
                            reportWSError(
                                context.filesDir,
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

    fun reset(context: Context) {
        resetAppEvents(context)
        resetHealthEvents(context)
        resetErrors(context)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Synchronized
    fun reportAppEvent(context: Context, eventName: AppEventName, desc: String? = null) {
        if (sharedPreferences.isOptIn) {
            CoroutineScope(Dispatchers.IO).launch {
                val timestampedEventList = getAppEvents(context).toMutableList()
                val timestamp = dateFormat.format(currentRoundedHourInstant())
                timestampedEventList += TimestampedEvent(eventName.name, timestamp, desc ?: "")
                val file = File(File(context.filesDir, FOLDER_NAME), FILE_NAME_APP_EVENTS)
                writeTimestampedEventProtoToFile(file, timestampedEventList.toProto())
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Synchronized
    fun reportHealthEvent(context: Context, eventName: HealthEventName, desc: String? = null) {
        if (sharedPreferences.isOptIn) {
            CoroutineScope(Dispatchers.IO).launch {
                val timestampedEventList = getHealthEvents(context).toMutableList()
                timestampedEventList += TimestampedEvent(eventName.name, dateFormat.format(currentRoundedHourInstant()), desc ?: "")
                val file = File(File(context.filesDir, FOLDER_NAME), FILE_NAME_HEALTH_EVENTS)
                writeTimestampedEventProtoToFile(file, timestampedEventList.toProto())
            }
        }
    }

    fun reportWSError(filesDir: File, wsName: String, wsVersion: String, errorCode: Int, desc: String? = null) {
        if (sharedPreferences.isOptIn) {
            if (desc?.contains("No address associated with hostname") != true) {
                val name = "ERR-${wsName.uppercase(Locale.getDefault())}-${wsVersion.uppercase(Locale.getDefault())}-$errorCode"
                reportError(filesDir, name)
            }
        }
    }

    fun reportErrorEvent(filesDir: File, errorEventName: ErrorEventName) {
        if (sharedPreferences.isOptIn) {
            reportError(filesDir, errorEventName.name)
        }
    }

    @Synchronized
    private fun reportError(filesDir: File, errorName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val timestampedEventList = getErrors(filesDir).toMutableList()
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

    private fun getHealthInfos(
        robertManager: AnalyticsRobertManager,
        infosProvider: AnalyticsInfosProvider
    ): HealthInfos {
        return HealthInfos(
            type = 1,
            os = "Android",
            secondsTracingActivated = getProximityActiveDuration() / 1000L,
            riskLevel = robertManager.atRiskStatus?.riskLevel,
            dateSample = infosProvider.getDateSample()?.formatEpoch(),
            dateFirstSymptoms = infosProvider.getDateFirstSymptom()?.formatEpoch(),
            dateLastContactNotification = infosProvider.getDateLastContactNotification()?.formatEpoch(),
        )
    }

    private fun Long.formatEpoch() = dateFormat.format(Instant.ofEpochMilli(this))

    private suspend fun getAppEvents(context: Context): List<TimestampedEvent> {
        val file = File(File(context.filesDir, FOLDER_NAME), FILE_NAME_APP_EVENTS)
        return getTimestampedEventFromFile(file)
    }

    private suspend fun getHealthEvents(context: Context): List<TimestampedEvent> {
        val file = File(File(context.filesDir, FOLDER_NAME), FILE_NAME_HEALTH_EVENTS)
        return getTimestampedEventFromFile(file)
    }

    private suspend fun getErrors(filesDir: File): List<TimestampedEvent> {
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

    private fun resetAppEvents(context: Context) {
        executeActionOnAtomicFile {
            AtomicFile(File(File(context.filesDir, FOLDER_NAME), FILE_NAME_APP_EVENTS)).delete()
        }
    }

    private fun resetHealthEvents(context: Context) {
        executeActionOnAtomicFile {
            AtomicFile(File(File(context.filesDir, FOLDER_NAME), FILE_NAME_HEALTH_EVENTS)).delete()
        }
    }

    private fun resetErrors(context: Context) {
        executeActionOnAtomicFile {
            AtomicFile(File(File(context.filesDir, FOLDER_NAME), FILE_NAME_APP_ERRORS)).delete()
        }
    }

    private fun currentRoundedHourInstant(): Instant {
        return LocalDateTime.now()
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .toInstant(ZoneOffset.UTC)
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