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
import com.nhaarman.mockitokotlin2.mock
import com.orange.proximitynotification.ProximityPayload
import com.orange.proximitynotification.ProximityPayloadId
import com.orange.proximitynotification.ProximityPayloadIdProvider
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

class BleDevicePropertiesKtTest {

    companion object {

        val payloadIdProvider = object : ProximityPayloadIdProvider {
            override suspend fun fromProximityPayload(proximityPayload: ProximityPayload): ProximityPayloadId {
                return proximityPayload.data
            }
        }
    }

    @Test
    fun `deviceId given scan without serviceData and same address should be equal`() = runBlockingTest {
        // Given
        val scan1 = bleScannedDevice(bluetoothDevice("1"), serviceData = null)
        val scan2 = bleScannedDevice(bluetoothDevice("1"), serviceData = null)

        // When
        val deviceId1 = scan1.deviceId(payloadIdProvider)
        val deviceId2 = scan2.deviceId(payloadIdProvider)

        // Then
        assertThat(deviceId1).isEqualTo(deviceId2)
    }

    @Test
    fun `deviceId given scan without serviceData and different address should not be equal`() = runBlockingTest {
        // Given
        val scan1 = bleScannedDevice(bluetoothDevice("1"), serviceData = null)
        val scan2 = bleScannedDevice(bluetoothDevice("2"), serviceData = null)

        // When
        val deviceId1 = scan1.deviceId(payloadIdProvider)
        val deviceId2 = scan2.deviceId(payloadIdProvider)

        // Then
        assertThat(deviceId1).isNotEqualTo(deviceId2)
    }

    @Test
    fun `deviceId given scan with same serviceData and different address should be equal`() = runBlockingTest {
        // Given
        val payload = payload(proximityPayload(1)).toByteArray()
        val scan1 = bleScannedDevice(bluetoothDevice("1"), serviceData = payload)
        val scan2 = bleScannedDevice(bluetoothDevice("2"), serviceData = payload)

        // When
        val deviceId1 = scan1.deviceId(payloadIdProvider)
        val deviceId2 = scan2.deviceId(payloadIdProvider)

        // Then
        assertThat(deviceId1).isEqualTo(deviceId2)
    }

    @Test
    fun `deviceId given scan with different serviceData and different address should not be equal`() = runBlockingTest {
        // Given
        val scan1 = bleScannedDevice(bluetoothDevice("1"), serviceData = payload(proximityPayload(1)).toByteArray())
        val scan2 = bleScannedDevice(bluetoothDevice("2"), serviceData = payload(proximityPayload(2)).toByteArray())

        // When
        val deviceId1 = scan1.deviceId(payloadIdProvider)
        val deviceId2 = scan2.deviceId(payloadIdProvider)

        // Then
        assertThat(deviceId1).isNotEqualTo(deviceId2)
    }

    @Test
    fun `deviceId given no proximityPayloadId should use serviceData contentHashCode`() = runBlockingTest {
        // Given
        val payloadIdProvider : ProximityPayloadIdProvider = mock()
        val scan = bleScannedDevice(bluetoothDevice("2"), serviceData = payload(proximityPayload(2)).toByteArray())

        // When
        val deviceId = scan.deviceId(payloadIdProvider)

        // Then
        assertThat(deviceId).isEqualTo(scan.serviceData.contentHashCode())
    }
}