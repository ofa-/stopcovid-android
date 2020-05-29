/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/11/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert

import android.content.Context
import com.lunabeestudio.domain.model.DeviceParameterCorrection
import com.lunabeestudio.domain.model.HelloBuilder
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData

interface RobertManager {

    val isRegistered: Boolean

    val isProximityActive: Boolean

    val isAtRisk: Boolean

    val atRiskLastRefresh: Long?

    val atRiskMinHourContactNotif: Int

    val atRiskMaxHourContactNotif: Int

    val lastExposureTimeframe: Int

    val quarantinePeriod: Int

    val isSick: Boolean

    val calibration: List<DeviceParameterCorrection>

    val serviceUUID: String

    val characteristicUUID: String

    val backgroundServiceManufacturerData: String

    suspend fun register(application: RobertApplication, captcha: String): RobertResult

    suspend fun activateProximity(application: RobertApplication, statusTried: Boolean = false): RobertResult

    fun deactivateProximity(application: RobertApplication)

    suspend fun updateStatus(robertApplication: RobertApplication): RobertResult

    fun clearOldData()

    fun clearLocalData(application: RobertApplication)

    suspend fun report(token: String, firstSymptoms: Int, application: RobertApplication): RobertResult

    suspend fun storeLocalProximity(vararg localProximity: LocalProximity)

    fun getCurrentHelloBuilder(): RobertResultData<HelloBuilder>

    suspend fun eraseLocalHistory(): RobertResult

    suspend fun eraseRemoteExposureHistory(): RobertResult

    suspend fun eraseRemoteAlert(): RobertResult

    suspend fun quitStopCovid(application: RobertApplication): RobertResult
}
