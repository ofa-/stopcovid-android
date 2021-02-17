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
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.extension.toDimensSize
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureCardBinding
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureChartCardBinding
import com.lunabeestudio.stopcovid.extension.formatCompact
import com.lunabeestudio.stopcovid.extension.getRelativeDateShortString
import com.lunabeestudio.stopcovid.extension.setupStyle
import com.lunabeestudio.stopcovid.model.ChartData
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class KeyFigureChartCardItem : AbstractBindingItem<ItemKeyFigureChartCardBinding>() {
    override val type: Int = R.id.item_key_figure_chart_card

    var chartExplanationLabel: String? = null
    var chartData: Array<ChartData> = emptyArray()
    var shareContentDescription: String? = null
    var onShareCard: ((binding: ItemKeyFigureChartCardBinding) -> Unit)? = null

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemKeyFigureChartCardBinding {
        return ItemKeyFigureChartCardBinding.inflate(inflater, parent, false)
    }

    @SuppressLint("NewApi")
    @ExperimentalTime
    override fun bindView(binding: ItemKeyFigureChartCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.chartDescriptionTextView.text = chartExplanationLabel
        binding.shareButton.contentDescription = shareContentDescription
        binding.shareButton.setOnClickListener { onShareCard?.invoke(binding) }

        binding.chartSerie1LegendTextView.setTextOrHide(chartData[0].description) {
            setTextColor(chartData[0].color)
            TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(chartData[0].color))
        }

        if (chartData.size > 1) {
            binding.chartSerie2LegendTextView.setTextOrHide(chartData[1].description) {
                setTextColor(chartData[1].color)
                TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(chartData[1].color))
            }
        } else {
            binding.chartSerie2LegendTextView.visibility = View.GONE
        }

        val dataSetArray = chartData.map {
            LineDataSet(it.entries, it.description).apply {
                setupStyle(it.color)
            }
        }.toTypedArray()

        binding.keyFigureChart.apply {
            data = LineData(*dataSetArray)
            setupStyle()

            axisLeft.apply {
                setupStyle()
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.formatCompact()
                    }
                }
            }

            xAxis.apply {
                setupStyle()
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toLong().seconds.getRelativeDateShortString(context) ?: ""
                    }
                }
            }
            updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = if (binding.chartSerie1LegendTextView.isGone && binding.chartSerie2LegendTextView.isGone) {
                    R.dimen.spacing_xlarge.toDimensSize(this@apply.context).toInt()
                } else {
                    0
                }
            }
        }
    }
}

fun keyFigureCardChartItem(block: (KeyFigureChartCardItem.() -> Unit)): KeyFigureChartCardItem = KeyFigureChartCardItem().apply(
    block
)

