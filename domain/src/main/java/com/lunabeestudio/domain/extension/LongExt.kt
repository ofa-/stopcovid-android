/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.domain.extension

fun Long.unixTimeMsToNtpTimeS(): Long = (this / 1000L).unixTimeSToNtpTimeS()
fun Long.unixTimeSToNtpTimeS(): Long = this + 2208988800
fun Long.ntpTimeSToUnixTimeMs(): Long = ntpTimeSToUnixTimeS() * 1000L
fun Long.ntpTimeSToUnixTimeS(): Long = (this - 2208988800)
