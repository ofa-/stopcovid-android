/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/28 - for the STOP-COVID project
 */

package com.orange.proximitynotification.filter

import java.util.Date

internal fun timestampedRssi(timestamp: Date = Date(), rssi: Int = 0) = TimestampedRssi(
    id = "any",
    timestamp = timestamp,
    rssi = rssi
)