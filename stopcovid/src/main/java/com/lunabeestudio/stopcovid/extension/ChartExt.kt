package com.lunabeestudio.stopcovid.extension

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.LineDataSet

// Chart extensions :
fun LineDataSet.setupStyle(lineColor: Int) {
    setDrawValues(false)
    lineWidth = 2.0f
    circleRadius = 4.0f
    color = lineColor
    setCircleColor(color)
    setDrawCircleHole(false)
}

fun LineChart.setupStyle() {
    legend.isEnabled = false
    description.isEnabled = false
    setTouchEnabled(false)
    extraBottomOffset = 16f

    axisRight.apply {
        isEnabled = false
    }
}

fun YAxis.setupStyle() {
    setDrawAxisLine(false)
    setDrawLabels(true)
    setDrawGridLines(true)
    gridColor = android.graphics.Color.LTGRAY
    setLabelCount(3, true)
    textSize = 15F
    textColor = android.graphics.Color.GRAY
    setDrawTopYLabelEntry(true)
    setDrawZeroLine(true)
}

fun XAxis.setupStyle() {
    position = XAxis.XAxisPosition.BOTTOM
    setDrawAxisLine(true)
    setDrawGridLines(false)
    setDrawLabels(true)
    setAvoidFirstLastClipping(true)
    setLabelCount(2, true)
    textSize = 15F
    textColor = Color.GRAY
}
