/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification

import com.google.common.truth.Truth.assertThat
import com.googlecode.zohhak.api.TestWith
import com.googlecode.zohhak.api.runners.ZohhakRunner
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(ZohhakRunner::class)
class ProximityPayloadTest {

    @TestWith(
        value = [
            "0", "1", "8", "17"
        ]
    )
    fun init_with_incorrect_data_size_should_throw_IllegalArgumentException(size: Int) {
        // Given
        val data = ByteArray(size)

        // When
        try {
            ProximityPayload(data)
            Assert.fail("init should have failed for size : $size")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().contains(" Got $size")
        }

        // Then

    }

    @Test
    fun init_with_correct_data_size_should_succeed() {
        // Given
        val data = ByteArray(ProximityPayload.SIZE)

        // When
        val result = ProximityPayload(data)

        // Then
        assertThat(result.data).isEqualTo(data)
    }

    @Test
    fun equal_with_same_values_should_return_true() {

        // Given
        val proximityPayload1 = ProximityPayload((1..16).map { it.toByte() }.toByteArray())
        val proximityPayload2 = ProximityPayload((1..16).map { it.toByte() }.toByteArray())

        // When

        // Then
        assertThat(proximityPayload1).isEqualTo(proximityPayload2)
    }

    @Test
    fun equal_with_different_values_should_return_false() {

        // Given
        val proximityPayload1 = ProximityPayload((1..16).map { it.toByte() }.toByteArray())
        val proximityPayload2 = ProximityPayload((2..17).map { it.toByte() }.toByteArray())

        // When

        // Then
        assertThat(proximityPayload1).isNotEqualTo(proximityPayload2)
    }
}