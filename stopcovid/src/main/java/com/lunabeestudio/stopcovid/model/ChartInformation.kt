/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/09/13 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

import android.content.Context
import com.github.mikephil.charting.data.Entry
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLocale
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.stringsFormat
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.extension.brighterColor
import com.lunabeestudio.stopcovid.extension.colorStringKey
import com.lunabeestudio.stopcovid.extension.formatNumberIfNeeded
import com.lunabeestudio.stopcovid.extension.getRelativeDateShortString
import com.lunabeestudio.stopcovid.extension.limitLineStringKey
import com.lunabeestudio.stopcovid.extension.safeParseColor
import com.lunabeestudio.stopcovid.fragment.ChartDataType
import java.text.NumberFormat
import kotlin.time.Duration.Companion.seconds

class ChartInformation(
    context: Context,
    strings: LocalizedStrings,
    keyFigure: KeyFigure,
    chartDataType: ChartDataType,
    departmentKeyFigure: DepartmentKeyFigure?,
    minDate: Long,
) {

    var chartType: KeyFigureChartType
        private set

    var chartData: Array<ChartData>
        private set

    var limitLineData: LimitLineData? = null
        private set

    var chartExplanationLabel: String? = null
        private set

    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(context.getApplicationLocale())

    init {
        val localChartData = departmentKeyFigure?.let {
            localData(
                context = context,
                figure = keyFigure,
                strings = strings,
                departmentKeyFigure = departmentKeyFigure,
                minDate = minDate,
            )
        }
        val globalChartData = globalData(
            context = context,
            figure = keyFigure,
            strings = strings,
            isSecondary = departmentKeyFigure != null,
            minDate = minDate,
        )

        limitLineData = limitLineData(context, keyFigure, strings)
        when (chartDataType) {
            ChartDataType.MULTI -> {
                chartData = arrayOf(localChartData, globalChartData).filterNotNull().toTypedArray()
                chartType = KeyFigureChartType.LINES
                chartExplanationLabel = chartExplanationLabel(context, keyFigure, strings, chartData)
            }
            ChartDataType.LOCAL -> {
                chartData = arrayOf(localChartData).filterNotNull().toTypedArray()
                chartType = keyFigure.chartType
                chartExplanationLabel = chartExplanationLabel(
                    context,
                    keyFigure,
                    strings,
                    chartData.plus(
                        listOfNotNull(
                            globalData(context, keyFigure, strings, true, minDate)
                        )
                    ),
                )
            }
            ChartDataType.GLOBAL -> {
                chartData = arrayOf(globalChartData).filterNotNull().toTypedArray()
                chartType = keyFigure.chartType
                chartExplanationLabel = chartExplanationLabel(context, keyFigure, strings, chartData)
            }
            ChartDataType.AVERAGE -> {
                chartData = arrayOf(avgGlobalData(context, keyFigure, strings, minDate))
                chartExplanationLabel = strings.stringsFormat(
                    "keyFigureDetailController.section.evolutionAvg.subtitle",
                    strings["${keyFigure.labelKey}.label"]
                )
                chartType = KeyFigureChartType.LINES
            }
        }
    }

    private fun chartExplanationLabel(
        context: Context,
        figure: KeyFigure,
        strings: LocalizedStrings,
        chartData: Array<ChartData>,
    ): String? {
        return when {
            chartData.isNotEmpty() && chartData[0].entries.isEmpty() -> strings.stringsFormat(
                "keyFigureDetailController.section.evolution.subtitle.nodata",
                strings["${figure.labelKey}.label"]
            )
            chartData.size > 1 -> strings.stringsFormat(
                "keyFigureDetailController.section.evolution.subtitle2Charts",
                strings["${figure.labelKey}.label"],
                chartData[0].entries.lastOrNull()?.x?.toLong()?.seconds?.getRelativeDateShortString(context)
                    ?: "",
                chartData[0].entries.lastOrNull()?.y?.toString()?.addUnitKeyFigure(figure, strings)?.formatNumberIfNeeded(numberFormat),
                chartData[1].entries.lastOrNull()?.y?.toString()?.addUnitKeyFigure(figure, strings)?.formatNumberIfNeeded(numberFormat)
            )
            chartData.isNotEmpty() -> strings.stringsFormat(
                "keyFigureDetailController.section.evolution.subtitle",
                strings["${figure.labelKey}.label"],
                chartData[0].entries.lastOrNull()?.x?.toLong()?.seconds?.getRelativeDateShortString(context)
                    ?: "",
                chartData[0].entries.lastOrNull()?.y?.toString()?.addUnitKeyFigure(figure, strings)?.formatNumberIfNeeded(numberFormat)
            )
            else -> null
        }
    }

    private fun localData(
        context: Context,
        figure: KeyFigure,
        strings: LocalizedStrings,
        departmentKeyFigure: DepartmentKeyFigure,
        minDate: Long,
    ) = departmentKeyFigure.series?.let { series ->
        ChartData(
            description = departmentKeyFigure.dptLabel,
            currentValueToDisplay = departmentKeyFigure.valueToDisplay,
            entries = prepareSeries(series, minDate),
            color = strings[figure.colorStringKey(context.isNightMode())].safeParseColor()
        )
    }

    private fun globalData(
        context: Context,
        figure: KeyFigure,
        strings: LocalizedStrings,
        isSecondary: Boolean,
        minDate: Long,
    ) = figure.series?.let { series ->
        ChartData(
            description = strings["common.country.france"],
            currentValueToDisplay = figure.valueGlobalToDisplay,
            entries = prepareSeries(series, minDate),
            color = if (isSecondary) {
                strings[figure.colorStringKey(context.isNightMode())].safeParseColor().brighterColor()
            } else {
                strings[figure.colorStringKey(context.isNightMode())].safeParseColor()
            }
        )
    }

    private fun avgGlobalData(
        context: Context,
        figure: KeyFigure,
        strings: LocalizedStrings,
        minDate: Long,
    ): ChartData {
        return ChartData(
            description = strings.stringsFormat(
                "keyFigureDetailController.section.evolutionAvg.legendWithLocation",
                strings["common.country.france"]
            ),
            currentValueToDisplay = figure.valueGlobalToDisplay,
            entries = prepareSeries(figure.avgSeries ?: emptyList(), minDate),
            color = strings[figure.colorStringKey(context.isNightMode())].safeParseColor()
        )
    }

    private fun prepareSeries(series: List<KeyFigureSeriesItem>, minDate: Long) = series
        .filter { it.date > minDate }
        .sortedBy { it.date }
        .map { Entry(it.date.toFloat(), it.value.toFloat()) }

    private fun limitLineData(
        context: Context,
        figure: KeyFigure,
        strings: LocalizedStrings,
    ): LimitLineData? {
        return figure.limitLine?.takeIf { it > 0.0 }?.let {
            LimitLineData(
                it.toFloat(),
                strings[figure.limitLineStringKey],
                strings[figure.colorStringKey(context.isNightMode())].safeParseColor()
            )
        }
    }

    private fun String.addUnitKeyFigure(figure: KeyFigure, strings: LocalizedStrings): String {
        return "$this${strings["${figure.labelKey}.unit"].takeIf { it == "%" } ?: ""}"
    }
}