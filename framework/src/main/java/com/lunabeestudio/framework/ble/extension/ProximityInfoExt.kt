/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.ble.extension

import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.Hello
import com.lunabeestudio.domain.model.LocalProximity
import com.orange.proximitynotification.ProximityInfo
import com.orange.proximitynotification.ble.BleProximityMetadata

internal fun ProximityInfo.hello(): Hello? {
    return try {
        Hello(payload.data)
    } catch (e: IllegalArgumentException) {
        null
    }
}

fun ProximityInfo.toLocalProximity(): LocalProximity? {
    return hello()?.let { hello ->
        (this.metadata as? BleProximityMetadata)?.let { (rawRssi, calibratedRssi) ->
            LocalProximity(hello = hello,
                collectedNtpTimeS = timestamp.time.unixTimeMsToNtpTimeS(),
                rawRssi = rawRssi,
                calibratedRssi = calibratedRssi)
        }
    }
}