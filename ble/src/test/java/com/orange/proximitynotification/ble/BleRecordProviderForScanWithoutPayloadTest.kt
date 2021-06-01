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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(AndroidJUnit4::class)
class BleRecordProviderForScanWithoutPayloadTest {

    companion object {
        private const val CACHE_TIMEOUT = 500L
        private const val MAX_CACHE_SIZE = 4
    }

    private val bleRecordProvider = BleRecordProviderForScanWithoutPayload(
        maxCacheSize = MAX_CACHE_SIZE,
        scanCacheTimeout = CACHE_TIMEOUT,
        payloadCacheTimeout = CACHE_TIMEOUT
    )

    @Test
    fun fromScan_without_payload_should_return_null() {
        // Given
        val device1Scan1 = bleScannedDevice(serviceData = null)

        // When
        val result = bleRecordProvider.fromScan(device1Scan1)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun fromScan_without_payload_given_existing_payload_should_merge_and_return_new_record() {
        // Given
        val payload = payload()
        val device = bluetoothDevice()
        val device1Scan1 = bleScannedDevice(device = device, serviceData = null)

        givenPayload(device, payload)

        // When
        val result = bleRecordProvider.fromScan(device1Scan1)

        // Then
        val expected = record(payload, device1Scan1)
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun fromScan_given_cache_having_scanned_device_should_return_record_with_new_scan() {
        // Given
        val payload = payload()
        val device = bluetoothDevice()
        val device1Scan1 = bleScannedDevice(device = device, rssi = 1, serviceData = null)

        givenCachedScannedDevices(device1Scan1)
        givenPayload(device, payload)

        // When
        val device1Scan2 = bleScannedDevice(device = device, rssi = 2, serviceData = null)
        val result = bleRecordProvider.fromScan(device1Scan2)

        // Then
        val expected = record(payload, device1Scan2)
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun fromPayload_without_previous_scan_should_return_null() {
        // Given
        val device = bluetoothDevice()
        val payload = payload()

        // When
        val result = bleRecordProvider.fromPayload(device, payload)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun fromPayload_given_scan_should_create_a_new_record() {
        // Given
        val device = bluetoothDevice()
        val payload = payload()
        val device1Scan1 = bleScannedDevice(device = device, serviceData = null)

        givenScanAndNoPayload(device1Scan1)

        // When
        val result = bleRecordProvider.fromPayload(device, payload)

        // Then
        val expected = record(payload, device1Scan1)
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun fromPayload_waiting_for_cache_to_expire_should_return_null() {
        // Given
        val payload = payload()
        val device1 = bluetoothDevice()
        val device1scan1 =
            bleScannedDevice(
                device = device1,
                rssi = 1,
                serviceData = payload.proximityPayload.data
            )

        givenCachedScannedDevices(device1scan1)
        givenPayload(device1, payload)

        // When
        waitForCacheToExpire()
        val result = bleRecordProvider.fromPayload(device1, payload)

        // Then
        assertThat(result).isNull()
        assertThat(bleRecordProvider.lastScanByDeviceAddress[device1.address]).isNull()
    }

    @Test
    fun fromPayload_should_cleanup_cache_if_size_it_above_75_percent() {
        // Given
        val payload = payload()
        givenScanAndNoPayload(bleScannedDevice(device = bluetoothDevice("device1")))
        givenScanAndNoPayload(bleScannedDevice(device = bluetoothDevice("device2")))
        givenScanAndNoPayload(bleScannedDevice(device = bluetoothDevice("device3")))

        // When
        advanceCacheBy(CACHE_TIMEOUT - 100)
        val device4Scan1 = bleScannedDevice(device = bluetoothDevice("device4"))
        givenScanAndNoPayload(device4Scan1)
        advanceCacheBy(100)

        assertThat(bleRecordProvider.lastScanByDeviceAddress.size()).isEqualTo(4)
        val result = bleRecordProvider.fromPayload(device4Scan1.device, payload = payload)

        // Then
        val expected = record(payload, device4Scan1)
        assertThat(result).isEqualTo(expected)
        assertThat(bleRecordProvider.lastScanByDeviceAddress.size()).isEqualTo(1)
    }

    @Test
    fun fromPayload_should_not_cleanup_cache_if_size_it_below75_percent() {
        // Given
        val payload = payload()
        givenScanAndNoPayload(bleScannedDevice(device = bluetoothDevice("device1")))

        // When
        advanceCacheBy(CACHE_TIMEOUT - 100)
        val device2Scan1 = bleScannedDevice(device = bluetoothDevice("device2"))
        givenScanAndNoPayload(device2Scan1)
        advanceCacheBy(100)

        assertThat(bleRecordProvider.lastScanByDeviceAddress.size()).isEqualTo(2)
        val result = bleRecordProvider.fromPayload(device2Scan1.device, payload = payload)

        // Then
        val expected = record(payload, device2Scan1)
        assertThat(result).isEqualTo(expected)
        assertThat(bleRecordProvider.lastScanByDeviceAddress.size()).isEqualTo(2)
    }

    @Test
    fun fromScan_should_cleanup_cache_if_size_it_above_75_percent() {
        // Given
        val payload = payload()
        givenPayload(bluetoothDevice("device1"), payload)
        givenPayload(bluetoothDevice("device2"), payload)
        givenPayload(bluetoothDevice("device3"), payload)

        // When
        advanceCacheBy(CACHE_TIMEOUT - 100)
        val device4 = bluetoothDevice("device4")
        givenPayload(device4, payload)
        advanceCacheBy(100)

        assertThat(bleRecordProvider.lastPayloadByDeviceAddress.size()).isEqualTo(4)
        val device4Scan1 = bleScannedDevice(device4, serviceData = null)
        val result = bleRecordProvider.fromScan(device4Scan1)

        // Then
        val expected = record(payload, device4Scan1)
        assertThat(result).isEqualTo(expected)
        assertThat(bleRecordProvider.lastPayloadByDeviceAddress.size()).isEqualTo(1)
    }

    @Test
    fun fromScan_should_not_cleanup_cache_if_size_it_below_75_percent() {
        // Given
        val payload = payload()
        givenPayload(bluetoothDevice("device1"), payload)

        // When
        advanceCacheBy(CACHE_TIMEOUT - 100)
        val device2 = bluetoothDevice("device2")
        givenPayload(device2, payload)
        advanceCacheBy(100)

        assertThat(bleRecordProvider.lastPayloadByDeviceAddress.size()).isEqualTo(2)
        val device2Scan1 = bleScannedDevice(device2, serviceData = null)
        val result = bleRecordProvider.fromScan(device2Scan1)

        // Then
        val expected = record(payload, device2Scan1)
        assertThat(result).isEqualTo(expected)
        assertThat(bleRecordProvider.lastPayloadByDeviceAddress.size()).isEqualTo(2)
    }

    @Test
    fun fromScanAndPayload_should_store_scan_and_payload() {
        // Given
        val device = bluetoothDevice("device1")
        val payload = payload()
        val deviceScan = bleScannedDevice(device = device, rssi = 1, serviceData = null)

        // When
        val result = bleRecordProvider.fromScanAndPayload(deviceScan, payload)

        // Then
        val expected = record(payload, deviceScan)
        assertThat(result).isEqualTo(expected)

        assertThat(bleRecordProvider.fromPayload(device, payload)).isEqualTo(expected)
        assertThat(bleRecordProvider.fromScan(deviceScan)).isEqualTo(expected)
    }

    private fun givenScanAndNoPayload(scannedDevice: BleScannedDevice) {
        bleRecordProvider.fromScan(scannedDevice)
    }

    private fun givenPayload(device: BluetoothDevice, payload: BlePayload) {
        bleRecordProvider.fromPayload(device, payload)
    }

    private fun givenCachedScannedDevices(vararg scannedDevices: BleScannedDevice) {
        scannedDevices.forEach {
            bleRecordProvider.lastScanByDeviceAddress.put(it.device.address, it)
        }
    }

    private fun waitForCacheToExpire() {
        val waitTimeout = CACHE_TIMEOUT + 100
        advanceCacheBy(waitTimeout)
    }

    private fun advanceCacheBy(waitTimeout: Long) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(waitTimeout))
        Thread.sleep(waitTimeout)
    }
}
