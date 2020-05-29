/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/07 - for the STOP-COVID project
 */

package com.orange.proximitynotification.tools

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExpiringCacheTest {

    @Test
    fun get_given_expired_value_should_remove_it_and_return_null() {

        // Given
        val cache = spy(ExpiringCache<String, String>(5, 1000))
        val key1 = "key1"
        val key2 = "key2"
        val value1 = "value1"
        val value2 = "value2"

        cache.advanceBy(100L)
        cache.put(key1, value1) // will expire at 1100
        cache.advanceBy(200L)
        cache.put(key2, value2) // will expire at 1200

        assertThat(cache[key1]).isEqualTo(value1)
        assertThat(cache[key2]).isEqualTo(value2)

        // When
        cache.advanceBy(1100L)

        // Then
        assertThat(cache.size()).isEqualTo(2)
        assertThat(cache[key1]).isNull()
        assertThat(cache[key2]).isEqualTo(value2)
        assertThat(cache.size()).isEqualTo(1)
    }

    @Test
    fun cleanUp_given_expired_values_should_remove_them() {

        // Given
        val cache = spy(ExpiringCache<String, String>(4, 1000))

        cache.put("key1", "value1")
        cache.put("key2", "value2")
        cache.put("key3", "value3")
        cache.advanceBy(200L)
        cache.put("key4", "value4")

        cache.advanceBy(1100L)
        assertThat(cache.size()).isEqualTo(4)
        assertThat(cache["key4"]).isEqualTo("value4")
        assertThat(cache.size()).isEqualTo(4)

        // When
        cache.cleanUp()

        // Then
        assertThat(cache.size()).isEqualTo(1)
        assertThat(cache["key1"]).isNull()
        assertThat(cache["key2"]).isNull()
        assertThat(cache["key3"]).isNull()
        assertThat(cache["key4"]).isEqualTo("value4")
    }

    @Test
    fun put_given_max_size_reached_should_keep_most_recent_entries() {

        // Given
        val cache = spy(ExpiringCache<String, String>(1, 1000))
        val key1 = "key1"
        val key2 = "key2"
        val value1 = "value1"
        val value2 = "value2"

        // When
        cache.put(key1, value1)
        cache.put(key2, value2)

        // Then
        assertThat(cache[key1]).isNull()
        assertThat(cache[key2]).isEqualTo(value2)
    }

    private fun <K, V> ExpiringCache<K, V>.advanceBy(timeout: Long) {
        doReturn(timeout).whenever(this).elapsedRealtime()
    }
}