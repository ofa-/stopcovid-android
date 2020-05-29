/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/20/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.testutils

import java.io.InputStream

object ResourcesHelper {
    fun readTestFileAsString(filename: String): String =
        this.javaClass.classLoader!!.getResourceAsStream(filename).use {
            it.bufferedReader().readText()
        }

    fun readTestFile(filename: String): ByteArray =
        this.javaClass.classLoader!!.getResourceAsStream(filename).use {
            it.readBytes()
        }
}