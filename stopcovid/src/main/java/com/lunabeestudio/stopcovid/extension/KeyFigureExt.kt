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
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.lunabeestudio.robert.extension.safeEnumValueOf
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.fastitem.KeyFigureCardItem
import com.lunabeestudio.stopcovid.fastitem.keyFigureCardItem
import com.lunabeestudio.stopcovid.fragment.ChartDataType
import com.lunabeestudio.stopcovid.model.ChartInformation
import com.lunabeestudio.stopcovid.model.DepartmentKeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigureCategory
import com.lunabeestudio.stopcovid.model.KeyFigureChartType
import com.lunabeestudio.stopcovid.model.KeyFigureSeriesItem
import keynumbers.Keynumbers
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

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

val KeyFigure.unitStringKey: String
    get() = "$labelKey.unit"

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

fun KeyFigure.itemForFigure(
    context: Context,
    sharedPrefs: SharedPreferences,
    departmentKeyFigure: DepartmentKeyFigure?,
    numberFormat: NumberFormat,
    dateFormat: DateFormat,
    strings: LocalizedStrings,
    block: KeyFigureCardItem.() -> Unit
): KeyFigureCardItem? {
    return keyFigureCardItem {
        val extractDateS: Long
        if (departmentKeyFigure != null) {
            rightLocation = strings["common.country.france"]
            leftLocation = departmentKeyFigure.dptLabel
            leftValue = departmentKeyFigure.valueToDisplay?.formatNumberIfNeeded(numberFormat)
            rightValue = valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
            extractDateS = departmentKeyFigure.extractDateS
        } else {
            if (sharedPrefs.hasChosenPostalCode) {
                leftLocation = strings["common.country.france"]
            }
            leftValue = valueGlobalToDisplay.formatNumberIfNeeded(numberFormat)
            extractDateS = this@itemForFigure.extractDateS
        }

        val extractDateMs = extractDateS.seconds.inWholeMilliseconds
        val dayInMs = 1.days.inWholeMilliseconds

        val nowDaysSinceEpoch = System.currentTimeMillis() / dayInMs
        val extractDaysSinceEpoch = extractDateMs / dayInMs
        updatedAt = when (nowDaysSinceEpoch) {
            extractDaysSinceEpoch -> strings.stringsFormat("keyFigures.update.today", strings["common.today"]?.lowercase())
            extractDaysSinceEpoch + 1 -> strings.stringsFormat("keyFigures.update.today", strings["common.yesterday"]?.lowercase())
            else -> strings.stringsFormat("keyFigures.update", dateFormat.format(Date(extractDateMs)))
        }

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
    this.magnitude
)

private fun Keynumbers.KeyNumbersMessage.ElementSerieMessage.toKeyFigureSeriesItem() = KeyFigureSeriesItem(
    this.date.toLong(),
    this.value
)

private fun KeyFigure.generateBarData(figureNumber: Int, context: Context, strings: LocalizedStrings, minDate: Long): BarDataSet {
    val chartInfo = ChartInformation(
        context = context,
        strings = strings,
        keyFigure = this,
        chartDataType = ChartDataType.GLOBAL,
        departmentKeyFigure = null,
        minDate = minDate,
    )
    val dataSet = BarDataSet(
        chartInfo.chartData[0].entries.map {
            BarEntry(it.x, it.y)
        },
        this.getLegend(strings)
    )
    getColorFigureFromConfig(context, figureNumber)?.let { dataSet.setupStyle(it) }
    return dataSet
}

private fun KeyFigure.generateLineData(figureNumber: Int, context: Context, strings: LocalizedStrings, minDate: Long): LineDataSet {
    val chartInfo = ChartInformation(
        context = context,
        strings = strings,
        keyFigure = this,
        chartDataType = ChartDataType.GLOBAL,
        departmentKeyFigure = null,
        minDate = minDate,
    )
    val dataSet = LineDataSet(chartInfo.chartData[0].entries, this.getLegend(strings))
    getColorFigureFromConfig(context, figureNumber)?.let { dataSet.setupStyle(it) }
    return dataSet
}

fun Pair<KeyFigure, KeyFigure>.generateCombinedData(context: Context, strings: LocalizedStrings, minDate: Long): CombinedData {
    return CombinedData().apply {

        val keyFigure1 = first.clearSeries(second)
        val keyFigure2 = second.clearSeries(first)

        val lineData = LineData()
        val barData = BarData()
        when (keyFigure1.chartType) {
            KeyFigureChartType.LINES -> lineData.addDataSet(keyFigure1.generateLineData(0, context, strings, minDate))
            KeyFigureChartType.BARS -> barData.addDataSet(keyFigure1.generateBarData(0, context, strings, minDate))
        }
        when (keyFigure2.chartType) {
            KeyFigureChartType.LINES -> lineData.addDataSet(keyFigure2.generateLineData(1, context, strings, minDate))
            KeyFigureChartType.BARS -> barData.addDataSet(keyFigure2.generateBarData(1, context, strings, minDate))
        }
        if (barData.dataSets.isNotEmpty()) {
            barData.apply {
                val entriesCount = barData.entryCount
                val xValueDiff = xMax - xMin
                barWidth = xValueDiff / entriesCount
                if (barData.dataSetCount > 1) {
                    groupBars(xMin, 0f, 0f)
                }
            }
        }
        setData(lineData)
        setData(barData)
    }
}

private fun KeyFigure.clearSeries(keyFigure2: KeyFigure): KeyFigure {
    val newSeries = keyFigure2.series?.let { serie2 -> this.series?.filter { entry -> serie2.any { it.date == entry.date } } }
    return KeyFigure(
        this.category,
        this.labelKey,
        this.valueGlobalToDisplay,
        this.valueGlobal,
        false,
        false,
        this.extractDateS,
        null,
        false,
        null,
        this.chartType,
        newSeries,
        null,
        this.magnitude
    )
}

private fun KeyFigure.getLegend(strings: LocalizedStrings): String {
    val unit = strings[this.unitStringKey]?.let { "($it)" } ?: ""
    return "${strings[this.labelStringKey]} $unit"
}

private fun getColorFigureFromConfig(context: Context, figureNumber: Int): Int? {
    val keyFigureColor = if (figureNumber == 1) {
        context.robertManager().configuration.colorsCompareKeyFigures?.colorKeyFigure1
    } else {
        context.robertManager().configuration.colorsCompareKeyFigures?.colorKeyFigure2
    }
    val color = if (context.isNightMode()) keyFigureColor?.darkColor else keyFigureColor?.lightColor
    return color?.safeParseColor()
}