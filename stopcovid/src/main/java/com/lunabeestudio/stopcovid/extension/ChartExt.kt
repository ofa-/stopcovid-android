package com.lunabeestudio.stopcovid.extension

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineDataSet
import com.lunabeestudio.stopcovid.Constants

// Chart extensions :
fun LineDataSet.setupStyle(lineColor: Int) {
    setDrawValues(false)
    val circleCount = values.size.toFloat()
    ((Constants.Chart.DEFAULT_CIRCLE_SIZE * Constants.Chart.RESIZE_START_CIRCLE_COUNT) / circleCount).coerceIn(
        Constants.Chart.MIN_CIRCLE_RADIUS_SIZE,
        Constants.Chart.DEFAULT_CIRCLE_SIZE
    ).let { radius ->
        circleRadius = radius
        lineWidth = radius / Constants.Chart.CIRCLE_LINE_RATIO
    }
    color = lineColor
    setCircleColor(color)
    setDrawCircleHole(false)
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
