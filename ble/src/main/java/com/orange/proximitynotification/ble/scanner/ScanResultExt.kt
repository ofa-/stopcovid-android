/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. 
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble.scanner

import android.os.ParcelUuid
import com.orange.proximitynotification.tools.nanosTimestampToDate
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.UUID

internal fun ScanResult.toBleScannedDevice(serviceUuid: UUID): BleScannedDevice =
    BleScannedDevice(
        device = device,
        rssi = rssi,
        serviceData = scanRecord?.serviceData?.get(ParcelUuid(serviceUuid)),
        timestamp = timestampNanos.nanosTimestampToDate()
    )

internal fun List<ScanResult>.toBleScannedDevices(serviceUuid: UUID): List<BleScannedDevice> =
    map { it.toBleScannedDevice(serviceUuid) }
