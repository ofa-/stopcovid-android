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

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.lunabeestudio.robert.extension.safeEnumValueOf
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.fastitem.KeyFigureCardItem
import com.lunabeestudio.stopcovid.fastitem.keyFigureCardItem
import com.lunabeestudio.stopcovid.model.DepartmentKeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigureCategory
import com.lunabeestudio.stopcovid.model.KeyFigureChartType
import com.lunabeestudio.stopcovid.model.KeyFigureSeriesItem
import keynumbers.Keynumbers
import java.text.NumberFormat
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

val KeyFigure.labelStringKey: String
    get() = "$labelKey.label"

val KeyFigure.labelShortStringKey: String
    get() = "$labelKey.shortLabel"

val KeyFigure.descriptionStringKey: String
    get() = "$labelKey.description"

val KeyFigure.learnMoreStringKey: String
    get() = "$labelKey.learnMore"

val KeyFigure.limitLineStringKey: String
    get() = "$labelKey.limitLine"

fun KeyFigure.colorStringKey(dark: Boolean?): String = if (dark == true) {
    "$labelKey.colorCode.dark"
} else {
    "$labelKey.colorCode.light"
}

fun KeyFigure.hasAverageChart(): Boolean = !avgSeries.isNullOrEmpty()

fun KeyFigure.getKeyFigureForPostalCode(postalCode: String?): DepartmentKeyFigure? {
    val key = postalCode?.let { KeyFigure.getDepartmentKeyFromPostalCode(it) }
    return valuesDepartments?.firstOrNull { it.dptNb == key }
}

fun List<KeyFigure>?.postalCodeExists(postalCode: String): Boolean {
    return this?.any {
        it.getKeyFigureForPostalCode(postalCode) != null
    } ?: false
}

fun List<KeyFigure>?.getDepartmentLabel(postalCode: String?): String? {
    val localization = this?.lastOrNull()?.getKeyFigureForPostalCode(
        postalCode
    ) ?: this?.firstOrNull()?.getKeyFigureForPostalCode(
        postalCode
    )

    return localization?.dptLabel
}

@OptIn(ExperimentalTime::class)
fun KeyFigure.itemForFigure(
    context: Context,
    sharedPrefs: SharedPreferences,
    departmentKeyFigure: DepartmentKeyFigure?,
    numberFormat: NumberFormat,
    strings: LocalizedStrings,
    block: (KeyFigureCardItem.() -> Unit)
): KeyFigureCardItem? {
    return keyFigureCardItem {
        val extractDate: Long
        if (departmentKeyFigure != null) {
            rightLocation = strings["common.country.france"]
            leftLocation = departmentKeyFigure.dptLabel
            leftValue = departmentKeyFigure.valueToDisplay?.formatNumberIfNeeded(numberFormat)
            rightValue = valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
            extractDate = departmentKeyFigure.extractDate
        } else {
            if (sharedPrefs.hasChosenPostalCode) {
                leftLocation = strings["common.country.france"]
            }
            leftValue = valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
            extractDate = this@itemForFigure.extractDate
        }
        updatedAt = strings.stringsFormat(
            "keyFigures.update",
            Duration.seconds(extractDate).getRelativeDateTimeString(context, strings["common.justNow"])
        )

        label = strings[labelStringKey]
        description = strings[descriptionStringKey]
        identifier = labelKey.hashCode().toLong()

        strings[colorStringKey(context.isNightMode())]?.let {
            color = Color.parseColor(it)
        }
    }
        .apply(block)
        .takeIf {
            it.label != null
        }
}

fun Keynumbers.KeyNumbersMessage.toKeyFigures(): List<KeyFigure> = keyfigureListList.map {
    it.toKeyFigure()
}

fun Keynumbers.KeyNumbersMessage.KeyfigureMessage.toKeyFigure(): KeyFigure = KeyFigure(
    safeEnumValueOf<KeyFigureCategory>(this.category) ?: KeyFigureCategory.UNKNOWN,
    this.labelKey,
    this.valueGlobalToDisplay,
    this.valueGlobal,
    this.isFeatured,
    this.isHighlighted,
    this.extractDate.toLong(),
    this.valuesDepartmentsList.map { departmentValuesMessage ->
        DepartmentKeyFigure(
            departmentValuesMessage.dptNb,
            departmentValuesMessage.dptLabel,
            departmentValuesMessage.extractDate.toLong(),
            departmentValuesMessage.value,
            departmentValuesMessage.valueToDisplay,
            departmentValuesMessage.seriesList.map { message ->
                message.toKeyFigureSeriesItem()
            },
        )
    },
    this.displayOnSameChart,
    this.limitLine,
    this.chartType?.takeIf { it.isNotEmpty() }?.let { safeEnumValueOf<KeyFigureChartType>(it) } ?: KeyFigureChartType.LINES,
    this.seriesList.map { message ->
        message.toKeyFigureSeriesItem()
    },
    this.avgSeriesList.map { message ->
        message.toKeyFigureSeriesItem()
    },
)

private fun Keynumbers.KeyNumbersMessage.ElementSerieMessage.toKeyFigureSeriesItem() = KeyFigureSeriesItem(
    this.date.toLong(),
    this.value
)