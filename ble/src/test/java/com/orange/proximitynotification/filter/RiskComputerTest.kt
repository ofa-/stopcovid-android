/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.filter

import com.google.common.truth.Truth.assertThat
import com.googlecode.zohhak.api.runners.ZohhakRunner
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.math.ln

@RunWith(ZohhakRunner::class)
class RiskComputerTest {

    private val rssiRiskComputer =
        RiskComputer(
            deltas = listOf(39.0, 27.0, 23.0, 21.0, 20.0, 19.0, 18.0, 17.0, 16.0, 15.0),
            p0 = -66.0,
            a = 10 / ln(10.0),
            timeWindow = 120,
            timeOverlap = 60
        )

    private val durationInSeconds = 15 * 60L

    @Test
    fun compute_without_duration_should_return_empty_risks() {

        // Given
        val timestampedRssis: List<TimestampedRssi> = emptyList()

        // When
        val result = rssiRiskComputer.compute(timestampedRssis, Date(), 0)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun compute_without_data_should_not_return_any_risks() {

        // Given
        val timestampedRssis: List<TimestampedRssi> = emptyList()

        // When
        val result = rssiRiskComputer.compute(timestampedRssis, Date(), durationInSeconds)

        // Then
        assertThat(result).hasSize(15)
        assertThat(result.sum()).isEqualTo(0.0)
    }

    @Test
    fun compute_with_one_timestamp_after_time_window_should_return_expected_zero_risk() {

        // Given
        val from = Date(0)
        val timestampedRssi = timestampedRssi(Date((durationInSeconds * 1000) + 1), 1)

        // When
        val result = rssiRiskComputer.compute(listOf(timestampedRssi), from, durationInSeconds)

        // Then
        assertThat(result).hasSize(15)
        assertThat(result.sum()).isEqualTo(0.0)
    }

    @Test
    fun compute_with_one_timestamp_before_time_window_should_return_zero_risk() {

        // Given
        val from = Date(1000)
        val timestampedRssi = timestampedRssi(Date(0), 1)

        // When
        val result = rssiRiskComputer.compute(listOf(timestampedRssi), from, durationInSeconds)

        // Then
        assertThat(result).hasSize(15)
        assertThat(result.sum()).isEqualTo(0.0)
    }
}
