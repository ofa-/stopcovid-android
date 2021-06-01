/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/08 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import android.bluetooth.BluetoothDevice
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.orange.proximitynotification.ProximityInfo
import com.orange.proximitynotification.ProximityMetadata
import com.orange.proximitynotification.ProximityPayload
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import java.util.Date

internal fun proximityPayload() = ProximityPayload((1..16).map { it.toByte() }.toByteArray())
internal fun proximityPayload(value : Byte) = ProximityPayload(ByteArray(ProximityPayload.SIZE, init = { value }))

internal fun record(
    payload: BlePayload = payload(),
    scannedDevice: BleScannedDevice
) = record(payload = payload, rssi = scannedDevice.rssi, timestamp = scannedDevice.timestamp)

internal fun record(
    payload: BlePayload = payload(),
    rssi: Int = 0,
    timestamp: Date = Date(),
    isRssiCalibrated: Boolean = false
) = BleRecord(
    payload = payload,
    rssi = rssi,
    timestamp = timestamp,
    isRssiCalibrated = isRssiCalibrated
)

internal fun payload(
    proximityPayload: ProximityPayload = ProximityPayload(ByteArray(ProximityPayload.SIZE)),
    version: Int = 1,
    txPowerLevel: Int = 0,
    calibratedRssi: Int? = null
) = BlePayload(
    proximityPayload = proximityPayload,
    version = version,
    txPowerLevel = txPowerLevel,
    calibratedRssi = calibratedRssi
)

internal fun bleScannedDevice(
    device: BluetoothDevice = bluetoothDevice(),
    rssi: Int = 0,
    serviceData: ByteArray? = null,
    timestamp: Date = Date()
) = BleScannedDevice(
    device = device,
    rssi = rssi,
    serviceData = serviceData,
    timestamp = timestamp
)

internal fun bluetoothDevice(address: String = "address"): BluetoothDevice {
    val bluetoothDevice = mock<BluetoothDevice>()
    whenever(bluetoothDevice.address).thenReturn(address)
    return bluetoothDevice
}

internal fun bleProximityInfo(
    payload: ProximityPayload = proximityPayload(),
    timestamp: Date = Date(),
    metadata: ProximityMetadata = bleProximityMetadata()
) = ProximityInfo(
    payload = payload,
    timestamp = timestamp,
    metadata = metadata
)

internal fun bleProximityMetadata(
    rawRssi: Int = 0,
    calibratedRssi: Int = 0,
    txPowerLevel: Int = 0
) = BleProximityMetadata(
    rawRssi = rawRssi,
    calibratedRssi = calibratedRssi,
    txPowerLevel = txPowerLevel
)

internal fun Date.minus(millis: Long) = Date(time - millis)
internal fun Date.plus(millis: Long) = Date(time + millis)