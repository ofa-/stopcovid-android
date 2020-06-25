/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/28 - for the STOP-COVID project
 */

package com.orange.proximitynotification.filter

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Date

class ProximityFilterTest {

    private val config = ProximityFilter.Config(
        durationThreshold = 2 * 60,
        rssiThreshold = 1
    )
    private val proximityFilter = ProximityFilter(config)

    @Test
    fun filter_given_empty_data_should_return_rejected() {

        // Given
        val timestampRssis = emptyList<TimestampedRssi>()

        // When
        val output = proximityFilter.filter(timestampRssis, Date(0), 2 * 60, ProximityFilter.Mode.FULL)

        // Then
        assertThat(output).isEqualTo(ProximityFilter.Output.Rejected)
    }

    @Test
    fun filter_given_full_mode_and_too_short_duration_should_return_rejected() {

        // Given
        val timestampRssis = listOf(
            timestampedRssi(Date(0), 0),
            timestampedRssi(Date(1 * 60 * 1000), 1)
        )

        // When
        val output = proximityFilter.filter(timestampRssis, Date(0), 2 * 60, ProximityFilter.Mode.FULL)

        // Then
        assertThat(output).isEqualTo(ProximityFilter.Output.Rejected)
    }

}