/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/07 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble

import java.util.Date

internal data class BleRecord(
    val payload: BlePayload,
    val rssi: Int,
    val timestamp: Date
) {
    val txPowerLevel: Int
        get() = payload.txPowerLevel
}

