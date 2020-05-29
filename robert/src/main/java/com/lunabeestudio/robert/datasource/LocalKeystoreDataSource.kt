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

import com.lunabeestudio.domain.model.DeviceParameterCorrection

interface LocalKeystoreDataSource {
    var kA: ByteArray?
    var kEA: ByteArray?
    var timeStart: Long?
    var atRisk: Boolean?
    var atRiskLastRefresh: Long?
    var atRiskMinHourContactNotif: Int?
    var atRiskMaxHourContactNotif: Int?
    var lastExposureTimeframe: Int?
    var proximityActive: Boolean?
    var isSick: Boolean?
    var calibration: List<DeviceParameterCorrection>?
    var serviceUUID: String?
    var characteristicUUID: String?
    var backgroundServiceManufacturerData: String?
    var dataRetentionPeriod: Int?
    var quarantinePeriod: Int?
    var checkStatusFrequency: Int?
    var randomStatusHour: Int?
    var preSymptomsSpan: Int?
    var appAvailability: Boolean?
}
