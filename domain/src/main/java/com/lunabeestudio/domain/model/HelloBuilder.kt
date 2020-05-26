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

import com.lunabeestudio.domain.extension.safeDestroy
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HelloBuilder(
    private val settings: HelloSettings,
    private val ephemeralBluetoothIdentifier: EphemeralBluetoothIdentifier,
    key: ByteArray
) {
    val isValidUntil: Long = ephemeralBluetoothIdentifier.ntpEndTimeS

    private val secretKeySpec: SecretKeySpec = SecretKeySpec(key, settings.algorithm)
    private val mac: Mac = Mac.getInstance(settings.algorithm)

    init {
        mac.init(secretKeySpec)
    }

    private fun isValid(time: Long): Boolean {
        return ephemeralBluetoothIdentifier.ntpStartTimeS <= time && time < ephemeralBluetoothIdentifier.ntpEndTimeS
    }

    /**
     * Build an [Hello] with the given timestamp
     *
     * @param currentTimeMillis Unix timestamp in millis
     * @return A complete [Hello] ready to send or null if the builder is not valid for the [currentTimeMillis]
     * @throws IllegalArgumentException if the [currentTimeMillis] does not match the ephemeralBluetoothIdentifier validity frame
     */
    fun build(currentTimeMillis: Long = System.currentTimeMillis()): Hello {
        val time = currentTimeMillis.unixTimeMsToNtpTimeS()

        return if (isValid(time)) {
            val timeByteArray = byteArrayOf(
                (time shr 8).toByte(),
                time.toByte()
            )

            val message = ephemeralBluetoothIdentifier.ecc + ephemeralBluetoothIdentifier.ebid + timeByteArray
            val mac = mac.doFinal(byteArrayOf(settings.prefix) + message).copyOfRange(0, 5)

            secretKeySpec.safeDestroy()

            Hello(ephemeralBluetoothIdentifier.ecc, ephemeralBluetoothIdentifier.ebid, timeByteArray, mac)
        } else {
            throw IllegalArgumentException("The provided time is not valid for the ebid.")
        }
    }
}