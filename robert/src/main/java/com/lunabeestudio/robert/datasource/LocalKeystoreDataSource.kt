/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.datasource

import androidx.lifecycle.LiveData
import com.lunabeestudio.domain.model.DeviceParameterCorrection
import com.lunabeestudio.domain.model.FormEntry

interface LocalKeystoreDataSource {
    var shouldReloadBleSettings: Boolean?
    var kA: ByteArray?
    var kEA: ByteArray?
    var timeStart: Long?
    var atRiskLastRefresh: Long?
    var atRiskLastError: Long?
    var atRiskMinHourContactNotif: Int?
    var atRiskMaxHourContactNotif: Int?
    var lastExposureTimeframe: Int?
    var proximityActive: Boolean?
    var isSick: Boolean?
    var calibration: List<DeviceParameterCorrection>?
    var filteringConfig: String?
    var filteringMode: String?
    var serviceUUID: String?
    var characteristicUUID: String?
    var backgroundServiceManufacturerData: String?
    var dataRetentionPeriod: Int?
    var quarantinePeriod: Int?
    var checkStatusFrequency: Float?
    var minStatusRetryDuraction: Float?
    var randomStatusHour: Float?
    var preSymptomsSpan: Int?
    var positiveSampleSpan: Int?
    var appAvailability: Boolean?
    var apiVersion: String?
    var configVersion: Int?
    var lastRiskReceivedDate: Long?
    var qrCodeDeletionHours: Float?
    var qrCodeExpiredHours: Float?
    var qrCodeFormattedString: String?
    var qrCodeFormattedStringDisplayed: String?
    var qrCodeFooterString: String?
    var displayDepartmentLevel: Boolean?
    var proximityReactivationReminderHours: List<Int>?
    val attestationsLiveData: LiveData<List<Map<String, FormEntry>>?>
    var attestations: List<Map<String, FormEntry>>?
    var savedAttestationData: Map<String, FormEntry>?
    var saveAttestationData: Boolean?
}
