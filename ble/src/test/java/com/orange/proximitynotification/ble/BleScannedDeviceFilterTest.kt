/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/06/30 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import com.google.common.truth.Truth.assertThat
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import org.junit.Test
import java.util.Date

class BleScannedDeviceFilterTest {

    @Test
    fun filter_with_empty_scanned_devices_should_return_empty() {

        // Given
        val filter = BleScannedDeviceFilter()
        val scans: List<BleScannedDevice> = emptyList()

        // When
        val result = filter.filter(scans)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun filter_with_old_scanned_devices_should_return_empty() {

        // Given
        val now = Date()
        val filter = BleScannedDeviceFilter(now)

        val device1 = bluetoothDevice("Device1")
        val device2 = bluetoothDevice("Device2")

        val scans: List<BleScannedDevice> = listOf(
            bleScannedDevice(device1, timestamp = now.minus(1), serviceData = null),
            bleScannedDevice(device2, timestamp = now, serviceData = null)
        )

        // When
        val result = filter.filter(scans)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun filter_with_different_scanned_devices_should_return_same_scanned_devices() {

        // Given
        val now = Date()
        val filter = BleScannedDeviceFilter(now.minus(1))

        val device1 = bluetoothDevice("Device1")
        val device2 = bluetoothDevice("Device2")
        val device3 = bluetoothDevice("Device3")
        val device4 = bluetoothDevice("Device4")

        val scans: List<BleScannedDevice> = listOf(
            bleScannedDevice(device1, timestamp = now, serviceData = null),
            bleScannedDevice(device2, timestamp = now, serviceData = null),
            bleScannedDevice(device3, timestamp = now, serviceData = byteArrayOf(1)),
            bleScannedDevice(device4, timestamp = now, serviceData = byteArrayOf(2))
        )

        // When
        val result = filter.filter(scans)

        // Then
        assertThat(result).isEqualTo(scans)
    }

    @Test
    fun filter_with_different_scanned_devices_having_same_service_data_should_squash() {

        // Given
        val now = Date()
        val filter = BleScannedDeviceFilter(now.minus(1))

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
    fun filter_with_same_scanned_devices_should_keep_most_recent_scans_and_order_them_by_timestamp() {

        // Given
        val now = Date()
        val filter = BleScannedDeviceFilter(now)

        val device1 = bluetoothDevice("Device1")
        val device2 = bluetoothDevice("Device2")
        val serviceData = byteArrayOf(1)

        val scans: List<BleScannedDevice> = listOf(
            bleScannedDevice(device = device2, serviceData = null, timestamp = now.plus(5)),
            bleScannedDevice(device = device2, serviceData = null, timestamp = now.plus(4)),
            bleScannedDevice(device = device1, serviceData = serviceData, timestamp = now.plus(3)),
            bleScannedDevice(device = device1, serviceData = serviceData, timestamp = now.plus(2)),
            bleScannedDevice(device = device1, serviceData = serviceData, timestamp = now.plus(1))
        )

        // When
        val result = filter.filter(scans)

        // Then
        assertThat(result).containsExactly(
            bleScannedDevice(device = device1, serviceData = serviceData, timestamp = now.plus(3)),
            bleScannedDevice(device = device2, serviceData = null, timestamp = now.plus(5))
        )
    }

    @Test
    fun filter_with_old_scans_should_only_keep_most_recent_ones() {

        // Given
        val now = Date()
        val filter = BleScannedDeviceFilter(now)

        val device1 = bluetoothDevice("Device1")
        val device2 = bluetoothDevice("Device2")
        val device3 = bluetoothDevice("Device3")

        val scansBefore: List<BleScannedDevice> = listOf(
            bleScannedDevice(device1, timestamp = now.plus(1_000), serviceData = null),
            bleScannedDevice(device2, timestamp = now.plus(1_001), serviceData = null)
        )
        filter.filter(scansBefore)

        val scans: List<BleScannedDevice> = listOf(
            bleScannedDevice(device1, timestamp = now.plus(1_000), serviceData = null),
            bleScannedDevice(device2, timestamp = now.plus(1_001), serviceData = null),
            bleScannedDevice(device3, timestamp = now.plus(1_002), serviceData = null)
        )

        // When
        val result = filter.filter(scans)

        // Then
        assertThat(result).containsExactly(
            bleScannedDevice(device3, timestamp = now.plus(1_002), serviceData = null)
        )
    }
}