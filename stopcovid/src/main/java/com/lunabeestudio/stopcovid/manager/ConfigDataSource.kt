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
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.robert.datasource.ConfigurationDataSource
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.stopcovid.coreui.manager.ConfigManager
import com.lunabeestudio.stopcovid.extension.remoteToRobertException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object ConfigDataSource : ConfigurationDataSource {

    override suspend fun fetchOrLoadConfig(context: Context): RobertResultData<Configuration> {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            try {
                val configuration = ConfigManager.fetchOrLoad(context)
                RobertResultData.Success(configuration)
            } catch (e: Exception) {
                Timber.e(e)
                RobertResultData.Failure(e.remoteToRobertException())
            }
        }
    }

    override fun loadConfig(context: Context): Configuration {
        return ConfigManager.load(context)
    }
}