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
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(ZohhakRunner::class)
class BleRecordTest {

    @TestWith(
        value = [
            "0", "1", "-1"
        ]
    )
    fun txPowerLevel_should_return_txPowerLevel_from_payload(txPowerLevel: Int) {
        // Given
        val record = record(payload = payload(txPowerLevel = txPowerLevel))

        // When
        val result = record.txPowerLevel

        // Then
        assertThat(result).isEqualTo(txPowerLevel)
    }

    @Test
    fun equal_with_same_values_should_return_true() {

        // Given
        val now = Date()
        val record1 = record(timestamp = now)
        val record2 = record(timestamp = now)

        // When

        // Then
        assertThat(record1).isEqualTo(record2)
    }

    @Test
    fun equal_with_different_rssi_should_return_false() {

        // Given
        val now = Date()
        val record1 = record(rssi = 1, timestamp = now)
        val record2 = record(rssi = 2, timestamp = now)

        // When

        // Then
        assertThat(record1).isNotEqualTo(record2)
    }
}
