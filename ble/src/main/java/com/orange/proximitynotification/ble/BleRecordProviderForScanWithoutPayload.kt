/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. 
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import android.bluetooth.BluetoothDevice
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import com.orange.proximitynotification.tools.ExpiringCache

internal class BleRecordProviderForScanWithoutPayload(
    private val maxCacheSize: Int,
    scanCacheTimeout: Long,
    payloadCacheTimeout : Long
) : BleRecordProvider() {

    internal val lastPayloadByDeviceAddress =
        ExpiringCache<BleDeviceAddress, BlePayload>(maxCacheSize, payloadCacheTimeout)
    internal val lastScanByDeviceAddress =
        ExpiringCache<BleDeviceAddress, BleScannedDevice>(maxCacheSize, scanCacheTimeout)

    @Synchronized
    fun fromPayload(device: BluetoothDevice, payload: BlePayload): BleRecord? {
        cleanCacheIfNeeded()

        val deviceAddress = device.deviceAddress()
        lastPayloadByDeviceAddress.put(deviceAddress, payload)

        return lastScanByDeviceAddress[deviceAddress]?.let { buildRecord(payload, it) }
    }

    @Synchronized
    fun fromScan(scannedDevice: BleScannedDevice): BleRecord? {
        cleanCacheIfNeeded()

        val deviceAddress = scannedDevice.deviceAddress()
        lastScanByDeviceAddress.put(deviceAddress, scannedDevice)

        return lastPayloadByDeviceAddress[deviceAddress]?.let { buildRecord(it, scannedDevice) }
    }

    @Synchronized
    fun fromScanAndPayload(
        scannedDevice: BleScannedDevice,
        payload: BlePayload
    ): BleRecord {
        cleanCacheIfNeeded()

        val deviceAddress = scannedDevice.deviceAddress()
        lastPayloadByDeviceAddress.put(deviceAddress, payload)
        lastScanByDeviceAddress.put(deviceAddress, scannedDevice)

        return buildRecord(payload, scannedDevice)
    }

    private fun cleanCacheIfNeeded() {
        val cleanUpPredicate: (ExpiringCache<*, *>) -> Boolean =
            { it.size() >= (maxCacheSize * 0.75) }
        lastPayloadByDeviceAddress.takeIf(cleanUpPredicate)?.cleanUp()
        lastScanByDeviceAddress.takeIf(cleanUpPredicate)?.cleanUp()
    }
}