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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentChartFullScreenBinding
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.fillWithChartData
import com.lunabeestudio.stopcovid.extension.getKeyFigureForPostalCode
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.setLegend1FromChartData
import com.lunabeestudio.stopcovid.extension.setLegend2FromChartData
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.model.ChartInformation
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigureChartType
import com.lunabeestudio.stopcovid.widget.TacMarkerView

class ChartFullScreenFragment : BaseFragment(), OnChartGestureListener {

    private val binding: FragmentChartFullScreenBinding by lazy {
        FragmentChartFullScreenBinding.inflate(layoutInflater)
    }

    private val args by navArgs<ChartFullScreenFragmentArgs>()

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val keyFiguresManager: KeyFiguresManager by lazy(LazyThreadSafetyMode.NONE) {
        injectionContainer.keyFiguresManager
    }

    private var keyFigure: KeyFigure? = null
    private val chartDataType: ChartDataType by lazy { args.chartDataType }
    private val keyFigureKey: String by lazy { args.keyFigureKey }
    private val minDate: Long by lazy { args.minDate }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBarCharts()
        setupLineCharts()
        setupScreen()
        setupObservers()
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
                isVisible = false
            }
        }
    }

    private fun setupObservers() {
        keyFiguresManager.figures.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }

        keyFiguresManager.figures.observe(viewLifecycleOwner) {
            keyFigure = it.peekContent().data?.first { figure ->
                figure.labelKey == keyFigureKey
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

    override fun refreshScreen() {
        keyFigure?.let { figure ->
            context?.let { safeContext ->

                val chartInformation = ChartInformation(
                    context = safeContext,
                    departmentKeyFigure = figure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode),
                    strings = strings,
                    keyFigure = figure,
                    chartDataType = chartDataType,
                    minDate = minDate
                )

                refreshChart(safeContext, chartInformation)
            }
        }
    }

    private fun refreshChart(context: Context, chartInformation: ChartInformation) {
        if (chartInformation.chartType == KeyFigureChartType.BARS) {
            binding.keyFigureBarChart.apply {
                fillWithChartData(context, chartInformation.chartData, chartInformation.limitLineData)
                marker = TacMarkerView(context, this)
                onChartGestureListener = this@ChartFullScreenFragment
            }
            binding.keyFigureLineChart.onChartGestureListener = null
        } else {
            binding.keyFigureLineChart.apply {
                fillWithChartData(context, chartInformation.chartData, chartInformation.limitLineData)
                marker = TacMarkerView(context, this)
                onChartGestureListener = this@ChartFullScreenFragment
            }
            binding.keyFigureBarChart.onChartGestureListener = null
        }

        binding.chartSerie1LegendTextView.setLegend1FromChartData(chartInformation.chartData)
        binding.chartSerie2LegendTextView.setLegend2FromChartData(chartInformation.chartData)
        binding.keyFigureLineChart.isVisible = chartInformation.chartType == KeyFigureChartType.LINES
        binding.keyFigureBarChart.isVisible = chartInformation.chartType == KeyFigureChartType.BARS
        binding.chartDescriptionTextView.text = chartInformation.chartExplanationLabel
    }

    override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}

    override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}

    override fun onChartLongPressed(me: MotionEvent?) {}

    override fun onChartDoubleTapped(me: MotionEvent?) {}

    override fun onChartSingleTapped(me: MotionEvent?) {}

    override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}

    override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
        binding.zoomOutButton.isVisible = !(binding.keyFigureLineChart.scaleX == 1f && binding.keyFigureBarChart.scaleX == 1f)

        // Use a ZOOM_MIN_THRESHOLD to compensate for the high zoom value precision and help to zoom out
        val zoomMin = binding.keyFigureLineChart.scaleX < Constants.Chart.ZOOM_MIN_THRESHOLD
            && binding.keyFigureBarChart.scaleX < Constants.Chart.ZOOM_MIN_THRESHOLD
        if (zoomMin && scaleX < 1f) {
            binding.keyFigureLineChart.fitScreen()
            binding.keyFigureBarChart.fitScreen()
            binding.zoomOutButton.isVisible = false
        }
    }

    override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
}

enum class ChartDataType { LOCAL, GLOBAL, MULTI, AVERAGE }