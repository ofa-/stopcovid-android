/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2021/03/31 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import com.google.common.truth.Truth.assertThat
import com.googlecode.zohhak.api.TestWith
import com.googlecode.zohhak.api.runners.ZohhakRunner
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(ZohhakRunner::class)
class BleRecordProviderTest {

    private val bleRecordProvider = BleRecordProvider()

    @Test
    fun buildRecord_with_payload_and_scan_should_return_expected_record() {
        // Given
        val payload = payload()
        val deviceScan = bleScannedDevice(serviceData = payload.proximityPayload.data)

        // When
        val result = bleRecordProvider.buildRecord(payload, deviceScan)

        // Then
        val expected = record(
            payload = payload,
            rssi = deviceScan.rssi,
            timestamp = deviceScan.timestamp,
            isRssiCalibrated = false
        )
        assertThat(result).isEqualTo(expected)
    }

    @TestWith(value = [
        "-60, true",
        "-50, true",
        "-60, false"
    ])
    fun buildRecord_with_manual_parameters_should_return_expected_record(rssi : Int, isRssiCalibrated : Boolean) {
        // Given
        val payload = payload()
        val timestamp = Date()

        // When
        val result = bleRecordProvider.buildRecord(payload,
            rssi = rssi,
            timestamp = timestamp,
            isRssiCalibrated = isRssiCalibrated)

        // Then
        val expected = record(
            payload = payload,
            rssi = rssi,
            timestamp = timestamp,
            isRssiCalibrated = isRssiCalibrated
        )
        assertThat(result).isEqualTo(expected)
    }

}
