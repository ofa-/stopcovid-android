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
import com.lunabeestudio.domain.model.DeviceParameterCorrection
import com.lunabeestudio.domain.model.HelloBuilder
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.robert.manager.LocalProximityFilter
import com.lunabeestudio.robert.model.AtRiskStatus
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.utils.Event

interface RobertManager {
    var shouldReloadBleSettings: Boolean

    val isRegistered: Boolean

    val isProximityActive: Boolean

    val isAtRisk: Boolean?

    val atRiskStatus: LiveData<Event<AtRiskStatus>>

    val atRiskLastRefresh: Long?

    val atRiskMinHourContactNotif: Int

    val atRiskMaxHourContactNotif: Int

    val lastExposureTimeframe: Int

    val quarantinePeriod: Int

    val isSick: Boolean

    val calibration: List<DeviceParameterCorrection>

    val filteringConfig: String

    val filteringMode: LocalProximityFilter.Mode

    val serviceUUID: String

    val characteristicUUID: String

    val backgroundServiceManufacturerData: String

    val checkStatusFrequencyHour: Float

    val minStatusRetryDuration: Float

    val randomStatusHour: Float

    val apiVersion: String

    val qrCodeDeletionHours: Float

    val qrCodeExpiredHours: Float

    val qrCodeFormattedString: String

    val qrCodeFormattedStringDisplayed: String

    val qrCodeFooterString: String

    val displayDepartmentLevel: Boolean

    val proximityReactivationReminderHours: List<Int>

    suspend fun refreshConfig(application: RobertApplication): RobertResult

    suspend fun generateCaptcha(type: String, local: String): RobertResultData<String>

    suspend fun getCaptchaImage(captchaId: String, path: String): RobertResult

    suspend fun getCaptchaAudio(captchaId: String, path: String): RobertResult

    suspend fun register(
        application: RobertApplication,
        captcha: String,
        captchaId: String
    ): RobertResult

    suspend fun activateProximity(
        application: RobertApplication,
        statusTried: Boolean = false
    ): RobertResult

    fun deactivateProximity(application: RobertApplication)

    suspend fun updateStatus(robertApplication: RobertApplication): RobertResult

    fun clearOldData()

    fun clearLocalData(application: RobertApplication)

    suspend fun report(
        token: String,
        firstSymptoms: Int?,
        positiveTest: Int?,
        application: RobertApplication
    ): RobertResult

    suspend fun storeLocalProximity(vararg localProximity: LocalProximity)

    fun getCurrentHelloBuilder(): RobertResultData<HelloBuilder>

    suspend fun eraseLocalHistory(): RobertResult

    suspend fun eraseRemoteExposureHistory(application: RobertApplication): RobertResult

    suspend fun eraseRemoteAlert(): RobertResult

    suspend fun quitStopCovid(application: RobertApplication): RobertResult

    fun refreshAtRisk()
}
