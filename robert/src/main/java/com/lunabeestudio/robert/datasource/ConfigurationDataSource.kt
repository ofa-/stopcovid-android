/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/28/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.datasource

import android.content.Context
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.robert.model.RobertResultData

interface ConfigurationDataSource {
    suspend fun fetchConfig(context: Context): RobertResultData<List<Configuration>?>
}
