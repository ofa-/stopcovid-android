/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/28/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import com.google.gson.Gson
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.robert.datasource.ConfigurationDataSource
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.model.UnknownException
import com.lunabeestudio.stopcovid.coreui.manager.ConfigManager
import com.lunabeestudio.stopcovid.model.ConfigurationWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object ConfigDataSource : ConfigurationDataSource {

    private val gson = Gson()

    override suspend fun fetchConfig(context: Context): RobertResultData<List<Configuration>?> {
        return withContext(Dispatchers.IO) {
            try {
                val response = ConfigManager.fetchLast(context)
                RobertResultData.Success(gson.fromJson(response, ConfigurationWrapper::class.java).config)
            } catch (e: Exception) {
                Timber.e(e)
                RobertResultData.Failure<List<Configuration>?>(UnknownException(e.localizedMessage ?: ""))
            }
        }
    }
}