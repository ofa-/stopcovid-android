/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.model.Trend

fun Int.getTrend(strings: LocalizedStrings): Trend? = when {
    this > 0 -> Trend.Up(strings["accessibility.hint.keyFigure.valueUp"])
    this == 0 -> Trend.Steady(strings["accessibility.hint.keyFigure.valueSteady"])
    this < 0 -> Trend.Down(strings["accessibility.hint.keyFigure.valueDown"])
    else -> null
}