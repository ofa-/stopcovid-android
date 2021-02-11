/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2021/01/20 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.tools

import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test

class ResultTest {

    @Test
    fun `valueOrNull given Result Failure should return null`() {

        // Given
        val failure : Result<Int> = Result.Failure(mock())

        // When
        val result = failure.valueOrNull()

        // Then
        Truth.assertThat(result).isNull()
    }

    @Test
    fun `valueOrNull given Result Success should return value`() {

        // Given
        val value = 4
        val success : Result<Int> = Result.Success(value)

        // When
        val result = success.valueOrNull()

        // Then
        Truth.assertThat(result).isNotNull()
    }
}