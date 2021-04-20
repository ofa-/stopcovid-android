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
import com.google.gson.JsonSyntaxException
import com.lunabeestudio.stopcovid.coreui.BuildConfig
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.getETagSharedPrefs
import com.lunabeestudio.stopcovid.coreui.extension.getFirstSupportedLanguage
import com.lunabeestudio.stopcovid.coreui.extension.saveTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type
import kotlin.math.abs

abstract class ServerManager<T> {

    private var gson: Gson = Gson()

    protected abstract val url: String
    protected abstract val folderName: String
    protected abstract val prefix: String
    protected abstract val type: Type
    protected abstract val lastRefreshSharedPrefsKey: String
    protected open fun transform(input: String): String = input
    private val extension: String = ".json"

    protected suspend fun loadLocal(context: Context): T? {
        val currentLanguage = context.getFirstSupportedLanguage()

        return loadFromAssets(context, currentLanguage)
            ?: loadFromFiles(context, currentLanguage)
    }

    protected suspend fun fetchLast(context: Context, forceRefresh: Boolean): Boolean {
        return if (shouldRefresh(context) || forceRefresh) {
            fetchLast(context)
        } else {
            Timber.v("Only use local data")
            false
        }
    }

    private suspend fun fetchLast(context: Context): Boolean {
        val languageCode = context.getFirstSupportedLanguage()
        val filename = "$prefix$languageCode$extension"
        val tmpFile = File(context.filesDir, "$filename.bck")
        return try {
            "$url$filename".saveTo(context, tmpFile, filename)
            if (fileNotCorrupted(tmpFile)) {
                tmpFile.copyTo(File(context.filesDir, filename), overwrite = true, bufferSize = 4 * 1024)
                saveLastRefresh(context)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Fetching fail for $languageCode")
            false
        }
    }

    private suspend fun loadFromFiles(context: Context, languageCode: String): T? {
        val fileName = "$prefix$languageCode$extension"
        val file = File(context.filesDir, fileName)
        return if (!file.exists()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    Timber.v("Loading $file to object")
                    gson.fromJson<T>(transform(file.readText()), type)
                } catch (e: Exception) {
                    Timber.e(e)
                    Timber.v("Loading local $languageCode file to object")
                    null
                }
            }
        }
    }

    private suspend fun loadFromAssets(context: Context, languageCode: String): T? {
        val fileName = "$prefix$languageCode$extension"
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            // Remove suffix to fix list asset issue with older API
            if (context.assets.list(folderName.removeSuffix("/"))?.contains(fileName) != true) {
                null
            } else {
                gson.fromJson<T>(context.assets.open("$folderName$fileName").use {
                    transform(it.readBytes().toString(Charsets.UTF_8))
                }, type)
            }
        }
    }

    private suspend fun fileNotCorrupted(file: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                gson.fromJson<T>(transform(file.readText()), type)
                true
            } catch (e: JsonSyntaxException) {
                Timber.e(e, "Fetched corrupted file ${file.name}")
                false
            }
        }
    }

    private fun shouldRefresh(context: Context): Boolean {
        val lastRefreshTimeMS = PreferenceManager.getDefaultSharedPreferences(context).getLong(lastRefreshSharedPrefsKey, 0L)
        val timeDiffMs = System.currentTimeMillis() - lastRefreshTimeMS
        return !BuildConfig.USE_LOCAL_DATA && abs(timeDiffMs) > BuildConfig.REFRESH_STRING_MIN_DURATION_MS
    }

    private fun saveLastRefresh(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putLong(lastRefreshSharedPrefsKey, System.currentTimeMillis())
        }
    }

    fun clearLocal(context: Context) {
        val filename = "$prefix${context.getFirstSupportedLanguage()}$extension"
        File(context.filesDir, filename).delete()
        val defaultFilename = "$prefix${UiConstants.DEFAULT_LANGUAGE}$extension"
        File(context.filesDir, defaultFilename).delete()
        context.getETagSharedPrefs().edit {
            remove(filename)
            remove(defaultFilename)
        }
    }
}
