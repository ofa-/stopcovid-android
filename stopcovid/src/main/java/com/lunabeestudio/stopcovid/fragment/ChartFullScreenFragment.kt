/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/09/13 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceManager
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentChartFullScreenBinding
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.fillWithChartData
import com.lunabeestudio.stopcovid.extension.generateCombinedData
import com.lunabeestudio.stopcovid.extension.getKeyFigureForPostalCode
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.setLegend1FromChartData
import com.lunabeestudio.stopcovid.extension.setLegend2FromChartData
import com.lunabeestudio.stopcovid.extension.setupStyle
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.model.ChartInformation
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigureChartType
import com.lunabeestudio.stopcovid.utils.lazyFast
import com.lunabeestudio.stopcovid.viewmodel.ChartFullScreenViewModel
import com.lunabeestudio.stopcovid.widget.TacMarkerView

class ChartFullScreenFragment : BaseFragment() {

    private val viewModel: ChartFullScreenViewModel by activityViewModels()

    private val binding: FragmentChartFullScreenBinding by lazy {
        FragmentChartFullScreenBinding.inflate(layoutInflater)
    }

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val keyFiguresManager: KeyFiguresManager by lazyFast {
        injectionContainer.keyFiguresManager
    }

    private var keyFigure: KeyFigure? = null
    private var keyFigure2: KeyFigure? = null

    private val chartGestureListener = object : OnChartGestureListener {
        override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}

        override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}

        override fun onChartLongPressed(me: MotionEvent?) {}

        override fun onChartDoubleTapped(me: MotionEvent?) {
            // post because scale is done after listener is called
            binding.chartContainer.post {
                toggleZoomOutButtonIfNeeded()
            }
        }

        override fun onChartSingleTapped(me: MotionEvent?) {}

        override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}

        override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
            toggleZoomOutButtonIfNeeded()

            // Use a ZOOM_MIN_THRESHOLD to compensate for the high zoom value precision and help to zoom out
            val zoomMin = binding.keyFigureLineChart.scaleX < Constants.Chart.ZOOM_MIN_THRESHOLD
                && binding.keyFigureBarChart.scaleX < Constants.Chart.ZOOM_MIN_THRESHOLD
                && binding.keyFigureCombinedChart.scaleX < Constants.Chart.ZOOM_MIN_THRESHOLD
            if (zoomMin && scaleX < 1f) {
                binding.keyFigureLineChart.fitScreen()
                binding.keyFigureBarChart.fitScreen()
                binding.keyFigureCombinedChart.fitScreen()
                binding.zoomOutButton.isVisible = false
            }
        }

        override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}

        private fun toggleZoomOutButtonIfNeeded() {
            binding.zoomOutButton.isVisible =
                binding.keyFigureLineChart.scaleX != 1f ||
                binding.keyFigureBarChart.scaleX != 1f ||
                binding.keyFigureCombinedChart.scaleX != 1f
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.chartData.observe(viewLifecycleOwner) {
            setupObservers()
        }

        setupBarCharts()
        setupLineCharts()
        setupScreen()
    }

    private fun setupScreen() {
        binding.exitButton.apply {
            text = strings["common.close"]
            setOnClickListener {
                activity?.finish()
            }
        }
        binding.zoomOutButton.apply {
            text = strings["keyFigureChartController.zoomOut"]
            isVisible = false
            setOnClickListener {
                binding.keyFigureBarChart.fitScreen()
                binding.keyFigureLineChart.fitScreen()
                binding.keyFigureCombinedChart.fitScreen()
                isVisible = false
            }
        }
    }

    private fun setupObservers() {
        keyFiguresManager.figures.removeObservers(viewLifecycleOwner)

        keyFiguresManager.figures.observe(viewLifecycleOwner) {
            keyFigure = it.peekContent().data?.first { figure ->
                figure.labelKey == viewModel.chartData.value?.keyFigureKey
            }
            keyFigure2 = it.peekContent().data?.firstOrNull { figure ->
                figure.labelKey == viewModel.chartData.value?.keyFigureKey2
            }
            refreshScreen()
        }
    }

    private fun setupBarCharts() {
        binding.keyFigureBarChart.apply {
            legend.isEnabled = false
            description.isEnabled = false
            extraBottomOffset = Constants.Chart.EXTRA_BOTTOM_OFFSET
            axisRight.apply {
                isEnabled = false
            }
            setFitBars(true)
            isScaleXEnabled = true
            isDragXEnabled = true
            isScaleYEnabled = false
            isDragYEnabled = false
            setTouchEnabled(true)
            // chart unresponsive after drag deceleration
            isDragDecelerationEnabled = false
        }
    }

    private fun setupLineCharts() {
        binding.keyFigureLineChart.apply {
            legend.isEnabled = false
            description.isEnabled = false
            setTouchEnabled(false)
            extraBottomOffset = Constants.Chart.EXTRA_BOTTOM_OFFSET
            axisRight.apply {
                isEnabled = false
            }
            isScaleXEnabled = true
            isDragXEnabled = true
            isScaleYEnabled = false
            isDragYEnabled = false
            setTouchEnabled(true)
            isDragDecelerationEnabled = false
        }
    }

    private fun setupCombinedCharts() {
        binding.keyFigureCombinedChart.apply {
            setupStyle(keyFigure?.magnitude != keyFigure2?.magnitude)
            isScaleXEnabled = true
            isDragXEnabled = true
            isScaleYEnabled = false
            isDragYEnabled = false
            setTouchEnabled(true)
            isDragDecelerationEnabled = false
        }
    }

    override fun refreshScreen() {
        keyFigure?.let { figure ->
            context?.let { safeContext ->
                if (keyFigure2 == null) {
                    viewModel.chartData.value?.let { chartData ->
                        val chartInformation = ChartInformation(
                            context = safeContext,
                            departmentKeyFigure = figure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode),
                            strings = strings,
                            keyFigure = figure,
                            chartDataType = chartData.chartDataType,
                            minDate = chartData.minDate
                        )
                        refreshChart(safeContext, chartInformation)
                    }
                } else {
                    keyFigure2?.let { figure2 ->
                        refreshCombinedChart(safeContext, figure, figure2)
                    }
                }
            }
        }
    }

    private fun refreshChart(context: Context, chartInformation: ChartInformation) {
        binding.apply {
            keyFigureLineChart.isVisible = chartInformation.chartType == KeyFigureChartType.LINES
            keyFigureBarChart.isVisible = chartInformation.chartType == KeyFigureChartType.BARS
            keyFigureCombinedChart.isVisible = false
            if (chartInformation.chartType == KeyFigureChartType.BARS) {
                keyFigureBarChart.apply {
                    fillWithChartData(context, chartInformation.chartData, chartInformation.limitLineData)
                    marker = TacMarkerView(context, this)
                    onChartGestureListener = chartGestureListener
                }
                keyFigureLineChart.onChartGestureListener = null
            } else {
                keyFigureLineChart.apply {
                    fillWithChartData(context, chartInformation.chartData, chartInformation.limitLineData)
                    marker = TacMarkerView(context, this)
                    onChartGestureListener = chartGestureListener
                }
                keyFigureBarChart.onChartGestureListener = null
            }
            keyFigureCombinedChart.onChartGestureListener = null

            chartSerie1LegendTextView.setLegend1FromChartData(chartInformation.chartData)
            chartSerie2LegendTextView.setLegend2FromChartData(chartInformation.chartData)
            chartDescriptionTextView.setTextOrHide(chartInformation.chartExplanationLabel)
        }
    }

    private fun refreshCombinedChart(context: Context, keyFigure: KeyFigure, keyFigure2: KeyFigure) {
        viewModel.chartData.value?.let { chartData ->
            val combinedChartData = Pair(keyFigure, keyFigure2).generateCombinedData(context, strings, chartData.minDate)
            binding.apply {
                keyFigureLineChart.isVisible = false
                keyFigureBarChart.isVisible = false
                keyFigureCombinedChart.isVisible = true
                // Set legend
                chartSerie1LegendTextView.text = combinedChartData.dataSets?.get(0)?.label
                chartSerie2LegendTextView.text = combinedChartData.dataSets?.get(1)?.label
                combinedChartData.dataSets?.get(0)?.color?.let {
                    chartSerie1LegendTextView.setTextColor(it)
                    TextViewCompat.setCompoundDrawableTintList(chartSerie1LegendTextView, ColorStateList.valueOf(it))
                }
                combinedChartData.dataSets?.get(1)?.color?.let {
                    chartSerie2LegendTextView.setTextColor(it)
                    TextViewCompat.setCompoundDrawableTintList(chartSerie2LegendTextView, ColorStateList.valueOf(it))
                }

                keyFigureCombinedChart.apply {
                    data = combinedChartData
                    marker = TacMarkerView(context, this)
                    onChartGestureListener = chartGestureListener
                }
                setupCombinedCharts()
                keyFigureLineChart.onChartGestureListener = null
                keyFigureBarChart.onChartGestureListener = null
                binding.chartDescriptionTextView.isVisible = false
            }
        }
    }
}

enum class ChartDataType { LOCAL, GLOBAL, MULTI, AVERAGE }