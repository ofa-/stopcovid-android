/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/11/04 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import android.bluetooth.BluetoothDevice
import com.orange.proximitynotification.ProximityPayloadIdProvider
import com.orange.proximitynotification.ble.scanner.BleScannedDevice

/**
 * BLE Device address aka Device MAC address.
 * This address could change regularly on every BLE device scan.
 */
internal typealias BleDeviceAddress = String

/**
 * BLE Device Identity represents a reliable identity for a BLE Device.
 * For Android device it is extracted using [ProximityPayloadIdProvider] from [BleScannedDevice.serviceData].
 * For iOS it uses the hash of its [BleDeviceAddress] since MAC address doesn't change to frequently.
 */
internal typealias BleDeviceId = Int

internal fun BleScannedDevice.deviceAddress(): BleDeviceAddress = device.deviceAddress()
internal fun BluetoothDevice.deviceAddress(): BleDeviceAddress = address

/**
 * Returns the current [BleDeviceId] for a [BleScannedDevice]
 * If scan contains a payload, try to extract [ProximityPayloadId] from payload and compute hash on it
 * Otherwise compute a hash on device address
 *
 * @param payloadIdProvider [ProximityPayloadIdProvider]
 * @return [BleDeviceId]
 */
internal suspend fun BleScannedDevice.deviceId(payloadIdProvider: ProximityPayloadIdProvider): BleDeviceId =
    when (serviceData) {
        null -> deviceAddress().hashCode()
        else -> BlePayload.fromOrNull(serviceData)?.proximityPayload?.let {
            payloadIdProvider.fromProximityPayload(it)?.contentHashCode()
        } ?: serviceData.contentHashCode()
    }
