package com.lunabeestudio.stopcovid.manager

import com.lunabeestudio.stopcovid.Constants
import timber.log.Timber

object ChartManager {

    enum class ChartRange(val labelKey: String, val rangeMs: Long) {
        THIRTY("keyFigureDetailController.chartRange.segmentTitle.30", 30L * 86400L),
        NINETY("keyFigureDetailController.chartRange.segmentTitle.90", 90L * 86400L),
        ALL("keyFigureDetailController.chartRange.segmentTitle.1000", Long.MAX_VALUE),
    }

    fun getChartRange(position: Int, itemCount: Int) = when {
        position == 0 && itemCount == 1 -> ChartRange.ALL
        position == 0 && itemCount == 2 -> ChartRange.NINETY
        position == 1 && itemCount == 2 -> ChartRange.THIRTY
        position == 0 && itemCount == 3 -> ChartRange.ALL
        position == 1 && itemCount == 3 -> ChartRange.NINETY
        position == 2 && itemCount == 3 -> ChartRange.THIRTY
        else -> {
            Timber.e("Unexpected range at $position with $itemCount items")
            null
        }
    }

    fun getItemCount(serieSize: Int) = when (serieSize) {
        in Int.MIN_VALUE..Constants.Chart.PAGER_FIRST_TAB_THRESHOLD -> 1
        in Constants.Chart.PAGER_FIRST_TAB_THRESHOLD..Constants.Chart.PAGER_SECOND_TAB_THRESHOLD -> 2
        else -> 3
    }
}