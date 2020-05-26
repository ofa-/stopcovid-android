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
import com.lunabeestudio.domain.model.Hello
import com.lunabeestudio.domain.model.LocalProximity
import org.junit.Test

class LocalProximityTest {

    @Test
    fun localProximity_secondary_ctor() {
        val collectedTime = 0L
        val rawRssi = 0
        val calibratedRssi = 0
        val helloTime = 55704
        val ecc64 = "Rg=="
        val ebid64 = "U1RPUENPVjE="
        val mac64 = "/MTh34E="
        val hello = Hello(byteArrayOf(70, 83, 84, 79, 80, 67, 79, 86, 49, -39, -104, -4, -60, -31, -33, -127))

        val ctor1 = LocalProximity(hello, collectedTime, rawRssi, calibratedRssi)
        val ctor2 = LocalProximity(ecc64, ebid64, mac64, helloTime, collectedTime, rawRssi, calibratedRssi)

        assertThat(ctor1).isEqualTo(ctor2)
    }
}
