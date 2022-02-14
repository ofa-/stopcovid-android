/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/20/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.utils

object ResourcesHelper {
    fun readTestFileAsString(filename: String): String =
        this.javaClass.classLoader!!.getResourceAsStream(filename).use {
            it.bufferedReader().readText()
        }
}