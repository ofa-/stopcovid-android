/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/19 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import java.util.Date

internal open class BleRecordProvider {

    fun buildRecord(payload: BlePayload, scannedDevice: BleScannedDevice) = buildRecord(
        payload = payload,
        rssi = scannedDevice.rssi,
        timestamp = scannedDevice.timestamp,
        isRssiCalibrated = false
    )

    fun buildRecord(
        payload: BlePayload,
        rssi: Int,
        timestamp: Date,
        isRssiCalibrated: Boolean
    ) =
        BleRecord(
            payload = payload,
            rssi = rssi,
            timestamp = timestamp,
            isRssiCalibrated = isRssiCalibrated
        )
}