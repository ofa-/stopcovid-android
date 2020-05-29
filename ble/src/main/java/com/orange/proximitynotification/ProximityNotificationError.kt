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

data class ProximityNotificationError(val type: Type, val rootErrorCode: Int? = null, val cause: String? = null) {

    enum class Type {
        /**
         * BLE advertising error
         */
        BLE_ADVERTISER,

        /**
         * BLE scanner error
         */
        BLE_SCANNER,

        /**
         * BLE proximity notification component error
         */
        BLE_PROXIMITY_NOTIFICATION
    }
}