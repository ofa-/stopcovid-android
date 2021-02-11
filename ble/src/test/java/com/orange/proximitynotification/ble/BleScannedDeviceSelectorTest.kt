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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.orange.proximitynotification.ProximityPayload
import com.orange.proximitynotification.ProximityPayloadId
import com.orange.proximitynotification.ProximityPayloadIdProvider
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class BleScannedDeviceSelectorTest {

    private val cacheTimeout = 3L
    private val minConfidenceScore = -1
    private val minStatsCount = 3

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
    fun select_given_multiple_devices_should_select_ones_with_best_confidence_score() =
        runBlockingTest {

            // Given
            val now = Date()
            val rssi = -30

            val scannedDeviceSelector = givenScannedDeviceSelector().withScans(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3))
            )
            assertThat(scannedDeviceSelector.select()).containsExactly(
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1))
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()

            // When
            scannedDeviceSelector.succeed(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1))
            )

            scannedDeviceSelector.withScans(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3))
            )

            // Then
            assertThat(scannedDeviceSelector.select()).containsExactly(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3)),
                bleScannedDevice(device(2), rssi, null, now.plus(2))
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()

        }

    @Test
    fun select_given_multiple_devices_should_evict_scans_with_bad_confidence_score() =
        runBlockingTest {

            // Given
            val now = Date()
            val rssi = -30

            val scannedDeviceSelector = givenScannedDeviceSelector().withScans(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3))
            )
            assertThat(scannedDeviceSelector.select()).containsExactly(
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1))
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()

            // When
            scannedDeviceSelector.failed(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1))
            )

            scannedDeviceSelector.withScans(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3))
            )

            // Then
            assertThat(scannedDeviceSelector.select()).containsExactly(
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3)),
                bleScannedDevice(device(2), rssi, null, now.plus(2))
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()

        }

    @Test
    fun select_given_multiple_devices_should_unprioritize_scans_if_failed_to_scan() =
        runBlockingTest {

            // Given
            val now = Date()
            val rssi = -30

            val scannedDeviceSelector = givenScannedDeviceSelector().withScans(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2))
            )
            assertThat(scannedDeviceSelector.select()).containsExactly(
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1))
            ).inOrder()

            // When
            scannedDeviceSelector.failedToScan(
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
            )

            scannedDeviceSelector.withScans(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2))
            )

            // Then
            assertThat(scannedDeviceSelector.select()).containsExactly(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2))
            ).inOrder()

        }

    @Test
    fun select_given_multiple_devices_should_not_prioritize_scans_if_stats_count_is_high_enough() =
        runBlockingTest {

            // Given
            val now = Date()
            val rssi = -30

            val scannedDeviceSelector = givenScannedDeviceSelector().withScans(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3))
            )
            assertThat(scannedDeviceSelector.select()).containsExactly(
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1))
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()

            // When
            bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1))
                .also { scan ->
                    repeat(minStatsCount + 1) {
                        scannedDeviceSelector.succeed(scan)
                    }
                }

            scannedDeviceSelector.withScans(
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3))
            )

            // Then
            assertThat(scannedDeviceSelector.select()).containsExactly(
                bleScannedDevice(device(3), rssi, byteArrayOf(3), now.plus(3)),
                bleScannedDevice(device(2), rssi, null, now.plus(2)),
                bleScannedDevice(device(1), rssi, byteArrayOf(1), now.plus(1))
            ).inOrder()
            assertThat(scannedDeviceSelector.isEmpty()).isTrue()

        }




    private fun givenScannedDeviceSelector(timestampIsImportant: Boolean = true) =
        BleScannedDeviceSelector(
            cacheTimeout = cacheTimeout,
            minConfidenceScore = minConfidenceScore,
            minStatsCount = minStatsCount,
            timestampIsImportantInSelection = timestampIsImportant,
            payloadIdProvider = object : ProximityPayloadIdProvider {
                override suspend fun fromProximityPayload(proximityPayload: ProximityPayload): ProximityPayloadId {
                    return proximityPayload.data
                }
            }
        )

    private fun BleScannedDeviceSelector.withoutScans() = this
    private fun BleScannedDeviceSelector.withScans(vararg scans: BleScannedDevice) = apply {
        add(scans.toList())
    }


    private val bluetoothDevices by lazy {
        (1..9).map { it to bluetoothDevice(it.toString()) }.toMap()
    }

    private fun device(index: Int): BluetoothDevice = bluetoothDevices.getValue(index)

}

