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
import com.lunabeestudio.stopcovid.coreui.BuildConfig
import com.lunabeestudio.stopcovid.coreui.extension.saveTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

object ConfigManager {

    private const val URL: String = BuildConfig.SERVER_URL + BuildConfig.CONFIG_JSON

    suspend fun fetchOrLoad(context: Context): String {
        val file = File(context.filesDir, BuildConfig.CONFIG_JSON)
        Timber.v("Fetching remote config at $URL")
        try {
            URL.saveTo(context, file)
        } catch (e: Exception) {
            Timber.e(e)
        }
        return loadLocal(context, file)
    }

    private suspend fun loadLocal(context: Context, file: File): String {
        return withContext(Dispatchers.IO) {
            if (file.exists()) {
                try {
                    Timber.v("Loading $file to object")
                    file.readText()
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

    private suspend fun getDefaultAssetFile(context: Context): String {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            context.assets.open("Config/${BuildConfig.CONFIG_JSON}").use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        }
    }

    fun clearLocal(context: Context) {
        File(context.filesDir, BuildConfig.CONFIG_JSON).delete()
    }
}
