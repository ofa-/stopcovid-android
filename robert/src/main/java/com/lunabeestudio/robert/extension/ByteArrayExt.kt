/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.extension

import java.security.SecureRandom

fun ByteArray.randomize() {
    for (i in 0 until size) {
        set(i, SecureRandom().nextInt().toByte())
    }
}

fun <T> ByteArray.use(block: (ByteArray) -> T): T {
    val res = block(this)
    this.randomize()
    return res
}