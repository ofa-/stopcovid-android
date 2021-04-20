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
import androidx.core.content.edit
import com.google.gson.Gson
import com.lunabeestudio.domain.model.Calibration
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.coreui.EnvConstant
import com.lunabeestudio.stopcovid.coreui.extension.getETagSharedPrefs
import com.lunabeestudio.stopcovid.coreui.extension.saveTo
import com.lunabeestudio.stopcovid.coreui.model.ApiCalibration
import com.lunabeestudio.stopcovid.coreui.model.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

object CalibrationManager {

    private val gson = Gson()

    suspend fun fetchOrLoad(context: Context): Calibration {
        val file = File(context.filesDir, ConfigConstant.Calibration.LOCAL_FILENAME)
        Timber.v("Fetching remote calibration at ${getUrl()}")
        try {
            getUrl().saveTo(context, file, ConfigConstant.Calibration.LOCAL_FILENAME)
        } catch (e: Exception) {
            Timber.e(e)
        }
        return loadLocal(context, file)
    }

    fun load(context: Context): Calibration {
        val file = File(context.filesDir, ConfigConstant.Calibration.LOCAL_FILENAME)
        Timber.v("Pre load local calibration")
        return runBlocking { loadLocal(context, file) }
    }

    private suspend fun loadLocal(context: Context, file: File): Calibration {
        return withContext(Dispatchers.IO) {
            if (file.exists()) {
                try {
                    Timber.v("Loading $file to object")
                    gson.fromJson(file.reader(), ApiCalibration::class.java).toDomain()
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

    private fun getUrl(): String {
        return ConfigConstant.Calibration.URL + EnvConstant.Prod.calibrationFilename
    }

    private suspend fun getDefaultAssetFile(context: Context): Calibration {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            context.assets.open(getAssetPath()).use {
                gson.fromJson(it.reader(), ApiCalibration::class.java).toDomain()
            }
        }
    }

    private fun getAssetPath(): String {
        return ConfigConstant.Calibration.FOLDER + EnvConstant.Prod.calibrationFilename
    }

    fun clearLocal(context: Context) {
        context.getETagSharedPrefs().edit {
            remove(ConfigConstant.Calibration.LOCAL_FILENAME)
        }
        File(context.filesDir, ConfigConstant.Calibration.LOCAL_FILENAME).delete()
    }
}
