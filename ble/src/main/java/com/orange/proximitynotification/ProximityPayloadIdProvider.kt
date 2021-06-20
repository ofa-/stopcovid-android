/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification

/**
 * [ProximityPayload] identifier
 */
typealias ProximityPayloadId = ByteArray

/**
 * Provides the [ProximityPayloadId]
 *
 * @see ProximityPayloadId
 */
interface ProximityPayloadIdProvider {

    /**
     * Returns the current [ProximityPayloadId] for a [ProximityPayload]
     *
     * @param proximityPayload [ProximityPayload] from which [ProximityPayloadId] should be extracted
     * @return [ProximityPayloadId] extracted or null if not found
     */
    suspend fun fromProximityPayload(proximityPayload: ProximityPayload): ProximityPayloadId?
}
