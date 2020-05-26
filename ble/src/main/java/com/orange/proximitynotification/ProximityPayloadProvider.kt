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

/**
 * Provides the ProximityPayload to exchange
 *
 * @see ProximityPayload
 */
interface ProximityPayloadProvider {

    /**
     * Return the current ProximityPayload
     *
     * @return ProximityPayload to exchange
     */
    suspend fun current(): ProximityPayload
}
