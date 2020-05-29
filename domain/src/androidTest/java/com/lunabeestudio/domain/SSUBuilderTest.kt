/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/06/05 - for the STOP-COVID project
 */

package com.lunabeestudio.domain

import android.util.Base64
import com.google.common.truth.Truth.assertThat
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import com.lunabeestudio.domain.model.SSUBuilder
import com.lunabeestudio.domain.model.SSUSettings
import org.junit.Test

class SSUBuilderTest {

    @Test
    fun `build_given_ecc64_ebid64_key64_and_time_should_return_expected_mac64`() {
        val ecc64 = "eg=="
        val ebid64 = "HivsWMEbkHo="
        val key64 = "I5lqt1XfQKstC8TYw6YOVhzfwvsTnJPfHLbwj3HZzTw="
        val currentTimeMillis = 1588752561000 // 3797741361 ntp seconds

        val expectedMac64 = "0BqhnJ3k7l7oaSxdoYgOLZwJRinH7YZzk4s0yDy3RLI="

        val builder = SSUBuilder(
            settings = SSUSettings(prefix = 0b00000010),
            ephemeralBluetoothIdentifier = EphemeralBluetoothIdentifier(
                epochId = 0,
                ntpStartTimeS = 0,
                ntpEndTimeS = 0,
                ecc = Base64.decode(ecc64, Base64.NO_WRAP),
                ebid = Base64.decode(ebid64, Base64.NO_WRAP)),
            key = Base64.decode(key64, Base64.NO_WRAP))

        val mac64 = builder.build(currentTimeMillis).mac

        assertThat(mac64).isEqualTo(expectedMac64)
    }
}
