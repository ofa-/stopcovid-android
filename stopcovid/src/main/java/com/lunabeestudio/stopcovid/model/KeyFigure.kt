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
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager

data class KeyFigure(
    val category: KeyFigureCategory = KeyFigureCategory.UNKNOWN,
    val labelKey: String,
    val valueGlobalToDisplay: String,
    val valueGlobal: Double?,
    val isFeatured: Boolean,
    val extractDate: Long,
    val valuesDepartments: List<DepartmentKeyFigure>?,
    val trend: Int?
)

data class DepartmentKeyFigure(
    val dptNb: String,
    val dptLabel: String,
    val extractDate: Long,
    val value: Double,
    val valueToDisplay: String?,
    val color: String,
    val trend: Int?
)

enum class Trend(@DrawableRes val imageRes: Int, val hint: String?) {
    UP(R.drawable.ic_up, StringsManager.strings["accessibility.hint.keyFigure.valueUp"]),
    STEADY(R.drawable.ic_steady, StringsManager.strings["accessibility.hint.keyFigure.valueSteady"]),
    DOWN(R.drawable.ic_down, StringsManager.strings["accessibility.hint.keyFigure.valueDown"]),
}

enum class KeyFigureCategory {
    @SerializedName("health")
    HEALTH,

    @SerializedName("app")
    APP,

    UNKNOWN
}
