package com.lunabeestudio.stopcovid.extension

import android.content.Context
import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.Constants.Chart.WIDGET_CIRCLE_SIZE
import com.lunabeestudio.stopcovid.Constants.Chart.WIDGET_LINE_WIDTH
import com.lunabeestudio.stopcovid.Constants.Chart.WIDGET_MARGIN_SIZE
import com.lunabeestudio.stopcovid.model.LimitLineData
import kotlin.time.Duration.Companion.seconds

// Chart extensions :
fun LineDataSet.setupStyle(lineColor: Int) {
    setDrawValues(false)
    val circleCount = values.size
    circleRadius = radius(circleCount)
    lineWidth = lineWidth(circleCount)
    color = lineColor
    setCircleColor(color)
    setDrawCircleHole(false)
}

fun LineDataSet.setupStyleWidget(lineColor: Int) {
    setDrawValues(false)
    circleRadius = WIDGET_CIRCLE_SIZE
    lineWidth = WIDGET_LINE_WIDTH
    color = lineColor
    setCircleColor(color)
    setDrawCircleHole(false)
}

fun BarDataSet.setupStyle(color: Int) {
    setDrawValues(false)
    barBorderWidth = 0f
    colors = List(values.size) { color }
}

fun LineChart.setupStyle() {
    legend.isEnabled = false
    description.isEnabled = false
    setTouchEnabled(false)
    extraBottomOffset = Constants.Chart.EXTRA_BOTTOM_OFFSET

    axisRight.apply {
        isEnabled = false
    }
}

fun LineChart.setupStyleWidget() {
    legend.isEnabled = false
    description.isEnabled = false
    setTouchEnabled(false)
    setViewPortOffsets(0f, 0f, 0f, 0f)
    contentRect.set(WIDGET_MARGIN_SIZE, 0f, this.width.toFloat() - WIDGET_MARGIN_SIZE, this.height.toFloat())
    axisRight.isEnabled = false
    xAxis.isEnabled = false
    axisLeft.isEnabled = false
}

fun LineChart.fillWithChartData(
    context: Context,
    chartData: Array<com.lunabeestudio.stopcovid.model.ChartData>,
    limitLineData: LimitLineData?,
) {
    resetMinMaxAxis()
    val dataSetArray = chartData.map {
        LineDataSet(it.entries, it.description).apply {
            setupStyle(it.color)
        }
    }.toTypedArray()
    dataSetArray.forEach { it.setDrawHighlightIndicators(false) }

    data = LineData(*dataSetArray)

    setupXAxis(context, xAxis, chartData)
    setupYAxis(axisLeft, limitLineData, context)
    animateX(Constants.Chart.X_ANIMATION_DURATION_MILLIS)
}

fun LineChart.resetMinMaxAxis() {
    xAxis.resetAxisMinimum()
    xAxis.resetAxisMaximum()
    axisLeft.resetAxisMinimum()
    axisLeft.resetAxisMaximum()
}

fun BarChart.setupStyle() {
    legend.isEnabled = false
    description.isEnabled = false
    setTouchEnabled(false)
    extraBottomOffset = Constants.Chart.EXTRA_BOTTOM_OFFSET
    setScaleEnabled(false)
    axisRight.apply {
        isEnabled = false
    }
    setFitBars(true)
}

fun BarChart.fillWithChartData(
    context: Context,
    chartData: Array<com.lunabeestudio.stopcovid.model.ChartData>,
    limitLineData: LimitLineData?,
) {
    resetMinMaxAxis()
    val dataSetArray = chartData.map { (description, _, entries, color) ->
        BarDataSet(
            entries.mapIndexed { _, entry ->
                BarEntry(entry.x, entry.y)
            },
            description
        ).apply {
            setupStyle(color)
        }
    }.toTypedArray()

    if (dataSetArray.isNotEmpty()) {
        data = BarData(*dataSetArray).apply {
            val xValueDiff = xMax - xMin
            val spacing = 0.05f
            val entriesCount = dataSetArray[0].entryCount
            barWidth = xValueDiff / (entriesCount) - (spacing * xValueDiff / (entriesCount + 1))
        }

        setupXAxis(context, xAxis, chartData)
        setupYAxis(axisLeft, limitLineData, context)

        animateY(Constants.Chart.X_ANIMATION_DURATION_MILLIS)
    }
}

// Reset Min/Max because axisMinimum = axisMinimum.coerceAtLeast(0f) of setupYAxis set custom axis to true
fun BarChart.resetMinMaxAxis() {
    xAxis.resetAxisMinimum()
    xAxis.resetAxisMaximum()
    axisLeft.resetAxisMinimum()
    axisLeft.resetAxisMaximum()
}

private fun setupYAxis(yAxis: YAxis, limitLineData: LimitLineData?, context: Context) {
    yAxis.apply {
        setupStyle()
        axisMinimum = axisMinimum.coerceAtLeast(0f)
        setValueFormatter(context)

        limitLineData?.let {
            val limitLine = LimitLine(it.limitLine.toFloat(), it.description)
            limitLine.setupStyle(it.color)
            addLimitLine(limitLine)
        }
    }
}

private fun setupXAxis(context: Context, xAxis: XAxis, chartData: Array<com.lunabeestudio.stopcovid.model.ChartData>) {
    xAxis.apply {
        setupStyle()
        val chartEntries = chartData.getOrNull(0)?.entries
        chartEntries?.firstOrNull()?.x?.let { axisMinimum = it }
        chartEntries?.lastOrNull()?.x?.let { axisMaximum = it }
        setValueFormatter(context)
    }
}

fun YAxis.setupStyle() {
    setDrawAxisLine(false)
    setDrawLabels(true)
    setDrawGridLines(true)
    gridColor = Color.LTGRAY
    setLabelCount(Constants.Chart.Y_AXIS_LABEL_COUNT, true)
    textSize = Constants.Chart.AXIS_LABEL_TEXT_SIZE
    textColor = Color.GRAY
    setDrawTopYLabelEntry(true)
    setDrawZeroLine(true)
}

fun XAxis.setupStyle() {
    position = XAxis.XAxisPosition.BOTTOM
    setDrawAxisLine(true)
    setDrawGridLines(false)
    setDrawLabels(true)
    setAvoidFirstLastClipping(true)
    setLabelCount(Constants.Chart.X_AXIS_LABEL_COUNT, true)
    textSize = Constants.Chart.AXIS_LABEL_TEXT_SIZE
    textColor = Color.GRAY
}

fun LimitLine.setupStyle(color: Int) {
    textSize = Constants.Chart.LIMIT_LINE_TEXT_SIZE
    textColor = color
    labelPosition = LimitLine.LimitLabelPosition.LEFT_TOP
    lineColor = color
    lineWidth = lineWidth(null)
    enableDashedLine(8f, 6f, 0f)
}

private fun radius(circleCount: Int?): Float {
    return circleCount?.let {
        ((Constants.Chart.DEFAULT_CIRCLE_SIZE * Constants.Chart.RESIZE_START_CIRCLE_COUNT) / it).coerceIn(
            Constants.Chart.MIN_CIRCLE_RADIUS_SIZE,
            Constants.Chart.DEFAULT_CIRCLE_SIZE
        )
    } ?: Constants.Chart.DEFAULT_CIRCLE_SIZE
}

private fun lineWidth(circleCount: Int?): Float {
    return radius(circleCount) / Constants.Chart.CIRCLE_LINE_RATIO
}

fun CombinedChart.setupStyle(isAxisRightEnabled: Boolean = true) {
    drawOrder = arrayOf(CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE)
    setTouchEnabled(false)
    legend.isEnabled = false
    description.isEnabled = false
    extraBottomOffset = Constants.Chart.EXTRA_BOTTOM_OFFSET
    extraTopOffset = Constants.Chart.EXTRA_BOTTOM_OFFSET

    xAxis.setupStyle()
    xAxis.setValueFormatter(context)

    // Display value when both figure have data
    data?.let { data ->
        val xMin = data.dataSets.maxOf { it.xMin }
        xAxis.axisMaximum = (data.xMax) + data.barData.barWidth
        xAxis.axisMinimum = xMin - data.barData.barWidth

        axisLeft.axisMinimum = axisLeft.axisMinimum.coerceAtLeast(0f)
        axisLeft.setupStyle()
        axisLeft.setValueFormatter(context)

        if (isAxisRightEnabled) {
            axisLeft.axisMaximum = data.dataSets[0].yMax
            axisLeft.textColor = data.dataSets[0].color

            axisRight.setValueFormatter(context)
            axisRight.setupStyle()
            axisRight.textColor = data.dataSets[1].color
            axisRight.axisMaximum = data.dataSets[1].yMax
            axisRight.axisMinimum = axisRight.axisMinimum.coerceAtLeast(0f)

            data.dataSets[1].axisDependency = YAxis.AxisDependency.RIGHT
        } else {
            axisRight.isEnabled = false
        }
    }
}

private fun YAxis.setValueFormatter(context: Context) {
    valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return value.formatCompact(context)
        }
    }
}

private fun XAxis.setValueFormatter(context: Context) {
    valueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return value.toLong().seconds.getRelativeDateShortString(context)
        }
    }
}