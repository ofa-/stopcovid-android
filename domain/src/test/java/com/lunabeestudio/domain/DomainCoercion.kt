/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.domain

import com.googlecode.zohhak.api.Coercion
import com.lunabeestudio.domain.model.Hello
import java.nio.ByteBuffer

class DomainCoercion {
    @Coercion
    fun stringToByteArray(input: String?): ByteArray {
        val split = input?.split('.')
        val byteBuffer = ByteBuffer.allocate(split?.size ?: 0)
        split?.forEach {
            byteBuffer.put(it.toByte())
        }
        return byteBuffer.array()
    }

    @Coercion
    fun bytesStringToHello(input: String?): Hello {
        return Hello(stringToByteArray(input))
    }
}
