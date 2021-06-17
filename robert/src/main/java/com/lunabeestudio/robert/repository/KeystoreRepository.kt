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

import com.lunabeestudio.domain.model.AtRiskStatus
import com.lunabeestudio.domain.model.Calibration
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

    var calibration: Calibration?
        get() = keystoreDataSource.calibration
        set(value) {
            keystoreDataSource.calibration = value
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

    var isRegistered: Boolean
        get() = keystoreDataSource.isRegistered
        set(value) {
            keystoreDataSource.isRegistered = value
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

    var atRiskStatus: AtRiskStatus?
        get() = keystoreDataSource.atRiskStatus
        set(value) {
            keystoreDataSource.atRiskStatus = value
            robertManager.refreshAtRisk()
        }

    var currentRobertAtRiskStatus: AtRiskStatus?
        get() = keystoreDataSource.currentRobertAtRiskStatus
        set(value) {
            keystoreDataSource.currentRobertAtRiskStatus = value
            robertManager.refreshAtRisk()
        }

    var currentWarningAtRiskStatus: AtRiskStatus?
        get() = keystoreDataSource.currentWarningAtRiskStatus
        set(value) {
            keystoreDataSource.currentWarningAtRiskStatus = value
            robertManager.refreshAtRisk()
        }

    var cleaLastStatusIteration: Int?
        get() = keystoreDataSource.cleaLastStatusIteration
        set(value) {
            keystoreDataSource.cleaLastStatusIteration = value
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

    var atRiskModelVersion: Int?
        get() = keystoreDataSource.atRiskModelVersion
        set(value) {
            keystoreDataSource.atRiskModelVersion = value
        }

    var deprecatedLastRiskReceivedDate: Long?
        get() = keystoreDataSource.deprecatedLastRiskReceivedDate
        set(value) {
            keystoreDataSource.deprecatedLastRiskReceivedDate = value
            robertManager.refreshAtRisk()
        }

    var deprecatedLastExposureTimeframe: Int?
        get() = keystoreDataSource.deprecatedLastExposureTimeframe
        set(value) {
            keystoreDataSource.deprecatedLastExposureTimeframe = value
            robertManager.refreshAtRisk()
        }

    var proximityActive: Boolean?
        get() = keystoreDataSource.proximityActive
        set(value) {
            keystoreDataSource.proximityActive = value
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

    var reportToSendStartTime: Long?
        get() = keystoreDataSource.reportToSendStartTime
        set(value) {
            keystoreDataSource.reportToSendStartTime = value
        }

    var reportToSendEndTime: Long?
        get() = keystoreDataSource.reportToSendEndTime
        set(value) {
            keystoreDataSource.reportToSendEndTime = value
        }

    var declarationToken: String?
        get() = keystoreDataSource.declarationToken
        set(value) {
            keystoreDataSource.declarationToken = value
        }
}