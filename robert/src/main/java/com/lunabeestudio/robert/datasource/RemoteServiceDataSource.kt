/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.datasource

import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.domain.model.RegisterReport
import com.lunabeestudio.domain.model.StatusReport
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData

interface RemoteServiceDataSource {
    suspend fun register(captcha: String): RobertResultData<RegisterReport>
    suspend fun unregister(serverStatusUpdate: ServerStatusUpdate): RobertResult
    suspend fun status(serverStatusUpdate: ServerStatusUpdate, ntpInitialTimeS: Long): RobertResultData<StatusReport>
    suspend fun report(token: String, localProximityList: List<LocalProximity>): RobertResult
    suspend fun deleteExposureHistory(serverStatusUpdate: ServerStatusUpdate): RobertResult
    suspend fun eraseRemoteAlert(): RobertResult
}
