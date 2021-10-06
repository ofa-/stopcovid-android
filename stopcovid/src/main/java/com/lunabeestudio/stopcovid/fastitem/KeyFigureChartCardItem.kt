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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureChartCardBinding
import com.lunabeestudio.stopcovid.extension.fillWithChartData
import com.lunabeestudio.stopcovid.extension.setLegend1FromChartData
import com.lunabeestudio.stopcovid.extension.setLegend2FromChartData
import com.lunabeestudio.stopcovid.extension.setupStyle
import com.lunabeestudio.stopcovid.model.ChartData
import com.lunabeestudio.stopcovid.model.KeyFigureChartType
import com.lunabeestudio.stopcovid.model.LimitLineData
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class KeyFigureChartCardItem : AbstractBindingItem<ItemKeyFigureChartCardBinding>() {
    override val type: Int = R.id.item_key_figure_chart_card

    var chartExplanationLabel: String? = null
    var chartData: Array<ChartData> = emptyArray()
    var limitLineData: LimitLineData? = null
    var shareContentDescription: String? = null
    var onShareCard: ((binding: ItemKeyFigureChartCardBinding) -> Unit)? = null
    var chartType: KeyFigureChartType? = null
    var onClickListener: View.OnClickListener? = null

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemKeyFigureChartCardBinding {
        return ItemKeyFigureChartCardBinding.inflate(inflater, parent, false)
    }

    @SuppressLint("NewApi")
    override fun bindView(binding: ItemKeyFigureChartCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.chartDescriptionTextView.text = chartExplanationLabel
        binding.shareButton.contentDescription = shareContentDescription
        binding.shareButton.setOnClickListener { onShareCard?.invoke(binding) }

        binding.chartSerie1LegendTextView.setLegend1FromChartData(chartData)
        binding.chartSerie2LegendTextView.setLegend2FromChartData(chartData)

        binding.keyFigureBarChart.isVisible = false
        binding.keyFigureLineChart.isVisible = false

        if (chartType == KeyFigureChartType.BARS) {
            binding.keyFigureBarChart.apply {
                setupStyle()
                fillWithChartData(context, chartData, limitLineData)
                isVisible = true
            }
        } else {
            binding.keyFigureLineChart.apply {
                setupStyle()
                fillWithChartData(context, chartData, limitLineData)
                isVisible = true
            }
        }

        binding.root.setOnClickListener(onClickListener)
    }

    override fun unbindView(binding: ItemKeyFigureChartCardBinding) {
        super.unbindView(binding)
        binding.keyFigureLineChart.isVisible = false
        binding.keyFigureBarChart.isVisible = false
        binding.root.setOnClickListener(null)

        unbindYAxis(binding)
        unbindXAxis(binding)
    }

    private fun unbindYAxis(binding: ItemKeyFigureChartCardBinding) {
        binding.keyFigureLineChart.axisLeft.resetAxisMinimum()
        binding.keyFigureBarChart.axisLeft.resetAxisMinimum()
    }

    private fun unbindXAxis(binding: ItemKeyFigureChartCardBinding) {
        binding.keyFigureLineChart.xAxis.removeAllLimitLines()
        binding.keyFigureBarChart.xAxis.removeAllLimitLines()
        binding.keyFigureLineChart.xAxis.resetAxisMinimum()
        binding.keyFigureBarChart.xAxis.resetAxisMinimum()
        binding.keyFigureLineChart.xAxis.resetAxisMaximum()
        binding.keyFigureBarChart.xAxis.resetAxisMaximum()
    }
}

fun keyFigureCardChartItem(block: (KeyFigureChartCardItem.() -> Unit)): KeyFigureChartCardItem = KeyFigureChartCardItem().apply(
    block
)
