/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.manager

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.lunabeestudio.stopcovid.coreui.BuildConfig
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.saveTo
import timber.log.Timber
import java.io.File
import java.lang.reflect.Type
import java.util.Locale
import java.util.concurrent.TimeUnit

abstract class ServerManager {

    private var gson: Gson = Gson()

    protected abstract fun folderName(): String
    protected abstract fun prefix(context: Context): String
    protected abstract fun fallbackFileName(context: Context): String
    protected abstract fun type(): Type
    protected abstract fun lastRefreshSharedPrefsKey(): String
    protected open fun transform(input: String): String = input
    protected open fun extension(): String = ".json"
    protected open fun url(): String = BuildConfig.SERVER_URL

    @WorkerThread
    protected fun fetchLast(context: Context, languageCode: String): Boolean {
        return try {
            if (shouldRefresh(context)) {
                val filename = "${prefix(context)}${languageCode}${extension()}"
                Timber.d("Fetching remote data at ${url()}$filename")
                "${url()}$filename".saveTo(context, File(context.filesDir, filename))
                saveLastRefresh(context)
                true
            } else {
                Timber.d("Only use local data")
                false
            }
        } catch (e: Exception) {
            Timber.d("Fetching fail for $languageCode")
            if (languageCode != UiConstants.DEFAULT_LANGUAGE) {
                Timber.d("Trying for ${UiConstants.DEFAULT_LANGUAGE}")
                fetchLast(context, UiConstants.DEFAULT_LANGUAGE)
            } else {
                false
            }
        }
    }

    protected fun <T> loadLocal(context: Context): T {
        var fileName = "${prefix(context)}${Locale.getDefault().language}${extension()}"
        if (!File(context.filesDir, fileName).exists()) {
            fileName = fallbackFileName(context)
        }
        return if (!File(context.filesDir, fileName).exists()) {
            Timber.d("Loading default file to object")
            getDefaultAssetFile<T>(context)
        } else {
            try {
                Timber.d("Loading file to object")
                gson.fromJson<T>(transform(File(context.filesDir, fileName).readText()), type())
            } catch (e: java.lang.Exception) {
                Timber.e(e)
                Timber.d("Loading default file to object")
                getDefaultAssetFile<T>(context)
            }
        }
    }

    private fun <T> getDefaultAssetFile(context: Context): T {
        var fileName = "${prefix(context)}${Locale.getDefault().language}${extension()}"
        if (context.assets.list(folderName())?.contains(fileName) != true) {
            fileName = fallbackFileName(context)
        }
        return gson.fromJson(context.assets.open("${folderName()}/$fileName").use {
            transform(it.readBytes().toString(Charsets.UTF_8))
        }, type())
    }

    private fun shouldRefresh(context: Context): Boolean {
        return !BuildConfig.USE_LOCAL_DATA
            && System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1L) > PreferenceManager.getDefaultSharedPreferences(context)
            .getLong(lastRefreshSharedPrefsKey(), 0L)
    }

    private fun saveLastRefresh(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putLong(lastRefreshSharedPrefsKey(), System.currentTimeMillis())
        }
    }
}