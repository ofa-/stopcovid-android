/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/12/19 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProximityPayloadIdProviderWithCacheTest {

    private val proximityPayloadIdProvider: ProximityPayloadIdProvider = mock()

    private val proximityPayloadIdProviderWithCache =
        ProximityPayloadIdProviderWithCache(
            proximityPayloadIdProvider,
            maxSize = 1,
            expiringTime = 30_000L
        )

    @Test
    fun fromProximityPayload_given_empty_cache_should_call_fromProximityPayload() =
        runBlockingTest {

            // Given
            val proximityPayload: ProximityPayload = mock()
            val proximityPayloadId: ProximityPayloadId = byteArrayOf(1, 2, 3)

            doReturn(proximityPayloadId)
                .whenever(proximityPayloadIdProvider)
                .fromProximityPayload(eq(proximityPayload))

            // When
            val result = proximityPayloadIdProviderWithCache.fromProximityPayload(proximityPayload)

            // Then
            assertThat(result).isEqualTo(proximityPayloadId)
            verify(proximityPayloadIdProvider, times(1))
                .fromProximityPayload(eq(proximityPayload))
        }

    @Test
    fun fromProximityPayload_given_cached_result_should_not_call_fromProximityPayload() =
        runBlockingTest {

            // Given
            val proximityPayload: ProximityPayload = mock()
            val proximityPayloadId: ProximityPayloadId = byteArrayOf(1, 2, 3)

            doReturn(proximityPayloadId)
                .whenever(proximityPayloadIdProvider)
                .fromProximityPayload(eq(proximityPayload))

            assertThat(proximityPayloadIdProviderWithCache.fromProximityPayload(proximityPayload))
                .isEqualTo(proximityPayloadId)
            verify(proximityPayloadIdProvider, times(1))
                .fromProximityPayload(eq(proximityPayload))
            reset(proximityPayloadIdProvider)

            // When
            val result = proximityPayloadIdProviderWithCache.fromProximityPayload(proximityPayload)

            // Then
            assertThat(result).isEqualTo(proximityPayloadId)
            verifyNoMoreInteractions(proximityPayloadIdProvider)
        }
}