/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/27 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import org.junit.Test
import java.util.Calendar
import java.util.Date
import kotlin.test.assertEquals

class DateExtTest {

    @Test
    fun roundTimeIntervalSince1900_interval_1() {
        val interval = 1L

        // 2099/12/31 23h59m59s999 => 2100/01/01 00h00m00s000
        assert(Date(4102444799999L).roundedTimeIntervalSince1900(interval) == 6311433599L)
        // 2000/01/01 00h00m00s000 => 2000/01/01 00h00m00s000
        assert(Date(946684800000L).roundedTimeIntervalSince1900(interval) == 3155673600L)
        // 2000/01/01 00h00m00s999 => 2000/01/01 00h00m00s000
        assert(Date(946684800999L).roundedTimeIntervalSince1900(interval) == 3155673600L)
        // 2000/01/01 00h00m01s000 => 2000/01/01 00h00m01s000
        assert(Date(946684801000L).roundedTimeIntervalSince1900(interval) == 3155673601L)
    }

    @Test
    fun roundTimeIntervalSince1900_interval_60() {
        val interval = 60L

        // 2099/12/31 23h59m59s999 => 2100/01/01 00h00m00s000
        assert(Date(4102444799999L).roundedTimeIntervalSince1900(interval) == 6311433600L)
        // 2000/01/01 00h00m00s000 => 2000/01/01 00h00m00s000
        assert(Date(946684800000L).roundedTimeIntervalSince1900(interval) == 3155673600L)
        // 1999/12/31 23h59m29s999 => 1999/12/31 23h59m00s000
        assert(Date(946684769999L).roundedTimeIntervalSince1900(interval) == 3155673540L)
        // 1999/12/31 23h59m30s000 => 2000/01/01 00h00m00s000
        assert(Date(946684770000L).roundedTimeIntervalSince1900(interval) == 3155673600L)
        // 1999/12/31 23h59m30s001 => 2000/01/01 00h00m00s000
        assert(Date(946684770001L).roundedTimeIntervalSince1900(interval) == 3155673600L)
    }

    @Test
    fun roundTimeIntervalSince1900_interval_900() {
        val interval = 900L

        // 2099/12/31 23h59m59s999 => 2100/01/01 00h00m00s000
        assert(Date(4102444799999L).roundedTimeIntervalSince1900(interval) == 6311433600L)
        // 2000/01/01 00h00m00s000 => 2000/01/01 00h00m00s000
        assert(Date(946684800000L).roundedTimeIntervalSince1900(interval) == 3155673600L)
        // 1999/12/31 23h52m29s999 => 1999/12/31 23h45m00s000
        assert(Date(946684349999L).roundedTimeIntervalSince1900(interval) == 3155672700L)
        // 1999/12/31 23h52m30s000 => 2000/01/01 00h00m00s000
        assert(Date(946684350000L).roundedTimeIntervalSince1900(interval) == 3155673600L)
        // 1999/12/31 23h52m30s001 => 2000/01/01 00h00m00s000
        assert(Date(946684350001L).roundedTimeIntervalSince1900(interval) == 3155673600L)
    }

    @Test
    fun roundTimeIntervalSince1900_interval_901() {
        val interval = 901L

        // 2099/12/31 23h59m59s999 => 2100/01/01 00h03m41s000
        assert(Date(4102444799999L).roundedTimeIntervalSince1900(interval) == 6311433821L)
        // 2000/01/01 00h00m00s000 => 1999/12/31 23h53m32s000
        assert(Date(946684800000L).roundedTimeIntervalSince1900(interval) == 3155673212L)
        // 1999/12/31 23h46m01s999 => 1999/12/31 23h38m31s000
        assert(Date(946683961999L).roundedTimeIntervalSince1900(interval) == 3155672311L)
        // 1999/12/31 23h46m02s000 => 1999/12/31 23h53m32s000
        assert(Date(946683962000L).roundedTimeIntervalSince1900(interval) == 3155673212L)
        // 1999/12/31 23h46m02s001 => 1999/12/31 23h53m32s000
        assert(Date(946683962001L).roundedTimeIntervalSince1900(interval) == 3155673212L)
    }

    @Test
    fun roundTimeIntervalSince1900_interval_3600() {
        val interval = 3600L

        // 2099/12/31 23h59m59s999 => 2100/01/01 00h00m00s000
        assert(Date(4102444799999L).roundedTimeIntervalSince1900(interval) == 6311433600L)
        // 2000/01/01 00h00m00s000 => 2000/01/01 00h00m00s000
        assert(Date(946684800000L).roundedTimeIntervalSince1900(interval) == 3155673600L)
        // 1999/12/31 23h29m59s999 => 1999/12/31 23h00m00s000
        assert(Date(946682999999L).roundedTimeIntervalSince1900(interval) == 3155670000L)
        // 1999/12/31 23h30m00s000 => 2000/01/01 00h00m00s000
        assert(Date(946683000000L).roundedTimeIntervalSince1900(interval) == 3155673600L)
        // 1999/12/31 23h30m00s001 => 2000/01/01 00h00m00s000
        assert(Date(946683000001L).roundedTimeIntervalSince1900(interval) == 3155673600L)
    }

    @Test
    fun yearsOld_before_birthday() {
        val today = Calendar.getInstance()
        today.add(Calendar.YEAR, -15)
        today.add(Calendar.DAY_OF_YEAR, 1)
        assertEquals(14, today.time.yearsOld())
    }

    @Test
    fun yearsOld_after_birthday() {
        val today = Calendar.getInstance()
        today.add(Calendar.YEAR, -15)
        today.add(Calendar.DAY_OF_YEAR, -1)
        assertEquals(15, today.time.yearsOld())
    }

    @Test
    fun yearsOld_on_birthday() {
        val today = Calendar.getInstance()
        today.add(Calendar.YEAR, -15)
        assertEquals(15, today.time.yearsOld())
    }
}