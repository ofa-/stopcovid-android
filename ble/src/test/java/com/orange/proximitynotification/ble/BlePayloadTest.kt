/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble

import com.google.common.truth.Truth.assertThat
import com.googlecode.zohhak.api.TestWith
import com.googlecode.zohhak.api.runners.ZohhakRunner
import com.orange.proximitynotification.ProximityPayload
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ZohhakRunner::class)
class BlePayloadTest {

    @TestWith(
        value = [
            "0", "1", "8", "16", "17"
        ]
    )
    fun from_with_incorrect_data_should_throw_IllegalArgumentException(size: Int) {

        // Given
        val data = ByteArray(size)

        // When
        try {
            BlePayload.from(data)
            Assert.fail("from should have failed for size : $size")
        } catch (e: IllegalArgumentException) {
            // Then
            assertThat(e).hasMessageThat().contains(" Got $size")
        }
    }

    @TestWith(
        value = [
            "0", "1", "8", "16", "17"
        ]
    )
    fun fromOrNull_with_incorrect_data_should_return_null(size: Int) {

        // Given
        val data = ByteArray(size)

        // When
        val result = BlePayload.fromOrNull(data)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun from_should_return_expected_payload() {

        // Given
        val proximityPayload = proximityPayload()
        val version = 2
        val txCompensationGain = -1
        val payload: ByteArray =
            proximityPayload.data + byteArrayOf(version.toByte()) + byteArrayOf(txCompensationGain.toByte())
        val expected = payload(proximityPayload = proximityPayload, version = 2, txPowerLevel = -1)

        // When
        val result = BlePayload.from(payload)

        // Then
        assertThat(result).isEqualTo(expected)
        assertThat(result.proximityPayload).isEqualTo(proximityPayload)
        assertThat(result.version).isEqualTo(version)
        assertThat(result.txPowerLevel).isEqualTo(txCompensationGain)
    }

    @Test
    fun toByteArray_should_match() {

        // Given
        val proximityPayload = ProximityPayload((1..16).map { it.toByte() }.toByteArray())
        val payload = payload(proximityPayload = proximityPayload, version = 2, txPowerLevel = -1)
        val expected: ByteArray =
            byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 2, -1)

        // When
        val result = payload.toByteArray()

        // Then
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun equal_with_same_values_should_return_true() {

        // Given
        val payload1 = payload()
        val payload2 = payload()

        // When

        // Then
        assertThat(payload1).isEqualTo(payload2)
    }

    @Test
    fun equal_with_different_proximity_payload_should_return_false() {

        // Given
        val payload1 =
            payload(proximityPayload = ProximityPayload((1..16).map { it.toByte() }.toByteArray()))
        val payload2 =
            payload(proximityPayload = ProximityPayload((2..17).map { it.toByte() }.toByteArray()))

        // When

        // Then
        assertThat(payload1).isNotEqualTo(payload2)
    }

    @Test
    fun equal_with_different_versions_should_return_false() {

        // Given
        val payload1 = payload(version = 1)
        val payload2 = payload(version = 2)

        // When

        // Then
        assertThat(payload1).isNotEqualTo(payload2)
    }

    @Test
    fun equal_with_different_txPowerLevel_should_return_false() {

        // Given
        val payload1 = payload(txPowerLevel = 1)
        val payload2 = payload(txPowerLevel = 2)

        // When

        // Then
        assertThat(payload1).isNotEqualTo(payload2)
    }

}