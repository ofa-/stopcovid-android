/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.manager

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.lunabeestudio.stopcovid.coreui.BuildConfig
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.saveTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type
import java.util.Locale
import kotlin.math.abs

abstract class ServerManager {

    private var gson: Gson = Gson()

    protected abstract fun folderName(): String
    protected abstract fun prefix(context: Context): String
    protected abstract fun type(): Type
    protected abstract fun lastRefreshSharedPrefsKey(): String
    protected open fun transform(input: String): String = input
    protected open fun extension(): String = ".json"
    protected open fun url(): String = BuildConfig.SERVER_URL

    protected suspend fun fetchLast(context: Context, forceRefresh: Boolean): Boolean {
        return if (shouldRefresh(context) || forceRefresh) {
            fetchLast(context, Locale.getDefault().language)
        } else {
            Timber.v("Only use local data")
            false
        }
    }

    protected suspend fun <T> loadLocal(context: Context): T? {
        val currentLanguage = Locale.getDefault().language

        return loadFromFiles(context, currentLanguage)
            ?: loadFromAssets(context, currentLanguage)
            ?: loadFromFiles(context, UiConstants.DEFAULT_LANGUAGE)
            ?: loadFromAssets(context, UiConstants.DEFAULT_LANGUAGE)
    }

    private suspend fun fetchLast(context: Context, languageCode: String): Boolean {
        return try {
            val filename = "${prefix(context)}${languageCode}${extension()}"
            "${url()}$filename".saveTo(context, File(context.filesDir, filename))
            saveLastRefresh(context)
            true
        } catch (e: Exception) {
            Timber.e(e, "Fetching fail for $languageCode")
            false
        }
    }

    private suspend fun <T> loadFromFiles(context: Context, languageCode: String): T? {
        val fileName = "${prefix(context)}$languageCode${extension()}"
        val file = File(context.filesDir, fileName)
        return if (!file.exists()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    Timber.v("Loading $file to object")
                    gson.fromJson<T>(transform(file.readText()), type())
                } catch (e: Exception) {
                    Timber.e(e)
                    Timber.v("Loading local $languageCode file to object")
                    null
                }
            }
        }
    }

    private suspend fun <T> loadFromAssets(context: Context, languageCode: String): T? {
        val fileName = "${prefix(context)}$languageCode${extension()}"
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            if (context.assets.list(folderName())?.contains(fileName) != true) {
                null
            } else {
                gson.fromJson<T>(context.assets.open("${folderName()}/$fileName").use {
                    transform(it.readBytes().toString(Charsets.UTF_8))
                }, type())
            }
        }
    }

    private fun shouldRefresh(context: Context): Boolean {
        val lastRefreshTimeMS = PreferenceManager.getDefaultSharedPreferences(context).getLong(lastRefreshSharedPrefsKey(), 0L)
        val timeDiffMs = System.currentTimeMillis() - lastRefreshTimeMS
        return !BuildConfig.USE_LOCAL_DATA && abs(timeDiffMs) > BuildConfig.REFRESH_STRING_MIN_DURATION_MS
    }

    private fun saveLastRefresh(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putLong(lastRefreshSharedPrefsKey(), System.currentTimeMillis())
        }
    }

    fun clearLocal(context: Context) {
        val filename = "${prefix(context)}${Locale.getDefault().language}${extension()}"
        File(context.filesDir, filename).delete()
        val defaultFilename = "${prefix(context)}${UiConstants.DEFAULT_LANGUAGE}${extension()}"
        File(context.filesDir, defaultFilename).delete()
    }
}
