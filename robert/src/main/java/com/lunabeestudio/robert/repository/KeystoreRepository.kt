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

import com.lunabeestudio.domain.model.DeviceParameterCorrection
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource

internal class KeystoreRepository(
    private val keystoreDataSource: LocalKeystoreDataSource,
    private val robertManager: RobertManager,
) {

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

    var atRiskMinHourContactNotif: Int?
        get() = keystoreDataSource.atRiskMinHourContactNotif
        set(value) {
            keystoreDataSource.atRiskMinHourContactNotif = value
        }

    var atRiskMaxHourContactNotif: Int?
        get() = keystoreDataSource.atRiskMaxHourContactNotif
        set(value) {
            keystoreDataSource.atRiskMaxHourContactNotif = value
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

    var calibration: List<DeviceParameterCorrection>?
        get() = keystoreDataSource.calibration
        set(value) {
            keystoreDataSource.calibration = value
        }

    var filteringConfig: String?
        get() = keystoreDataSource.filteringConfig
        set(value) {
            keystoreDataSource.filteringConfig = value
        }

    var filteringMode: String?
        get() = keystoreDataSource.filteringMode
        set(value) {
            keystoreDataSource.filteringMode = value
        }

    var serviceUUID: String?
        get() = keystoreDataSource.serviceUUID
        set(value) {
            keystoreDataSource.serviceUUID = value
        }

    var characteristicUUID: String?
        get() = keystoreDataSource.characteristicUUID
        set(value) {
            keystoreDataSource.characteristicUUID = value
        }

    var backgroundServiceManufacturerData: String?
        get() = keystoreDataSource.backgroundServiceManufacturerData
        set(value) {
            keystoreDataSource.backgroundServiceManufacturerData = value
        }

    var dataRetentionPeriod: Int?
        get() = keystoreDataSource.dataRetentionPeriod
        set(value) {
            keystoreDataSource.dataRetentionPeriod = value
        }

    var quarantinePeriod: Int?
        get() = keystoreDataSource.quarantinePeriod
        set(value) {
            keystoreDataSource.quarantinePeriod = value
            robertManager.refreshAtRisk()
        }

    var checkStatusFrequencyHour: Float?
        get() = keystoreDataSource.checkStatusFrequency
        set(value) {
            keystoreDataSource.checkStatusFrequency = value
        }

    var minStatusRetryDuration: Float?
        get() = keystoreDataSource.minStatusRetryDuraction
        set(value) {
            keystoreDataSource.minStatusRetryDuraction = value
        }

    var randomStatusHour: Float?
        get() = keystoreDataSource.randomStatusHour
        set(value) {
            keystoreDataSource.randomStatusHour = value
        }

    var preSymptomsSpan: Int?
        get() = keystoreDataSource.preSymptomsSpan
        set(value) {
            keystoreDataSource.preSymptomsSpan = value
        }

    var positiveSampleSpan: Int?
        get() = keystoreDataSource.positiveSampleSpan
        set(value) {
            keystoreDataSource.positiveSampleSpan = value
        }

    var appAvailability: Boolean?
        get() = keystoreDataSource.appAvailability
        set(value) {
            keystoreDataSource.appAvailability = value
        }

    var apiVersion: String?
        get() = keystoreDataSource.apiVersion
        set(value) {
            keystoreDataSource.apiVersion = value
        }

    var displayAttestation: Boolean?
        get() = keystoreDataSource.displayAttestation
        set(value) {
            keystoreDataSource.displayAttestation = value
        }

    var qrCodeDeletionHours: Float?
        get() = keystoreDataSource.qrCodeDeletionHours
        set(value) {
            keystoreDataSource.qrCodeDeletionHours = value
        }

    var qrCodeExpiredHours: Float?
        get() = keystoreDataSource.qrCodeExpiredHours
        set(value) {
            keystoreDataSource.qrCodeExpiredHours = value
        }

    var qrCodeFormattedString: String?
        get() = keystoreDataSource.qrCodeFormattedString
        set(value) {
            keystoreDataSource.qrCodeFormattedString = value
        }

    var qrCodeFormattedStringDisplayed: String?
        get() = keystoreDataSource.qrCodeFormattedStringDisplayed
        set(value) {
            keystoreDataSource.qrCodeFormattedStringDisplayed = value
        }

    var qrCodeFooterString: String?
        get() = keystoreDataSource.qrCodeFooterString
        set(value) {
            keystoreDataSource.qrCodeFooterString = value
        }

    var configVersion: Int?
        get() = keystoreDataSource.configVersion
        set(value) {
            keystoreDataSource.configVersion = value
        }

    var displayDepartmentLevel: Boolean?
        get() = keystoreDataSource.displayDepartmentLevel
        set(value) {
            keystoreDataSource.displayDepartmentLevel = value
        }

    var proximityReactivationReminderHours: List<Int>?
        get() = keystoreDataSource.proximityReactivationReminderHours
        set(value) {
            keystoreDataSource.proximityReactivationReminderHours = value
        }

    var reportDate: Long?
        get() = keystoreDataSource.reportDate
        set(value) {
            keystoreDataSource.reportDate = value
        }
}