/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.tools

import android.os.SystemClock
import android.util.LruCache

private data class ExpiringValue<V>(val value: V, val expireTime: Long)

/**
 * ExpiringCache uses LruCache to store values and keeping them depending on 2 conditions:
 * - max size is not exceeded
 * - expireTime for a value is expired
 */
internal class ExpiringCache<K, V>(maxSize: Int, private val expireTime: Long) {
    private val lruCache: LruCache<K, ExpiringValue<V>> = LruCache(maxSize)

    @Synchronized
    operator fun get(key: K): V? {
        val current = lruCache.get(key) ?: return null
        if (elapsedRealtime() >= current.expireTime) {
            remove(key)
            return null
        }
        return current.value
    }

    @Synchronized
    fun put(key: K, value: V): V? {
        val previous = lruCache.put(key, ExpiringValue(value, elapsedRealtime() + expireTime))
        return previous?.value
    }

    @Synchronized
    fun cleanUp() {
        val elapsedTime = elapsedRealtime()
        lruCache.snapshot()
            .filterValues { elapsedTime >= it.expireTime }
            .forEach { remove(it.key) }
    }

    private fun remove(key: K) {
        lruCache.remove(key)
    }

    fun size(): Int {
        return lruCache.size()
    }

    internal fun elapsedRealtime(): Long {
        return SystemClock.elapsedRealtime()
    }

}