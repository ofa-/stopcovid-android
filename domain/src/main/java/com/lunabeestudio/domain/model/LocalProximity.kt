/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.domain.model

import android.util.Base64

data class LocalProximity(val eccBase64: String,
    val ebidBase64: String,
    val macBase64: String,
    val helloTime: Int,
    val collectedTime: Long,
    val rawRssi: Int,
    val calibratedRssi: Int) {

    constructor(hello: Hello, collectedNtpTimeS: Long, rawRssi: Int, calibratedRssi: Int) : this(
        eccBase64 = Base64.encodeToString(hello.eccArray, Base64.NO_WRAP),
        ebidBase64 = Base64.encodeToString(hello.ebidArray, Base64.NO_WRAP),
        macBase64 = Base64.encodeToString(hello.macArray, Base64.NO_WRAP),
        helloTime = hello.time,
        collectedTime = collectedNtpTimeS,
        rawRssi = rawRssi,
        calibratedRssi = calibratedRssi
    )
}
