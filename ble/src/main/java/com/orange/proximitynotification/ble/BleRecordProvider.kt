/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/19 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble

import android.bluetooth.BluetoothDevice
import com.orange.proximitynotification.ble.scanner.BleScannedDevice

internal typealias DeviceId = String

internal abstract class BleRecordProvider {

    protected fun buildRecord(payload: BlePayload, scannedDevice: BleScannedDevice) = BleRecord(
        payload = payload,
        rssi = scannedDevice.rssi,
        timestamp = scannedDevice.timestamp
    )

    protected fun BleScannedDevice.deviceId(): DeviceId = device.deviceId()
    protected fun BluetoothDevice.deviceId(): DeviceId = address
}