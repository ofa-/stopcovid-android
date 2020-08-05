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

import com.google.common.truth.Truth.assertThat
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import org.junit.Test
import java.util.Date

class BleScannedDeviceFilterTest {

    private val filter = BleScannedDeviceFilter()

    @Test
    fun filter_with_empty_should_return_empty() {

        // Given
        val scans: List<BleScannedDevice> = emptyList()

        // When
        val result = filter.filter(scans)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun filter_with_different_device_scans_should_return_same_device_scans() {

        // Given
        val now = Date()
        val scans: List<BleScannedDevice> = listOf(
            bleScannedDevice(bluetoothDevice("Device1"), timestamp = now, serviceData = null),
            bleScannedDevice(bluetoothDevice("Device2"), timestamp = now, serviceData = null),
            bleScannedDevice(bluetoothDevice("Device3"), timestamp = now, serviceData = byteArrayOf(1)),
            bleScannedDevice(bluetoothDevice("Device4"), timestamp = now, serviceData = byteArrayOf(2))
        )

        // When
        val result = filter.filter(scans)

        // Then
        assertThat(result).isEqualTo(scans)
    }

    @Test
    fun filter_with_different_device_scans_but_same_service_data_should_squash() {

        // Given
        val now = Date()

        val device1 = bluetoothDevice("Device1")
        val device2 = bluetoothDevice("Device2")
        val device3 = bluetoothDevice("Device3")
        val device4 = bluetoothDevice("Device4")

        val scans: List<BleScannedDevice> = listOf(
            bleScannedDevice(device1, timestamp = now, serviceData = null),
            bleScannedDevice(device2, timestamp = now, serviceData = null),
            bleScannedDevice(device3, timestamp = now.minus(2), serviceData = byteArrayOf(1)),
            bleScannedDevice(device4, timestamp = now, serviceData = byteArrayOf(1))
        )

        // When
        val result = filter.filter(scans)

        // Then
        assertThat(result).containsExactly(
            bleScannedDevice(device1, timestamp = now, serviceData = null),
            bleScannedDevice(device2, timestamp = now, serviceData = null),
            bleScannedDevice(device4, timestamp = now, serviceData = byteArrayOf(1))
        )
    }

    @Test
    fun filter_with_same_devices_scans_should_keep_most_recent_scans_and_order_them_by_timestamp() {

        // Given
        val device1 = bluetoothDevice("Device1")
        val device2 = bluetoothDevice("Device2")
        val serviceData = byteArrayOf(1)
        val now = Date()

        val scans: List<BleScannedDevice> = listOf(
            bleScannedDevice(device = device2, serviceData = null, timestamp = now.minus(5)),
            bleScannedDevice(device = device2, serviceData = null, timestamp = now.minus(4)),
            bleScannedDevice(device = device1, serviceData = serviceData, timestamp = now.minus(3)),
            bleScannedDevice(device = device1, serviceData = serviceData, timestamp = now.minus(2)),
            bleScannedDevice(device = device1, serviceData = serviceData, timestamp = now.minus(1))
        )

        // When
        val result = filter.filter(scans)

        // Then
        assertThat(result).containsExactly(
            bleScannedDevice(device = device1, serviceData = serviceData, timestamp = now.minus(1)),
            bleScannedDevice(device = device2, serviceData = null, timestamp = now.minus(4))
        )
    }

    @Test
    fun filter_with_old_scans_should_only_keep_most_recent_ones() {

        // Given
        val now = Date()

        val scansBefore: List<BleScannedDevice> = listOf(
            bleScannedDevice(bluetoothDevice("Device-skipped-1"), timestamp = now.minus(1_001), serviceData = null),
            bleScannedDevice(bluetoothDevice("Device-skipped-2"), timestamp = now.minus(1_000), serviceData = null)
        )
        filter.filter(scansBefore)

        val device1 = bluetoothDevice("Device1")
        val device2 = bluetoothDevice("Device2")
        val device3 = bluetoothDevice("Device3")
        val scans: List<BleScannedDevice> = listOf(
            bleScannedDevice(device1, timestamp = now.minus(1_001), serviceData = null),
            bleScannedDevice(device2, timestamp = now.minus(1_000), serviceData = null),
            bleScannedDevice(device3, timestamp = now.minus(999), serviceData = null)
        )

        // When
        val result = filter.filter(scans)

        // Then
        assertThat(result).containsExactly(
            bleScannedDevice(device3, timestamp = now.minus(999), serviceData = null)
        )
    }

    private fun Date.minus(millis: Long) = Date(time - millis)

}