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
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.core.util.AtomicFile
import androidx.lifecycle.LifecycleObserver
import com.lunabeestudio.analytics.extension.toDomain
import com.lunabeestudio.analytics.extension.toProto
import com.lunabeestudio.analytics.model.AnalyticsResult
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.analytics.model.AppInfos
import com.lunabeestudio.analytics.model.HealthEventName
import com.lunabeestudio.analytics.model.HealthInfos
import com.lunabeestudio.analytics.model.TimestampedEvent
import com.lunabeestudio.analytics.network.AnalyticsServerManager
import com.lunabeestudio.analytics.network.model.SendAnalyticsRQ
import com.lunabeestudio.analytics.proto.ProtoStorage
import com.lunabeestudio.analytics.proxy.AnalyticsInfosProvider
import com.lunabeestudio.analytics.proxy.AnalyticsRobertManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object AnalyticsManager : LifecycleObserver {

    private const val FOLDER_NAME: String = "TacAnalytics"
    private const val FILE_NAME_APP_EVENTS: String = "app_events"
    private const val FILE_NAME_APP_ERRORS: String = "app_errors"
    private const val FILE_NAME_HEALTH_EVENTS: String = "heath_events"
    private const val SHARED_PREFS_NAME: String = "TacAnalytics"
    private const val SHARED_PREFS_INSTALLATION_UUID: String = "Shared.Prefs.Installation.UUID"
    private const val SHARED_PREFS_PROXIMITY_START_TIME: String = "Shared.Prefs.Proximity.Start.Time"
    private const val SHARED_PREFS_PROXIMITY_ACTIVE_DURATION: String = "Shared.Prefs.Proximity.Active.Duration"
    private const val SHARED_PREFS_STATUS_SUCCESS_COUNT: String = "Shared.Prefs.Status.Success.Count"

    private val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.FRANCE)

    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        if (!getSharedPrefs(context).contains(SHARED_PREFS_INSTALLATION_UUID)) {
            getSharedPrefs(context).edit {
                putString(SHARED_PREFS_INSTALLATION_UUID, UUID.randomUUID().toString())
            }
        }
    }

    private fun getSharedPrefs(context: Context): SharedPreferences {
        if (!AnalyticsManager::sharedPreferences.isInitialized) {
            sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        }
        return sharedPreferences
    }

    fun register(context: Context) {
        getSharedPrefs(context).edit {
            putString(SHARED_PREFS_INSTALLATION_UUID, UUID.randomUUID().toString())
        }
    }

    fun unregister(context: Context) {
        getSharedPrefs(context).edit {
            remove(SHARED_PREFS_INSTALLATION_UUID)
            remove(SHARED_PREFS_PROXIMITY_START_TIME)
            remove(SHARED_PREFS_PROXIMITY_ACTIVE_DURATION)
            remove(SHARED_PREFS_STATUS_SUCCESS_COUNT)
        }
        reset(context)
    }

    fun proximityDidStart(context: Context) {
        getSharedPrefs(context).edit {
            putLong(SHARED_PREFS_PROXIMITY_START_TIME, System.currentTimeMillis())
        }
    }

    fun proximityDidStop(context: Context) {
        getSharedPrefs(context).edit {
            putLong(SHARED_PREFS_PROXIMITY_ACTIVE_DURATION, getProximityActiveDuration(context))
            remove(SHARED_PREFS_PROXIMITY_START_TIME)
        }
    }

    fun statusDidSucceed(context: Context) {
        reportAppEvent(context, AppEventName.e16)
        getSharedPrefs(context).edit {
            putInt(SHARED_PREFS_STATUS_SUCCESS_COUNT, getSharedPrefs(context).getInt(SHARED_PREFS_STATUS_SUCCESS_COUNT, 0) + 1)
        }
    }

    private fun getProximityActiveDuration(context: Context): Long {
        val oldDuration = getSharedPrefs(context).getLong(SHARED_PREFS_PROXIMITY_ACTIVE_DURATION, 0L)
        val addedDuration = System.currentTimeMillis() - getSharedPrefs(context).getLong(
            SHARED_PREFS_PROXIMITY_START_TIME,
            System.currentTimeMillis()
        )
        return oldDuration + addedDuration
    }

    suspend fun sendAnalytics(
        context: Context,
        robertManager: AnalyticsRobertManager,
        analyticsInfosProvider: AnalyticsInfosProvider,
        token: String
    ) {
        if (robertManager.configuration.isAnalyticsOn) {
            val receivedHelloMessagesCount = robertManager.getLocalProximityCount()
            sendAppAnalytics(context, analyticsInfosProvider, token, receivedHelloMessagesCount)
            sendHealthAnalytics(context, robertManager, analyticsInfosProvider, token, receivedHelloMessagesCount)
        } else {
            reset(context)
        }
    }

    private suspend fun sendAppAnalytics(
        context: Context,
        analyticsInfosProvider: AnalyticsInfosProvider,
        token: String,
        receivedHelloMessagesCount: Int
    ) {
        val appInfos = getAppInfos(context, analyticsInfosProvider, receivedHelloMessagesCount)
        val appEvents = getAppEvents(context)
        val appErrors = getErrors(context.filesDir)
        val sendAnalyticsRQ = SendAnalyticsRQ(
            installationUuid = sharedPreferences.getString(SHARED_PREFS_INSTALLATION_UUID, null) ?: UUID.randomUUID().toString(),
            infos = appInfos,
            events = appEvents,
            errors = appErrors
        )
        withContext(Dispatchers.IO) {
            val result = AnalyticsServerManager.sendAnalytics(
                context,
                analyticsInfosProvider.getBaseUrl(),
                analyticsInfosProvider.getCertificateSha256(),
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
        token: String,
        receivedHelloMessagesCount: Int
    ) {
        val healthInfos = getHealthInfos(context, robertManager, analyticsInfosProvider, receivedHelloMessagesCount)
        val healthEvents = getHealthEvents(context)
        val sendAnalyticsRQ = SendAnalyticsRQ(
            installationUuid = UUID.randomUUID().toString(),
            infos = healthInfos,
            events = healthEvents,
            errors = emptyList()
        )
        withContext(Dispatchers.IO) {
            val result = AnalyticsServerManager.sendAnalytics(
                context,
                analyticsInfosProvider.getBaseUrl(),
                analyticsInfosProvider.getCertificateSha256(),
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

    fun reset(context: Context) {
        resetAppEvents(context)
        resetHealthEvents(context)
        resetErrors(context)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Synchronized
    fun reportAppEvent(context: Context, eventName: AppEventName, desc: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val timestampedEventList = getAppEvents(context).toMutableList()
            timestampedEventList += TimestampedEvent(eventName.name, dateFormat.format(Date()), desc ?: "")
            val file = File(File(context.filesDir, FOLDER_NAME), FILE_NAME_APP_EVENTS)
            writeTimestampedEventProtoToFile(file, timestampedEventList.toProto())
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Synchronized
    fun reportHealthEvent(context: Context, eventName: HealthEventName, desc: String? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val timestampedEventList = getHealthEvents(context).toMutableList()
            timestampedEventList += TimestampedEvent(eventName.name, dateFormat.format(Date()), desc ?: "")
            val file = File(File(context.filesDir, FOLDER_NAME), FILE_NAME_HEALTH_EVENTS)
            writeTimestampedEventProtoToFile(file, timestampedEventList.toProto())
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Synchronized
    fun reportWSError(filesDir: File, wsName: String, wsVersion: String, errorCode: Int, desc: String? = null) {
        if (desc?.contains("No address associated with hostname") != true) {
            CoroutineScope(Dispatchers.IO).launch {
                val name = "ERR-${wsName.toUpperCase(Locale.getDefault())}-${wsVersion.toUpperCase(Locale.getDefault())}-$errorCode"
                val timestampedEventList = getErrors(filesDir).toMutableList()
                timestampedEventList += TimestampedEvent(name, dateFormat.format(Date()), desc ?: "")
                val file = File(File(filesDir, FOLDER_NAME), FILE_NAME_APP_ERRORS)
                writeTimestampedEventProtoToFile(file, timestampedEventList.toProto())
            }
        }
    }

    private fun writeTimestampedEventProtoToFile(file: File, timestampedEventProtoList: ProtoStorage.TimestampedEventProtoList) {
        executeActionOnAtomicFile {
            val atomicFile = AtomicFile(file)
            val fileOutputStream = atomicFile.startWrite()
            timestampedEventProtoList.writeTo(fileOutputStream)
            atomicFile.finishWrite(fileOutputStream)
        }
    }

    private fun getAppInfos(context: Context,
        infosProvider: AnalyticsInfosProvider,
        receivedHelloMessagesCount: Int): AppInfos {
        return AppInfos(
            type = 0,
            os = "Android",
            deviceModel = Build.MODEL,
            osVersion = Build.VERSION.SDK_INT.toString(),
            appVersion = infosProvider.getAppVersion(),
            appBuild = infosProvider.getAppBuild(),
            receivedHelloMessagesCount = receivedHelloMessagesCount,
            secondsTracingActivated = getProximityActiveDuration(context) / 1000L,
            placesCount = infosProvider.getPlacesCount(),
            formsCount = infosProvider.getFormsCount(),
            certificatesCount = infosProvider.getCertificatesCount(),
            statusSuccessCount = sharedPreferences.getInt(SHARED_PREFS_STATUS_SUCCESS_COUNT, 0),
            userHasAZipcode = infosProvider.userHaveAZipCode(),
        )
    }

    private fun getHealthInfos(context: Context,
        robertManager: AnalyticsRobertManager,
        infosProvider: AnalyticsInfosProvider,
        receivedHelloMessagesCount: Int): HealthInfos {
        return HealthInfos(
            type = 1,
            os = "Android",
            deviceModel = Build.MODEL,
            osVersion = Build.VERSION.SDK_INT.toString(),
            appVersion = infosProvider.getAppVersion(),
            appBuild = infosProvider.getAppBuild(),
            receivedHelloMessagesCount = receivedHelloMessagesCount,
            secondsTracingActivated = getProximityActiveDuration(context) / 1000L,
            placesCount = infosProvider.getPlacesCount(),
            riskLevel = robertManager.atRiskStatus?.riskLevel,
            dateSample = infosProvider.getDateSample()?.let { dateFormat.format(Date(it)) },
            dateFirstSymptoms = infosProvider.getDateFirstSymptom()?.let { dateFormat.format(Date(it)) },
            dateLastContactNotification = infosProvider.getDateLastContactNotification()?.let { dateFormat.format(Date(it)) }
        )
    }

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
                        ProtoStorage.TimestampedEventProtoList.parseFrom(inputStream).toDomain()
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

    @Synchronized
    private fun <T> executeActionOnAtomicFile(action: () -> T): T {
        return action()
    }
}