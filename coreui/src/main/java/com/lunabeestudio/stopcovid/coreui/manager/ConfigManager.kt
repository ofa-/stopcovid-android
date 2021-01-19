/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/28/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.manager

import android.content.Context
import com.google.gson.Gson
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.stopcovid.coreui.BuildConfig
import com.lunabeestudio.stopcovid.coreui.extension.saveTo
import com.lunabeestudio.stopcovid.coreui.model.ApiConfiguration
import com.lunabeestudio.stopcovid.coreui.model.ConfigurationWrapper
import com.lunabeestudio.stopcovid.coreui.model.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File

object ConfigManager {

    private const val URL: String = BuildConfig.SERVER_URL + BuildConfig.CONFIG_JSON

    private val gson = Gson()

    suspend fun fetchOrLoad(context: Context): Configuration {
        val file = File(context.filesDir, BuildConfig.CONFIG_JSON)
        Timber.v("Fetching remote config at $URL")
        try {
            URL.saveTo(context, file)
        } catch (e: Exception) {
            Timber.e(e)
        }
        return loadLocal(context, file)
    }

    fun load(context: Context): Configuration {
        val file = File(context.filesDir, BuildConfig.CONFIG_JSON)
        Timber.v("Pre load local config")
        return runBlocking { loadLocal(context, file) }
    }

    private suspend fun loadLocal(context: Context, file: File): Configuration {
        return withContext(Dispatchers.IO) {
            if (file.exists()) {
                try {
                    Timber.v("Loading $file to object")
                    file.readText().apiToConfiguration()
                } catch (e: Exception) {
                    Timber.e(e)
                    Timber.v("Loading default file to object")
                    getDefaultAssetFile(context)
                }
            } else {
                Timber.v("Loading default file to object")
                getDefaultAssetFile(context)
            }
        }
    }

    private suspend fun getDefaultAssetFile(context: Context): Configuration {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            context.assets.open("Config/${BuildConfig.CONFIG_JSON}").use {
                it.readBytes().toString(Charsets.UTF_8).apiToConfiguration()
            }
        }
    }

    private fun String.apiToConfiguration(): Configuration {
        val configList = gson.fromJson(this, ConfigurationWrapper::class.java).config
        val jsonObject = JSONObject()
        configList.forEach {
            jsonObject.put(it.name, if (it.value is List<*>) gson.toJson(it.value) else it.value)
        }
        return gson.fromJson(jsonObject.toString(), ApiConfiguration::class.java).toDomain(gson)
    }

    fun clearLocal(context: Context) {
        File(context.filesDir, BuildConfig.CONFIG_JSON).delete()
    }
}
