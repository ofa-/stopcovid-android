package com.lunabeestudio.stopcovid.fastitem

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.github.mikephil.charting.data.CombinedData
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureChartCardBinding
import com.lunabeestudio.stopcovid.extension.setupStyle
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class CompareFigureChartItem : AbstractBindingItem<ItemKeyFigureChartCardBinding>() {
    override val type: Int = R.id.item_compare_key_figure_chart_card

    var chartExplanationLabel: String? = null
    var shareContentDescription: String? = null
    var onShareCard: ((binding: ItemKeyFigureChartCardBinding) -> Unit)? = null
    var onClickListener: View.OnClickListener? = null
    var chartData: (() -> ChartCompareFiguresData?)? = null
    var isChartAnimated: Boolean = true

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ItemKeyFigureChartCardBinding {

        return ItemKeyFigureChartCardBinding.inflate(inflater, parent, false).apply {
            keyFigureBarChart.isVisible = false
            keyFigureLineChart.isVisible = false
            keyFigureCombinedChart.isVisible = true
        }
    }

    override fun bindView(binding: ItemKeyFigureChartCardBinding, payloads: List<Any>) {
        super.bindView(binding, payloads)

        binding.apply {
            chartDescriptionTextView.text = chartExplanationLabel
            shareButton.contentDescription = shareContentDescription
            shareButton.setOnClickListener { onShareCard?.invoke(binding) }

            val datas = chartData?.invoke()

            keyFigureCombinedChart.apply {
                data = datas?.combinedData
                datas?.areMagnitudeTheSame?.let { setupStyle(!it) }
                if (isChartAnimated) {
                    animateX(Constants.Chart.X_ANIMATION_DURATION_MILLIS)
                }
            }

            // Set legend
            chartSerie1LegendTextView.text = datas?.combinedData?.dataSets?.get(0)?.label
            chartSerie2LegendTextView.text = datas?.combinedData?.dataSets?.get(1)?.label
            datas?.combinedData?.dataSets?.get(0)?.color?.let {
                chartSerie1LegendTextView.setTextColor(it)
                TextViewCompat.setCompoundDrawableTintList(chartSerie1LegendTextView, ColorStateList.valueOf(it))
            }
            datas?.combinedData?.dataSets?.get(1)?.color?.let {
                chartSerie2LegendTextView.setTextColor(it)
                TextViewCompat.setCompoundDrawableTintList(chartSerie2LegendTextView, ColorStateList.valueOf(it))
            }
        }

        binding.root.setOnClickListener(onClickListener)
    }

    override fun unbindView(binding: ItemKeyFigureChartCardBinding) {
        super.unbindView(binding)
        binding.root.setOnClickListener(null)
        unbindYAxis(binding)
        unbindXAxis(binding)
    }

    private fun unbindYAxis(binding: ItemKeyFigureChartCardBinding) {
        binding.keyFigureCombinedChart.axisLeft.resetAxisMinimum()
    }

    private fun unbindXAxis(binding: ItemKeyFigureChartCardBinding) {
        binding.keyFigureCombinedChart.xAxis.removeAllLimitLines()
        binding.keyFigureCombinedChart.xAxis.resetAxisMinimum()
        binding.keyFigureCombinedChart.xAxis.resetAxisMaximum()
    }

    data class ChartCompareFiguresData(
        val combinedData: CombinedData?,
        val areMagnitudeTheSame: Boolean
    )
}

fun compareFigureCardChartItem(block: (CompareFigureChartItem.() -> Unit)): CompareFigureChartItem = CompareFigureChartItem().apply(
    block
)
