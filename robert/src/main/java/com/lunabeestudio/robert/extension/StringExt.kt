/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/29/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.extension

import java.nio.ByteBuffer

fun String.splitToByteArray(): ByteArray {
    val split = split('.')
    val byteBuffer = ByteBuffer.allocate(split.size)
    split.forEach {
        byteBuffer.put(it.toByte())
    }
    return byteBuffer.array()
}