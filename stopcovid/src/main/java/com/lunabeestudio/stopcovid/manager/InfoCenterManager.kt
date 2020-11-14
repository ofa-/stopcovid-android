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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.stopcovid.BuildConfig
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.fixFormatter
import com.lunabeestudio.stopcovid.coreui.extension.saveTo
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.extension.areInfoNotificationsEnabled
import com.lunabeestudio.stopcovid.extension.lastInfoCenterFetch
import com.lunabeestudio.stopcovid.extension.lastInfoCenterRefresh
import com.lunabeestudio.stopcovid.model.InfoCenterEntry
import com.lunabeestudio.stopcovid.model.InfoCenterLastUpdatedAt
import com.lunabeestudio.stopcovid.model.InfoCenterTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.minutes

object InfoCenterManager {

    private var gson: Gson = Gson()

    private const val lastUpdatePrefix: String = "info-center-lastupdate"
    private const val infosPrefix: String = "info-center"
    private const val tagsPrefix: String = "info-tags"
    private const val stringPrefix: String = "info-labels-"
    private const val stringFallbackFileName: String = "info-labels-en.json"
    private val url: String = com.lunabeestudio.stopcovid.coreui.BuildConfig.SERVER_URL + BuildConfig.INFO_CENTER_FOLDER
    private val typeInfoCenterLastUpdatedAt: Type = object : TypeToken<InfoCenterLastUpdatedAt>() {}.type
    private val typeInfoCenterEntry: Type = object : TypeToken<List<InfoCenterEntry>>() {}.type
    private val typeInfoCenterTag: Type = object : TypeToken<List<InfoCenterTag>>() {}.type
    private val typeInfoCenterStrings: Type = object : TypeToken<Map<String, String>>() {}.type

    @OptIn(ExperimentalTime::class)
    private val refreshMinDelay: Duration = 5.minutes

    private var lastUpdatedAt: InfoCenterLastUpdatedAt? = null

    private val _infos: MutableLiveData<Event<List<InfoCenterEntry>>> = MutableLiveData()
    val infos: LiveData<Event<List<InfoCenterEntry>>>
        get() = _infos

    private val _tags: MutableLiveData<Event<List<InfoCenterTag>>> = MutableLiveData()
    val tags: LiveData<Event<List<InfoCenterTag>>>
        get() = _tags

    private val _strings: MutableLiveData<Event<Map<String, String>>> = MutableLiveData()
    val strings: LiveData<Event<Map<String, String>>>
        get() = _strings

    private var prevLanguage: String? = null

    fun initialize(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            prevLanguage = Locale.getDefault().language
            loadLocal(context)
        }
    }

    suspend fun refreshIfNeeded(context: Context) {
        val forceRefresh = prevLanguage != Locale.getDefault().language
        fetchLastTimestamp(context)
        if (fetchLast(context, infosPrefix, null, forceRefresh)
            && fetchLast(context, tagsPrefix, null, forceRefresh)
            && fetchLast(context, stringPrefix, Locale.getDefault().language, forceRefresh)) {
            saveLastRefresh(context)
        }
        prevLanguage = Locale.getDefault().language
        loadLocal(context)
    }

    private fun loadLocal(context: Context) {
        lastUpdatedAt = loadLocal(
            context,
            lastUpdatePrefix,
            false,
            null,
            { it },
            typeInfoCenterLastUpdatedAt
        )
        loadLocal<List<InfoCenterEntry>>(
            context,
            infosPrefix,
            false,
            null,
            { it },
            typeInfoCenterEntry
        )?.let { infos ->
            val sortedInfos = infos.sortedByDescending { it.timestamp }
            if (this.infos.value?.peekContent() != sortedInfos) {
                this._infos.postValue(Event(sortedInfos))
            }
        }
        loadLocal<List<InfoCenterTag>>(
            context,
            tagsPrefix,
            false,
            null,
            { it },
            typeInfoCenterTag
        )?.let { tags ->
            if (this.tags.value?.peekContent() != tags) {
                this._tags.postValue(Event(tags))
            }
        }
        loadLocal<Map<String, String>>(
            context,
            stringPrefix,
            true,
            stringFallbackFileName,
            { it.fixFormatter() },
            typeInfoCenterStrings
        )?.let { strings ->
            if (this.strings.value?.peekContent() != strings) {
                this._strings.postValue(Event(strings))
            }
        }
    }

    private suspend fun fetchLastTimestamp(context: Context): Boolean {
        return try {
            val filename = "$lastUpdatePrefix.json"
            Timber.v("Fetching remote data at $url$filename")
            "$url$filename".saveTo(context, File(context.filesDir, filename))
            true
        } catch (e: Exception) {
            Timber.v("Fetching fail for last timestamp")
            false
        }
    }

    private suspend fun fetchLast(context: Context,
        prefix: String,
        languageCode: String?,
        forceRefresh: Boolean): Boolean {
        return try {
            if (shouldRefresh(context) || forceRefresh) {
                val filename = "$prefix${languageCode ?: ""}.json"
                Timber.v("Fetching remote data at $url$filename")
                "$url$filename".saveTo(context, File(context.filesDir, filename))
                true
            } else {
                Timber.v("Use local data")
                false
            }
        } catch (e: Exception) {
            Timber.e("Fetching fail for $languageCode")
            if (languageCode != null && languageCode != UiConstants.DEFAULT_LANGUAGE && !localFileExists(context, prefix)) {
                Timber.v("Trying for ${UiConstants.DEFAULT_LANGUAGE}")
                fetchLast(context, prefix, UiConstants.DEFAULT_LANGUAGE, forceRefresh)
            } else {
                false
            }
        }
    }

    private fun localFileExists(context: Context, prefix: String): Boolean {
        val fileName = "$prefix${Locale.getDefault().language}.json"
        val file = File(context.filesDir, fileName)
        return file.exists()
    }

    private fun <T> loadLocal(context: Context,
        prefix: String,
        isLocalized: Boolean,
        fallbackFileName: String?,
        transform: (String) -> String,
        type: Type): T? {
        val fileName = "$prefix${if (isLocalized) Locale.getDefault().language else ""}.json"
        var file = File(context.filesDir, fileName)
        if (!file.exists() && fallbackFileName != null) {
            file = File(context.filesDir, fallbackFileName)
        }
        return if (file.exists()) {
            try {
                Timber.v("Loading $file to object")
                gson.fromJson<T>(transform(file.readText()), type)
            } catch (e: java.lang.Exception) {
                Timber.e(e)
                null
            }
        } else {
            Timber.v("Nothing to load")
            null
        }
    }

    private fun isLastUpdatedAtDifferent(context: Context): Boolean = (lastUpdatedAt?.lastUpdatedAt
        ?: 0) > PreferenceManager.getDefaultSharedPreferences(context).lastInfoCenterRefresh

    @OptIn(ExperimentalTime::class)
    private fun shouldRefresh(context: Context): Boolean {
        val isAppInForeground = (context.applicationContext as? StopCovid)?.isAppInForeground == true
        val isMinDelayElapsed = System.currentTimeMillis().milliseconds - refreshMinDelay > PreferenceManager.getDefaultSharedPreferences(
            context
        ).lastInfoCenterFetch
        return lastUpdatedAt == null ||
            isLastUpdatedAtDifferent(context) ||
            (isAppInForeground && isMinDelayElapsed)
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun saveLastRefresh(context: Context) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.lastInfoCenterFetch = System.currentTimeMillis().milliseconds
        val isAppInBackground = (context.applicationContext as? StopCovid)?.isAppInForeground != true
        if (sharedPreferences.areInfoNotificationsEnabled
            && lastUpdatedAt != null
            && isLastUpdatedAtDifferent(context)
            && isAppInBackground) {
            sendUpdateNotification(context)
        }
        lastUpdatedAt?.lastUpdatedAt?.let { lastUpdatedAt ->
            sharedPreferences.lastInfoCenterRefresh = lastUpdatedAt
        }
    }

    private suspend fun sendUpdateNotification(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(Constants.SharedPrefs.HAS_NEWS, true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (StringsManager.strings.isEmpty()) {
            StringsManager.initialize(context)
        }
        val strings = StringsManager.strings

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.NEWS.channelId,
                strings["notification.channel.news.title"] ?: "News",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(
            context,
            UiConstants.Notification.NEWS.channelId
        )
            .setContentTitle(strings["info.notification.newsAvailable.title"])
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setSmallIcon(com.lunabeestudio.stopcovid.coreui.R.drawable.ic_notification_bar)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(strings["info.notification.newsAvailable.body"])
            )
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(UiConstants.Notification.TIME.notificationId, notification)
    }
}
