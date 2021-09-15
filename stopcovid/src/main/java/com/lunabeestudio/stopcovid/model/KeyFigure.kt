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

data class KeyFigure(
    val category: KeyFigureCategory = KeyFigureCategory.UNKNOWN,
    val labelKey: String,
    val valueGlobalToDisplay: String,
    val valueGlobal: Double?,
    val isFeatured: Boolean,
    val isHighlighted: Boolean?,
    val extractDate: Long,
    val valuesDepartments: List<DepartmentKeyFigure>?,
    val displayOnSameChart: Boolean,
    val limitLine: Double?,
    val chartType: KeyFigureChartType = KeyFigureChartType.LINES,
    val series: List<KeyFigureSeriesItem>?,
    val avgSeries: List<KeyFigureSeriesItem>?,
) {
    companion object {
        private const val CORSICA_KEY: String = "20"
        private const val CORSE_DU_SUD_KEY: String = "2A"
        private const val HAUTE_CORSE_KEY: String = "2B"

        private val CORSE_DU_SUD: Array<String> = arrayOf("200", "201")
        private val OVERSEAS_FRANCE: Array<String> = arrayOf("97", "98")

        fun getDepartmentKeyFromPostalCode(postalCode: String): String {
            var key = postalCode.take(2)

            if (key == CORSICA_KEY) { // Corsica case
                key = if (postalCode.take(3) in CORSE_DU_SUD) CORSE_DU_SUD_KEY else HAUTE_CORSE_KEY
            } else if (key in OVERSEAS_FRANCE) { // Overseas France case
                key = postalCode.take(3)
            }
            return key
        }
    }
}

data class DepartmentKeyFigure(
    val dptNb: String,
    val dptLabel: String,
    val extractDate: Long,
    val value: Number,
    val valueToDisplay: String?,
    val series: List<KeyFigureSeriesItem>?
)

data class KeyFigureSeriesItem(
    val date: Long,
    val value: Number
)

enum class KeyFigureCategory(val stringCode: String) {
    HEALTH("health"),
    APP("app"),
    VACCINE("vaccine"),
    UNKNOWN("unknown")
}

enum class KeyFigureChartType {
    BARS,
    LINES
}
