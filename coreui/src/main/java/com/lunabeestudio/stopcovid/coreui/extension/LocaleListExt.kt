/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/14/01 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.extension

import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.N)
fun LocaleList.toList(): List<Locale> {
    return (0 until size()).map { get(it) }
}