/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/12 - for the STOP-COVID project
 */

package com.orange.proximitynotification.tools

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

interface CoroutineContextProvider {
    val main: CoroutineContext
        get() = Dispatchers.Main
    val default: CoroutineContext
        get() = Dispatchers.Default
    val io: CoroutineContext
        get() = Dispatchers.IO

    class Default : CoroutineContextProvider
}