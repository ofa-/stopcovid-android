/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/12/18 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification

import com.orange.proximitynotification.tools.ExpiringCache

/**
 * [ProximityPayloadIdProvider] wrapper implementation using [ExpiringCache]
 *
 * @param proximityPayloadIdProvider wrapped [ProximityPayloadIdProvider]
 * @param maxSize [ExpiringCache] maxSize
 * @param expiringTime [ExpiringCache] expiringTime
 */
internal class ProximityPayloadIdProviderWithCache(
    private val proximityPayloadIdProvider: ProximityPayloadIdProvider,
    maxSize: Int,
    expiringTime: Long
) : ProximityPayloadIdProvider {

    private val cache = ExpiringCache<ProximityPayload, ProximityPayloadId>(maxSize, expiringTime)

    override suspend fun fromProximityPayload(proximityPayload: ProximityPayload): ProximityPayloadId? {
        cache[proximityPayload]?.let {
            return it
        }

        val proximityPayloadId = proximityPayloadIdProvider.fromProximityPayload(proximityPayload)
        proximityPayloadId?.let {
            cache.put(proximityPayload, proximityPayloadId)
        }

        return proximityPayloadId
    }
}
