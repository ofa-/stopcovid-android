/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/26 - for the STOP-COVID project
 */

package com.orange.proximitynotification.filter

import com.google.common.truth.Truth
import com.googlecode.zohhak.api.Coercion
import com.googlecode.zohhak.api.TestWith
import com.googlecode.zohhak.api.runners.ZohhakRunner
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(ZohhakRunner::class)
class RssiClipperTest {

    @Test
    fun clip_with_empty_rssi_should_return_empty_result() {

        // Given
        val timestampedRssis = emptyList<TimestampedRssi>()
        val rssiClipper = RssiClipper(rssiThreshold = 0)

        // When
        val result = rssiClipper.clip(timestampedRssis)

        // Then
        Truth.assertThat(result.clippedTimestampedRssis).isEmpty()
        Truth.assertThat(result.filteredPeaks).isEmpty()
    }

    @TestWith(
        separator = ";",
        value = [
            "[0];0;[0];[]",
            "[1];0;[0];[1]",
            "[1,2,3];0;[0,0,0];[1,2,3]",
            "[1,-20];0;[-20,-20];[1]",
            "[-20,1];0;[-20,-20];[1]"
        ]
    )
    fun clip_with_rssi_should_return_expected_rssi_and_peaks(
        timestampedRssis: List<Int>,
        rssiThreshold: Int,
        expectedTimestampedRssis: List<Int>,
        expectedPeaks: List<Int>
    ) {

        // Given
        val rssiClipper = RssiClipper(rssiThreshold = rssiThreshold)

        // When
        val result = rssiClipper.clip(timestampedRssis.toTimestampedRssis())

        // Then
        Truth.assertThat(result.clippedTimestampedRssis)
            .isEqualTo(expectedTimestampedRssis.toTimestampedRssis())
        Truth.assertThat(result.filteredPeaks).isEqualTo(expectedPeaks)
    }

    @Coercion
    fun toIntegerArray(input: String): List<Int> {
        return runCatching {
            input
                .replace("[", "")
                .replace("]", "")
                .split(",")
                .map { it.toInt() }
        }.getOrElse { emptyList() }
    }
}

private fun List<Int>.toTimestampedRssis() = map {
    timestampedRssi(
        timestamp = Date(0),
        rssi = it
    )
}