/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/18 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

import android.os.Parcelable
import com.lunabeestudio.stopcovid.fragment.ChartDataType
import kotlinx.parcelize.Parcelize

@Parcelize
class ChartFullScreenData(
    val keyFigureKey: String,
    val chartDataType: ChartDataType,
    val minDate: Long,
    val keyFigureKey2: String?,
) : Parcelable