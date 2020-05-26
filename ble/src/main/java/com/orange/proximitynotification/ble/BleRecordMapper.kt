/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/08 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble

import com.orange.proximitynotification.ProximityInfo
import com.orange.proximitynotification.ble.calibration.BleRssiCalibration

internal class BleRecordMapper(private val settings: BleSettings) {

    fun toProximityInfo(record: BleRecord) = ProximityInfo(
        payload = record.payload.proximityPayload,
        timestamp = record.timestamp,
        metadata = record.toProximityMetadata()
    )

    private fun BleRecord.toProximityMetadata() = BleProximityMetadata(
        rawRssi = rssi,
        calibratedRssi = BleRssiCalibration.calibrate(
            rssi = rssi,
            txCompensationGain = txPowerLevel,
            rxCompensationGain = settings.rxCompensationGain
        ),
        txPowerLevel = txPowerLevel
    )
}