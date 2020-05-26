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
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import org.junit.Test

class EphemeralBluetoothIdentifierTest {

    @Test
    fun ephemeralBluetoothIdentifier_secondary_ctor() {
        val ntpStartTimeS = 0L
        val ntpEndTimeS = 1L
        val ecc = byteArrayOf(70)
        val ebid = byteArrayOf(83, 84, 79, 80, 67, 79, 86, 49)
        val eccString = "Rg=="
        val ebidString = "U1RPUENPVjE="

        val ctor1 = EphemeralBluetoothIdentifier(ntpStartTimeS, ntpEndTimeS, ecc, ebid)
        val ctor2 = EphemeralBluetoothIdentifier(ntpStartTimeS, ntpEndTimeS, eccString, ebidString)

        assertThat(ctor1).isEqualTo(ctor2)
    }
}