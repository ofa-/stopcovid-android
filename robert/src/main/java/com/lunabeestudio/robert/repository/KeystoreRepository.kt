/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.repository

import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource

internal class KeystoreRepository(
    private val keystoreDataSource: LocalKeystoreDataSource,
    private val robertManager: RobertManager,
) {
    var configuration: Configuration?
        get() = keystoreDataSource.configuration
        set(value) {
            keystoreDataSource.configuration = value
        }

    var reportPositiveTestDate: Long?
        get() = keystoreDataSource.reportPositiveTestDate
        set(value) {
            keystoreDataSource.reportPositiveTestDate = value
        }

    var reportSymptomsStartDate: Long?
        get() = keystoreDataSource.reportSymptomsStartDate
        set(value) {
            keystoreDataSource.reportSymptomsStartDate = value
        }

    var shouldReloadBleSettings: Boolean
        get() = keystoreDataSource.shouldReloadBleSettings ?: false
        set(value) {
            keystoreDataSource.shouldReloadBleSettings = value
        }

    var kA: ByteArray?
        get() = keystoreDataSource.kA
        set(value) {
            keystoreDataSource.kA = value
        }

    var kEA: ByteArray?
        get() = keystoreDataSource.kEA
        set(value) {
            keystoreDataSource.kEA = value
        }

    var timeStart: Long?
        get() = keystoreDataSource.timeStart
        set(value) {
            keystoreDataSource.timeStart = value
        }

    var lastRiskReceivedDate: Long?
        get() = keystoreDataSource.lastRiskReceivedDate
        set(value) {
            keystoreDataSource.lastRiskReceivedDate = value
            robertManager.refreshAtRisk()
        }

    var isWarningAtRisk: Boolean?
        get() = keystoreDataSource.isWarningAtRisk
        set(value) {
            keystoreDataSource.isWarningAtRisk = value
            robertManager.refreshAtRisk()
        }

    var lastWarningReceivedDate: Long?
        get() = keystoreDataSource.lastWarningReceivedDate
        set(value) {
            keystoreDataSource.lastWarningReceivedDate = value
            robertManager.refreshAtRisk()
        }

    var atRiskLastRefresh: Long?
        get() = keystoreDataSource.atRiskLastRefresh
        set(value) {
            keystoreDataSource.atRiskLastRefresh = value
            robertManager.refreshAtRisk()
        }

    var atRiskLastError: Long?
        get() = keystoreDataSource.atRiskLastError
        set(value) {
            keystoreDataSource.atRiskLastError = value
            robertManager.refreshAtRisk()
        }

    var lastExposureTimeframe: Int?
        get() = keystoreDataSource.lastExposureTimeframe
        set(value) {
            keystoreDataSource.lastExposureTimeframe = value
            robertManager.refreshAtRisk()
        }

    var proximityActive: Boolean?
        get() = keystoreDataSource.proximityActive
        set(value) {
            keystoreDataSource.proximityActive = value
        }

    var isSick: Boolean?
        get() = keystoreDataSource.isSick
        set(value) {
            keystoreDataSource.isSick = value
        }

    var reportDate: Long?
        get() = keystoreDataSource.reportDate
        set(value) {
            keystoreDataSource.reportDate = value
        }

    var reportValidationToken: String?
        get() = keystoreDataSource.reportValidationToken
        set(value) {
            keystoreDataSource.reportValidationToken = value
        }

    var reportToSendTime: Long?
        get() = keystoreDataSource.reportToSendTime
        set(value) {
            keystoreDataSource.reportToSendTime = value
        }
}