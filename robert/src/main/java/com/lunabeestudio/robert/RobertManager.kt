/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/11/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert

import androidx.lifecycle.LiveData
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.HelloBuilder
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.robert.manager.LocalProximityFilter
import com.lunabeestudio.robert.model.AtRiskStatus
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.utils.Event

interface RobertManager {
    val configuration: Configuration

    var shouldReloadBleSettings: Boolean

    val canActivateProximity: Boolean

    val isRegistered: Boolean

    val isProximityActive: Boolean

    val isAtRisk: Boolean?

    val isWarningAtRisk: Boolean?

    val lastWarningReceivedDate: Long?

    val atRiskStatus: LiveData<Event<AtRiskStatus>>

    val atRiskLastRefresh: Long?

    val lastExposureTimeframe: Int

    val lastRiskReceivedDate: Long?

    val isSick: Boolean

    val reportSymptomsStartDate: Long?

    val reportPositiveTestDate: Long?

    val filteringMode: LocalProximityFilter.Mode

    suspend fun refreshConfig(application: RobertApplication): RobertResult

    suspend fun generateCaptcha(type: String, local: String): RobertResultData<String>

    suspend fun getCaptchaImage(captchaId: String, path: String): RobertResult

    suspend fun getCaptchaAudio(captchaId: String, path: String): RobertResult

    suspend fun register(
        application: RobertApplication,
        captcha: String,
        captchaId: String,
        activateProximity: Boolean,
    ): RobertResult

    suspend fun activateProximity(
        application: RobertApplication,
        statusTried: Boolean = false,
    ): RobertResult

    fun deactivateProximity(application: RobertApplication)

    suspend fun updateStatus(robertApplication: RobertApplication): RobertResult

    suspend fun clearOldData()

    suspend fun clearLocalData(application: RobertApplication)

    suspend fun report(
        token: String,
        firstSymptoms: Int?,
        positiveTest: Int?,
        application: RobertApplication,
    ): RobertResult

    suspend fun wreportIfNeeded(application: RobertApplication, shouldRetry: Boolean)

    suspend fun storeLocalProximity(vararg localProximity: LocalProximity)

    suspend fun getCurrentHelloBuilder(): RobertResultData<HelloBuilder>

    suspend fun eraseLocalHistory(): RobertResult

    suspend fun eraseRemoteExposureHistory(application: RobertApplication): RobertResult

    suspend fun eraseRemoteAlert(): RobertResult

    suspend fun quitStopCovid(application: RobertApplication): RobertResult

    fun refreshAtRisk()
}
