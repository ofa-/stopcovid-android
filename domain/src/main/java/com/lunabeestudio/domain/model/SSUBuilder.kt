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
import com.lunabeestudio.domain.extension.safeDestroy
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SSUBuilder(
    private val settings: SSUSettings,
    private val ephemeralBluetoothIdentifier: EphemeralBluetoothIdentifier,
    key: ByteArray
) {
    private val secretKeySpec = SecretKeySpec(key, settings.algorithm)
    private val mac: Mac = Mac.getInstance(settings.algorithm)

    init {
        mac.init(secretKeySpec)
    }

    /**
     * Build an [ServerStatusUpdate] with the given timestamp
     *
     * @param currentTimeMillis Unix timestamp in millis
     * @return A complete [ServerStatusUpdate] ready to send
     */
    fun build(currentTimeMillis: Long = System.currentTimeMillis()): ServerStatusUpdate {
        val timeLong = currentTimeMillis.unixTimeMsToNtpTimeS()

        val timeByteArray = byteArrayOf(
            (timeLong shr 24).toByte(),
            (timeLong shr 16).toByte(),
            (timeLong shr 8).toByte(),
            timeLong.toByte()
        )

        val message = ephemeralBluetoothIdentifier.ebid + timeByteArray
        val macByteArray = mac.doFinal(byteArrayOf(settings.prefix) + message)

        secretKeySpec.safeDestroy()

        val ebid = Base64.encodeToString(ephemeralBluetoothIdentifier.ebid, Base64.NO_WRAP)
        val time = Base64.encodeToString(timeByteArray, Base64.NO_WRAP)
        val mac = Base64.encodeToString(macByteArray, Base64.NO_WRAP)

        return ServerStatusUpdate(ebid, time, mac)
    }
}