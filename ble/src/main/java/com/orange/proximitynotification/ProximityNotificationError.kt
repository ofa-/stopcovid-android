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

data class ProximityNotificationError(
    val type: Type,
    val rootErrorCode: Int? = null,
    val cause: String? = null
) {

    companion object {
        /**
         * Root error code when too much BLE operation fail which may indicate that Bluetooth Stack
         * is unhealthy.
         */
        const val UNHEALTHY_BLUETOOTH_ERROR_CODE = 1000
    }

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
         * BLE gatt error
         */
        BLE_GATT,
    }
}