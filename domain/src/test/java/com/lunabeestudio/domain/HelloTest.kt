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

import com.google.common.truth.Truth.assertThat
import com.googlecode.zohhak.api.TestWith
import com.googlecode.zohhak.api.runners.ZohhakRunner
import com.lunabeestudio.domain.model.Hello
import org.junit.Assert
import org.junit.runner.RunWith

@RunWith(ZohhakRunner::class)
class HelloTest {
    @TestWith(
        coercers = [DomainCoercion::class],
        value = [
            "70.83.84.79.80.67.79.86.49.-39.-104.-4.-60.-31.-33.-127, 55704"]
    )
    fun `time given data should return expected`(data: ByteArray, expected: Int) {
        val hello = Hello(data)
        assertThat(hello.time).isEqualTo(expected)
    }

    @TestWith(
        coercers = [DomainCoercion::class],
        value = [
            "70.83.84.79.80.67.79.86.49.-39.-104.-4.-60.-31.-33.-127, 70, 83.84.79.80.67.79.86.49, -39.-104, -4.-60.-31.-33.-127"]
    )
    fun `ecc, ebid, time and mac given data should return expected`(data: ByteArray,
        expectedEcc: ByteArray,
        expectedEbid: ByteArray,
        expectedTime: ByteArray,
        expectedMac: ByteArray) {

        val hello = Hello(data)

        assertThat(hello.eccArray).isEqualTo(expectedEcc)
        assertThat(hello.ebidArray).isEqualTo(expectedEbid)
        assertThat(hello.timeArray).isEqualTo(expectedTime)
        assertThat(hello.macArray).isEqualTo(expectedMac)
    }

    @TestWith(
        coercers = [DomainCoercion::class],
        value = ["11.22"]
    )
    fun `ctor given too short data should throw illegal argument exception`(data: ByteArray) {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            Hello(data)
        }
    }
}