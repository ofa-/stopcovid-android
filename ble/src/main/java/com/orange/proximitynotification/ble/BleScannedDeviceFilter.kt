/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/06/30 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble

import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import java.util.Date

internal class BleScannedDeviceFilter {

    private var mostRecentScanTimestamp = Date(0)

    fun filter(scannedDevices: List<BleScannedDevice>): List<BleScannedDevice> {

        val results = scannedDevices.sortedByDescending { it.timestamp }
            .distinctBy {
                it.serviceData?.contentHashCode() ?: run { it.device.address }
            }
            .filter { it.timestamp > mostRecentScanTimestamp }

        results.firstOrNull()?.let { this.mostRecentScanTimestamp = it.timestamp }
        return results
    }
}