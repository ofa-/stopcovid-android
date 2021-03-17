/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/01/15 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fastitem

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureChartCardBinding
import com.lunabeestudio.stopcovid.extension.formatCompact
import com.lunabeestudio.stopcovid.extension.getRelativeDateShortString
import com.lunabeestudio.stopcovid.extension.setupStyle
import com.lunabeestudio.stopcovid.model.ChartData
import com.lunabeestudio.stopcovid.model.KeyFigureChartType
import com.lunabeestudio.stopcovid.model.LimitLineData
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class KeyFigureChartCardItem : AbstractBindingItem<ItemKeyFigureChartCardBinding>() {
    override val type: Int = R.id.item_key_figure_chart_card

    var chartExplanationLabel: String? = null
    var chartData: Array<ChartData> = emptyArray()
    var limitLineData: LimitLineData? = null
    var shareContentDescription: String? = null
    var onShareCard: ((binding: ItemKeyFigureChartCardBinding) -> Unit)? = null
    var chartType: KeyFigureChartType? = null

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemKeyFigureChartCardBinding {
        return ItemKeyFigureChartCardBinding.inflate(inflater, parent, false)
    }

    @ExperimentalTime
    @SuppressLint("NewApi")
    override fun bindView(binding: ItemKeyFigureChartCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.chartDescriptionTextView.text = chartExplanationLabel
        binding.shareButton.contentDescription = shareContentDescription
        binding.shareButton.setOnClickListener { onShareCard?.invoke(binding) }

        if (chartData.isNotEmpty()) {
            binding.chartSerie1LegendTextView.setTextOrHide(chartData[0].description) {
                setTextColor(chartData[0].color)
                TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(chartData[0].color))
            }
        }
        binding.chartSerie1LegendTextView.isVisible = chartData.isNotEmpty()

        if (chartData.size > 1) {
            binding.chartSerie2LegendTextView.setTextOrHide(chartData[1].description) {
                setTextColor(chartData[1].color)
                TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(chartData[1].color))
            }
        }
        binding.chartSerie2LegendTextView.isVisible = chartData.size > 1

        binding.keyFigureBarChart.isVisible = false
        binding.keyFigureLineChart.isVisible = false

        if (chartType == KeyFigureChartType.BARS) {
            val dataSetArray = chartData.map { (description, _, entries, color) ->
                BarDataSet(entries.mapIndexed { _, entry ->
                    BarEntry(entry.x, entry.y)
                }, description).apply {
                    setupStyle(color)
                }
            }.toTypedArray()

            if (dataSetArray.isNotEmpty()) {
                binding.keyFigureBarChart.apply {
                    data = BarData(*dataSetArray).apply {
                        val xValueDiff = xMax - xMin
                        val spacing = 0.05f
                        val entriesCount = dataSetArray[0].entryCount
                        barWidth = xValueDiff / (entriesCount) - (spacing * xValueDiff / (entriesCount + 1))
                    }
                    setupStyle()

                    setupXAxis(binding, xAxis)
                    setupYAxis(axisLeft)
                }
                binding.keyFigureBarChart.isVisible = true
            }

        } else {
            val dataSetArray = chartData.map {
                LineDataSet(it.entries, it.description).apply {
                    setupStyle(it.color)
                }
            }.toTypedArray()

            binding.keyFigureLineChart.apply {
                data = LineData(*dataSetArray)
                setupStyle()

                setupXAxis(binding, xAxis)
                setupYAxis(axisLeft)
            }
            binding.keyFigureLineChart.isVisible = true
        }
    }

    override fun unbindView(binding: ItemKeyFigureChartCardBinding) {
        super.unbindView(binding)
        binding.keyFigureLineChart.isVisible = false
        binding.keyFigureBarChart.isVisible = false
    }

    @ExperimentalTime
    private fun setupXAxis(binding: ItemKeyFigureChartCardBinding, xAxis: XAxis) {
        xAxis.apply {
            setupStyle()
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toLong().seconds.getRelativeDateShortString(binding.root.context) ?: ""
                }
            }
        }
    }

    private fun setupYAxis(yAxis: YAxis) {
        yAxis.apply {
            setupStyle()
            removeAllLimitLines()
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.formatCompact()
                }
            }

            limitLineData?.let {
                val limitLine = LimitLine(it.limitLine.toFloat(), it.description)
                limitLine.setupStyle(it.color)
                addLimitLine(limitLine)
            }
        }
    }

}

fun keyFigureCardChartItem(block: (KeyFigureChartCardItem.() -> Unit)): KeyFigureChartCardItem = KeyFigureChartCardItem().apply(
    block
)

