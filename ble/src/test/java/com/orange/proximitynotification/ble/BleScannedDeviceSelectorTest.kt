/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/06/16 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import android.bluetooth.BluetoothDevice
import com.google.common.truth.Truth.assertThat
import com.orange.proximitynotification.ProximityPayload
import com.orange.proximitynotification.ProximityPayloadId
import com.orange.proximitynotification.ProximityPayloadIdProvider
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.util.Date

class BleScannedDeviceSelectorTest {

    @Test
    fun select_given_no_scans_should_return_empty() = runBlockingTest {

        // Given
        val scannedDeviceSelector = givenScannedDeviceSelector().withoutScans()

        // When
        assertThat(scannedDeviceSelector.isEmpty()).isTrue()
        val results = scannedDeviceSelector.select()

        // Then
        assertThat(results).isEmpty()
        assertThat(scannedDeviceSelector.isEmpty()).isTrue()
    }

    @Test
    fun select_given_multiple_devices_having_same_rssi_bracket_and_where_timestamp_is_important_should_select_most_recent_ones() =
        runBlockingTest {

            // Given
            val timestamp = Date(0)

            val scannedDeviceSelector = givenScannedDeviceSelector(timestampIsImportant = true)
                .withScans(
                    bleScannedDevice(device(1), -8, byteArrayOf(1), timestamp.plus(0)),
                    bleScannedDevice(device(2), -10, byteArrayOf(2), timestamp.plus(200)),
                )

            // When
            assertThat(scannedDeviceSelector.isEmpty()).isFalse()
            val results = scannedDeviceSelector.select()

            // Then
            assertThat(results).hasSize(2)
            assertThat(results).containsExactly(
                bleScannedDevice(device(2), -10, byteArrayOf(2), timestamp.plus(200)),
                bleScannedDevice(device(1), -8, byteArrayOf(1), timestamp.plus(0))
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()
        }

    @Test
    fun select_given_multiple_devices_having_same_rssi_bracket_and_where_timestamp_is_not_important_should_select_ones_with_best_rssis() =
        runBlockingTest {

            // Given
            val timestamp = Date(0)

            val scannedDeviceSelector = givenScannedDeviceSelector(timestampIsImportant = false)
                .withScans(
                    bleScannedDevice(device(1), -8, byteArrayOf(1), timestamp.plus(0)),
                    bleScannedDevice(device(2), -10, byteArrayOf(2), timestamp.plus(200)),
                )

            // When
            assertThat(scannedDeviceSelector.isEmpty()).isFalse()
            val results = scannedDeviceSelector.select()

            // Then
            assertThat(results).hasSize(2)
            assertThat(results).containsExactly(
                bleScannedDevice(device(1), -8, byteArrayOf(1), timestamp.plus(0)),
                bleScannedDevice(device(2), -10, byteArrayOf(2), timestamp.plus(200))
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()
        }

    @Test
    fun select_given_multiple_devices_having_same_timestamp_bracket_should_select_best_ones_using_rssi_average_and_scans_count() =
        runBlockingTest {

            // Given
            val timestamp = Date(0)

            val scannedDeviceSelector = givenScannedDeviceSelector().withScans(
                bleScannedDevice(device(1), -20, null, timestamp.plus(0)),
                bleScannedDevice(device(2), -10, null, timestamp.plus(1)),
                bleScannedDevice(device(3), -20, null, timestamp.plus(99)),
                bleScannedDevice(device(4), -30, null, timestamp.plus(199)),
            )

            // When
            assertThat(scannedDeviceSelector.isEmpty()).isFalse()
            val results = scannedDeviceSelector.select()

            // Then
            assertThat(results).hasSize(4)
            assertThat(results).containsExactly(
                bleScannedDevice(device(2), -10, null, timestamp.plus(1)),
                bleScannedDevice(device(3), -20, null, timestamp.plus(99)),
                bleScannedDevice(device(1), -20, null, timestamp.plus(0)),
                bleScannedDevice(device(4), -30, null, timestamp.plus(199)),
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()
        }

    @Test
    fun select_given_multiple_devices_having_same_timestamp_bracket_and_almost_same_rssi_should_select_best_ones_using_scans_count() =
        runBlockingTest {

            // Given
            val t = Date(0)

            val scannedDeviceSelector = givenScannedDeviceSelector().withScans(
                bleScannedDevice(device(1), -10, null, t.plus(0)),
                bleScannedDevice(device(2), -11, null, t.plus(1)),
                bleScannedDevice(device(2), -10, null, t.plus(2)),
                bleScannedDevice(device(3), -10, null, t.plus(3)),
                bleScannedDevice(device(3), -10, null, t.plus(4)),
                bleScannedDevice(device(3), -11, null, t.plus(5)),
            )

            // When
            assertThat(scannedDeviceSelector.isEmpty()).isFalse()
            val results = scannedDeviceSelector.select()

            // Then
            assertThat(results).hasSize(3)
            assertThat(results).containsExactly(
                bleScannedDevice(device(3), -11, null, t.plus(5)),
                bleScannedDevice(device(2), -10, null, t.plus(2)),
                bleScannedDevice(device(1), -10, null, t.plus(0)),
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()
        }


    @Test
    fun select_given_multiple_devices_with_same_timestamp_should_select_best_ones_using_rssi_average_and_scans_count() =
        runBlockingTest {

            // Given
            val now = Date()

            val scannedDeviceSelector = givenScannedDeviceSelector().withScans(
                bleScannedDevice(device(1), rssi = -60, byteArrayOf(1), now),
                bleScannedDevice(device(2), rssi = -30, null, now),
                bleScannedDevice(device(3), rssi = -20, byteArrayOf(3), now),
                bleScannedDevice(device(4), rssi = -40, byteArrayOf(4), now),
                bleScannedDevice(device(5), rssi = -50, null, now),
                bleScannedDevice(device(1), rssi = -60, byteArrayOf(1), now),
                bleScannedDevice(device(2), rssi = -90, null, now),
                bleScannedDevice(device(3), rssi = -30, byteArrayOf(3), now),
                bleScannedDevice(device(1), rssi = -60, byteArrayOf(1), now)
            )

            // When
            assertThat(scannedDeviceSelector.isEmpty()).isFalse()
            val results = scannedDeviceSelector.select()

            // Then
            assertThat(results).containsExactly(
                bleScannedDevice(device(3), rssi = -20, byteArrayOf(3), now),
                bleScannedDevice(device(4), rssi = -40, byteArrayOf(4), now),
                bleScannedDevice(device(5), rssi = -50, null, now),
                bleScannedDevice(device(1), rssi = -60, byteArrayOf(1), now),
                bleScannedDevice(device(2), rssi = -30, null, now),
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()
        }

    @Test
    fun select_given_multiple_address_for_a_same_device_having_unique_service_data_should_select_most_recent_one() =
        runBlockingTest {

            // Given
            val timestamp = Date()
            val serviceData = byteArrayOf(1, 2, 3)
            val rssi = -30

            val scannedDeviceSelector = givenScannedDeviceSelector().withScans(
                bleScannedDevice(device(1), rssi, serviceData, timestamp.plus(1)),
                bleScannedDevice(device(2), rssi, serviceData, timestamp.plus(2)),
                bleScannedDevice(device(3), rssi, serviceData, timestamp.plus(3)),
                bleScannedDevice(device(4), rssi, serviceData, timestamp.plus(4)),
                bleScannedDevice(device(5), rssi, serviceData, timestamp.plus(5))
            )

            // When
            assertThat(scannedDeviceSelector.isEmpty()).isFalse()
            val results = scannedDeviceSelector.select()

            // Then
            assertThat(results).containsExactly(
                bleScannedDevice(device(5), rssi, serviceData, timestamp.plus(5))
            )
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()
        }


    @Test
    fun select_given_multiple_devices_without_stats_should_select_ones_with_rssi_timestammp() =
        runBlockingTest {

            // Given
            val now = Date()
            val rssi = -30

            val deviceStatsProvider = givenDeviceStatsProvider(emptyMap())

            val scannedDeviceSelector = givenScannedDeviceSelector(
                maxDelayBetweenSuccess = 0L,
                deviceStatsProvider = deviceStatsProvider
            ).withScans(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3))
            )

            // When
            val result = scannedDeviceSelector.select()

            // Then
            assertThat(result).containsExactly(
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1))
            ).inOrder()
        }

    @Test
    fun select_given_multiple_devices_should_select_ones_with_best_confidence_score() =
        runBlockingTest {

            // Given
            val now = Date()
            val rssi = -30

            val deviceStatsProvider = givenDeviceStatsProvider(
                mapOf(
                    byteArrayOf(1).deviceId() to BleDeviceStats(successCount = 1),
                    byteArrayOf(3).deviceId() to BleDeviceStats(failureCount = 1)
                )
            )

            val scannedDeviceSelector = givenScannedDeviceSelector(
                maxDelayBetweenSuccess = 0L,
                deviceStatsProvider = deviceStatsProvider
            ).withScans(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3))
            )

            // When
            val result = scannedDeviceSelector.select()

            // Then
            assertThat(result).containsExactly(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3))
            ).inOrder()
        }

    @Test
    fun select_given_mutliple_devices_should_remove_ones_which_just_succeed_and_select_ones_with_best_confidence_score() =
        runBlockingTest {

            // Given
            val now = Date()
            val rssi = -30

            val deviceStatsProvider = givenDeviceStatsProvider(
                mapOf(
                    byteArrayOf(1).deviceId() to BleDeviceStats(lastSuccessTime = now.minus(1000).time)
                )
            )

            val scannedDeviceSelector = givenScannedDeviceSelector(
                maxDelayBetweenSuccess = 30 * 1000L,
                deviceStatsProvider = deviceStatsProvider
            ).withScans(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3))
            )


            // When
            val result = scannedDeviceSelector.select()

            // Then
            assertThat(result).containsExactly(
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3)),
                bleScannedDevice(device(2), rssi, null, now.plus(2))
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()

        }


    @Test
    fun select_given_multiple_devices_should_remove_ones_with_too_many_failures_and_select_others_with_best_confidence_score() =
        runBlockingTest {

            // Given
            val now = Date()
            val rssi = -30

            val deviceStatsProvider = givenDeviceStatsProvider(
                mapOf(
                    byteArrayOf(1).deviceId() to BleDeviceStats(successiveFailureCount = 11)
                )
            )

            val scannedDeviceSelector = givenScannedDeviceSelector(
                maxDelayBetweenSuccess = 0L,
                deviceStatsProvider = deviceStatsProvider
            ).withScans(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3))
            )

            // When
            val result = scannedDeviceSelector.select()

            // Then
            assertThat(result).containsExactly(
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3)),
                bleScannedDevice(device(2), rssi, null, now.plus(2))
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()

        }

    @Test
    fun select_given_blacklisted_device_should_not_select_it() = runBlockingTest {

        // Given
        val now = Date()
        val rssi = -30

        val deviceStatsProvider = givenDeviceStatsProvider(
            mapOf(
                byteArrayOf(1).deviceId() to BleDeviceStats(shouldIgnore = true)
            )
        )

        val scannedDeviceSelector = givenScannedDeviceSelector(
            deviceStatsProvider = deviceStatsProvider
        ).withScans(
            bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
        )

        // When
        val result = scannedDeviceSelector.select()

        // Then
        assertThat(result).isEmpty()
        assertThat(scannedDeviceSelector.isEmpty()).isTrue()
    }

    private fun givenScannedDeviceSelector(
        maxDelayBetweenSuccess: Long = 0L,
        maxSuccessiveFailureCount: Int = 10,
        timestampIsImportant: Boolean = true,
        deviceStatsProvider: BleDeviceStatsProvider = givenDeviceStatsProvider()
    ) = BleScannedDeviceSelector(
        maxDelayBetweenSuccess = maxDelayBetweenSuccess,
        maxSuccessiveFailureCount = maxSuccessiveFailureCount,
        timestampIsImportantInSelection = timestampIsImportant,
        payloadIdProvider = object : ProximityPayloadIdProvider {
            override suspend fun fromProximityPayload(proximityPayload: ProximityPayload): ProximityPayloadId {
                return proximityPayload.data
            }
        },
        deviceStatsProvider = deviceStatsProvider
    )

    private fun givenDeviceStatsProvider(stats: Map<BleDeviceId, BleDeviceStats> = emptyMap()): BleDeviceStatsProvider =
        { stats[it] }

    private fun BleScannedDeviceSelector.withoutScans() = this
    private fun BleScannedDeviceSelector.withScans(vararg scans: BleScannedDevice) = apply {
        add(scans.toList())
    }

    private fun ByteArray.deviceId() = contentHashCode()

    private val bluetoothDevices by lazy {
        (1..9).map { it to bluetoothDevice(it.toString()) }.toMap()
    }

    private fun device(index: Int): BluetoothDevice = bluetoothDevices.getValue(index)

}

