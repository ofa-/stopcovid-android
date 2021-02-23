package com.lunabeestudio.stopcovid.extension

import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.LineDataSet
import com.lunabeestudio.stopcovid.Constants

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