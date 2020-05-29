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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class RecordProviderForScanWithPayloadTest {

    private val bleRecordProvider = RecordProviderForScanWithPayload()

    @Test
    fun fromScan_with_payload_should_return_new_record() {
        // Given
        val payload = payload()
        val device1Scan1 = bleScannedDevice(serviceData = payload.proximityPayload.data)

        // When
        val result = bleRecordProvider.fromScan(device1Scan1, payload)

        // Then
        val expected = record(payload, device1Scan1)
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun fromScan_with_payload_given_previous_scan_should_return_new_record() {
        // Given
        val payload = payload()
        val device1Scan1 = bleScannedDevice(serviceData = payload.proximityPayload.data)
        val device1Scan2 = device1Scan1.copy(timestamp = Date(device1Scan1.timestamp.time + 1000))

        givenScanAndPayload(device1Scan1, payload)

        // When
        val result = bleRecordProvider.fromScan(device1Scan2, payload)

        // Then
        val expected = record(payload, device1Scan2)
        assertThat(result).isEqualTo(expected)
    }

    private fun givenScanAndPayload(scannedDevices: BleScannedDevice, payload: BlePayload) {
        bleRecordProvider.fromScan(scannedDevices, payload)
    }

}
