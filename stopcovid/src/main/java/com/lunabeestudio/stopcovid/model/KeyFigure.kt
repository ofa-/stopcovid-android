/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

import androidx.annotation.DrawableRes
import com.google.gson.annotations.SerializedName
import com.lunabeestudio.stopcovid.R

data class KeyFigure(
    val category: KeyFigureCategory = KeyFigureCategory.UNKNOWN,
    val labelKey: String,
    val valueGlobalToDisplay: String,
    val valueGlobal: Double?,
    val isFeatured: Boolean,
    val isHighlighted: Boolean?,
    val extractDate: Long,
    val valuesDepartments: List<DepartmentKeyFigure>?,
    val trend: Int?,
    val displayOnSameChart: Boolean,
    val limitLine: Number?,
    val chartType: KeyFigureChartType = KeyFigureChartType.LINES,
    val series: List<KeyFigureSeriesItem>?,
    val avgSeries: List<KeyFigureSeriesItem>?
)

data class DepartmentKeyFigure(
    val dptNb: String,
    val dptLabel: String,
    val extractDate: Long,
    val value: Number,
    val valueToDisplay: String?,
    val color: String,
    val trend: Int?,
    val series: List<KeyFigureSeriesItem>?
)

data class KeyFigureSeriesItem(
    val date: Long,
    val value: Number
)

sealed class Trend(@DrawableRes val imageRes: Int, val hint: String?) {
    class Up(hint: String?) : Trend(R.drawable.ic_up, hint)
    class Steady(hint: String?) : Trend(R.drawable.ic_steady, hint)
    class Down(hint: String?) : Trend(R.drawable.ic_down, hint)
}

enum class KeyFigureCategory {
    @SerializedName("health")
    HEALTH,

    @SerializedName("app")
    APP,

    UNKNOWN
}

enum class KeyFigureChartType {
    @SerializedName("bars")
    BARS,
    LINES
}
