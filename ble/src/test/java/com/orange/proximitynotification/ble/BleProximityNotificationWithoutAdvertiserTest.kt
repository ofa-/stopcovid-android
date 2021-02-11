/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2021/01/04 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import com.google.common.truth.Truth
import org.junit.Test

class BleProximityNotificationWithoutAdvertiserTest {


    class StatsTest {

        private val stats = BleProximityNotificationWithoutAdvertiser.Stats(
            minStatCount = 5,
            errorRateThreshold = 0.5F,
            minScanErrorCount = 1
        )

        @Test
        fun `isUnHealthy given no results should return false`() {

            // Given

            // When
            val result = stats.isUnHealthy()

            // Then
            Truth.assertThat(result).isFalse()
        }

        @Test
        fun `isUnHealthy given results in success should return false`() {

            // Given
            stats.succeed()
            stats.failed()
            stats.succeed()

            // When
            val result = stats.isUnHealthy()

            // Then
            Truth.assertThat(result).isFalse()
        }

        @Test
        fun `isUnHealthy given errors but not enough history should return false`() {

            // Given
            stats.failed()
            stats.failed()
            stats.failed()
            stats.failed()

            // When
            val result = stats.isUnHealthy()

            // Then
            Truth.assertThat(result).isFalse()
        }

        @Test
        fun `isUnHealthy given enough errors should return true`() {

            // Given
            stats.failed()
            stats.failed()
            stats.failed()
            stats.failed()
            stats.failed()

            // When
            val result = stats.isUnHealthy()

            // Then
            Truth.assertThat(result).isTrue()
        }

        @Test
        fun `isUnHealthy given too much scanner start error should return true`() {

            // Given
            stats.scanFailed()

            // When
            val result = stats.isUnHealthy()

            // Then
            Truth.assertThat(result).isTrue()
        }

        @Test
        fun `isUnHealthy given scanner start success after an error should return false`() {

            // Given
            stats.scanFailed()
            stats.scanSucceed()

            // When
            val result = stats.isUnHealthy()

            // Then
            Truth.assertThat(result).isFalse()
        }

        @Test
        fun `isUnHealthy given too much errors should return true`() {

            // Given
            stats.failed()
            stats.failed()
            stats.succeed()
            stats.failed()
            stats.succeed()

            // When
            val result = stats.isUnHealthy()

            // Then
            Truth.assertThat(result).isTrue()
        }


        @Test
        fun `isUnHealthy should use last results from history to compute final result`() {

            // Given 1 success / 0 error Then result should be false
            stats.succeed()
            Truth.assertThat(stats.isUnHealthy()).isFalse()

            // Given 1 success / 1 error Then result should be false
            stats.failed()
            Truth.assertThat(stats.isUnHealthy()).isFalse()

            // Given 3 success / 2 error Then result should be false
            stats.succeed()
            stats.failed()
            stats.succeed()
            Truth.assertThat(stats.isUnHealthy()).isFalse()

            // Given 2 success / 3 error Then result should be true
            stats.failed()
            Truth.assertThat(stats.isUnHealthy()).isTrue()

            // Given 3 success / Z error Then result should be false
            stats.succeed()
            Truth.assertThat(stats.isUnHealthy()).isFalse()
        }

    }
}