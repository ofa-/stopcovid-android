/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. 
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble

import android.bluetooth.BluetoothDevice
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import com.orange.proximitynotification.tools.ExpiringCache

internal class RecordProviderForScanWithoutPayload(
    settings: BleSettings,
    private val maxCacheSize: Int = 1000
) : BleRecordProvider() {

    internal val lastPayloadByDeviceId =
        ExpiringCache<DeviceId, BlePayload>(
            maxCacheSize,
            settings.cacheTimeout
        )
    internal val lastScanByDeviceId =
        ExpiringCache<DeviceId, BleScannedDevice>(
            maxCacheSize,
            settings.cacheTimeout
        )

    @Synchronized
    fun fromPayload(device: BluetoothDevice, payload: BlePayload): BleRecord? {
        cleanCacheIfNeeded()

        val deviceId = device.deviceId()
        lastPayloadByDeviceId.put(deviceId, payload)

        return lastScanByDeviceId[deviceId]?.let {
            buildRecord(payload, it)
        }
    }

    @Synchronized
    fun fromScan(scannedDevice: BleScannedDevice, payload: BlePayload?): BleRecord? {
        cleanCacheIfNeeded()

        val deviceId = scannedDevice.deviceId()
        lastScanByDeviceId.put(deviceId, scannedDevice)

        return (payload ?: lastPayloadByDeviceId[deviceId])
            ?.let { buildRecord(it, scannedDevice) }
    }

    private fun cleanCacheIfNeeded() {
        val cleanUpPredicate: (ExpiringCache<*, *>) -> Boolean =
            { it.size() >= (maxCacheSize * 0.75) }
        lastPayloadByDeviceId.takeIf(cleanUpPredicate)?.cleanUp()
        lastScanByDeviceId.takeIf(cleanUpPredicate)?.cleanUp()
    }

}