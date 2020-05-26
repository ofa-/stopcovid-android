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

data class Hello(
    val eccArray: ByteArray = ByteArray(1),
    val ebidArray: ByteArray = ByteArray(8),
    val timeArray: ByteArray = ByteArray(2),
    val macArray: ByteArray = ByteArray(5)
) {

    constructor(rawData: ByteArray) : this() {
        if (rawData.size < 16) {
            throw IllegalArgumentException("Expecting a byte array >= 16 bytes. Got ${rawData.size}.")
        }
        rawData.copyInto(eccArray, startIndex = 0, endIndex = 1)
        rawData.copyInto(ebidArray, startIndex = 1, endIndex = 9)
        rawData.copyInto(timeArray, startIndex = 9, endIndex = 11)
        rawData.copyInto(macArray, startIndex = 11, endIndex = 16)
    }

    val data: ByteArray
        get() = this.eccArray + this.ebidArray + this.timeArray + this.macArray

    @OptIn(ExperimentalUnsignedTypes::class)
    val time: Int
        get() = (timeArray[0].toUByte().toInt() shl 8) + (timeArray[1].toUByte().toInt())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Hello

        if (!eccArray.contentEquals(other.eccArray)) return false
        if (!ebidArray.contentEquals(other.ebidArray)) return false
        if (!timeArray.contentEquals(other.timeArray)) return false
        if (!macArray.contentEquals(other.macArray)) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = eccArray.contentHashCode()
        result = 31 * result + ebidArray.contentHashCode()
        result = 31 * result + timeArray.contentHashCode()
        result = 31 * result + macArray.contentHashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}