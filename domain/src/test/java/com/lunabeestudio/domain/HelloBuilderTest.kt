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
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import com.lunabeestudio.domain.model.Hello
import com.lunabeestudio.domain.model.HelloBuilder
import com.lunabeestudio.domain.model.HelloSettings
import org.junit.runner.RunWith
import java.nio.ByteBuffer

@RunWith(ZohhakRunner::class)
class HelloBuilderTest {
    @TestWith(
        coercers = [DomainCoercion::class],
        value = [
            "46, 53544F50434F5631, string, 4276036795200, 70.83.84.79.80.67.79.86.49.-93.59.-49.57.-64.113.102"]
    )
    fun `build given ecc, ebid, key and time should return expected`(ecc: String,
        ebid: String,
        key: String,
        currentTimeMillis: Long,
        expected: Hello?) {

        val buffer = ByteBuffer
            .allocate(8)
            .putLong(ebid.toLong(16))

        buffer.flip()

        val builder = HelloBuilder(HelloSettings(),
            EphemeralBluetoothIdentifier(ntpStartTimeS = 6485025595,
                ntpEndTimeS = 6485025596,
                ecc = byteArrayOf(ecc.toByte(16)),
                ebid = buffer.array()),
            key.toByteArray(Charsets.UTF_8))

        val hello = builder.build(currentTimeMillis)

        assertThat(hello).isEqualTo(expected)
    }

    @TestWith(
        coercers = [DomainCoercion::class],
        value = [
            "46, 53544F50434F5631, string, 4276036796200",
            "46, 53544F50434F5631, string, 4276036794200"]
    )
    fun `build given ecc, ebid, key and bad time should throw IllegalArgumentException`(ecc: String,
        ebid: String,
        key: String,
        currentTimeMillis: Long) {

        val buffer = ByteBuffer
            .allocate(8)
            .putLong(ebid.toLong(16))

        buffer.flip()

        val builder = HelloBuilder(HelloSettings(),
            EphemeralBluetoothIdentifier(ntpStartTimeS = 6485025595,
                ntpEndTimeS = 6485025596,
                ecc = byteArrayOf(ecc.toByte(16)),
                ebid = buffer.array()),
            key.toByteArray(Charsets.UTF_8))

        var error: Exception? = null
        try {
            builder.build(currentTimeMillis)
        } catch (e: Exception) {
            error = e
        }

        assertThat(error).isNotNull()
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }
}