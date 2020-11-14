/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/28/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import com.google.gson.Gson
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.robert.datasource.ConfigurationDataSource
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.stopcovid.coreui.manager.ConfigManager
import com.lunabeestudio.stopcovid.extension.remoteToRobertException
import com.lunabeestudio.stopcovid.model.ConfigurationWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object ConfigDataSource : ConfigurationDataSource {

    private val gson = Gson()

    override suspend fun fetchOrLoadConfig(context: Context): RobertResultData<List<Configuration>?> {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            try {
                val json = ConfigManager.fetchOrLoad(context)
                RobertResultData.Success(gson.fromJson(json, ConfigurationWrapper::class.java).config)
            } catch (e: Exception) {
                Timber.e(e)
                RobertResultData.Failure(e.remoteToRobertException())
            }
        }
    }
}
