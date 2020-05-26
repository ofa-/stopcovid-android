/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.repository

import androidx.annotation.WorkerThread
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.domain.model.RegisterReport
import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.domain.model.StatusReport
import com.lunabeestudio.robert.datasource.RemoteServiceDataSource
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData

@WorkerThread
internal class RemoteServiceRepository(
    private val remoteServiceDataSource: RemoteServiceDataSource
) {

    suspend fun register(captcha: String): RobertResultData<RegisterReport> =
        remoteServiceDataSource.register(captcha)

    suspend fun unregister(serverStatusUpdate: ServerStatusUpdate): RobertResult =
        remoteServiceDataSource.unregister(serverStatusUpdate)

    suspend fun status(serverStatusUpdate: ServerStatusUpdate, ntpInitialTimeS: Long): RobertResultData<StatusReport> =
        remoteServiceDataSource.status(serverStatusUpdate, ntpInitialTimeS)

    suspend fun report(token: String, localProximityList: List<LocalProximity>): RobertResult =
        remoteServiceDataSource.report(token, localProximityList)

    suspend fun deleteExposureHistory(serverStatusUpdate: ServerStatusUpdate): RobertResult =
        remoteServiceDataSource.deleteExposureHistory(serverStatusUpdate)

    suspend fun eraseRemoteAlert(): RobertResult =
        remoteServiceDataSource.eraseRemoteAlert()
}
