/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/01/07 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.mikephil.charting.data.Entry
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.databinding.ItemCaptionBinding
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureChartCardBinding
import com.lunabeestudio.stopcovid.extension.brighterColor
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.colorStringKey
import com.lunabeestudio.stopcovid.extension.formatNumberIfNeeded
import com.lunabeestudio.stopcovid.extension.getKeyFigureForPostalCode
import com.lunabeestudio.stopcovid.extension.getRelativeDateShortString
import com.lunabeestudio.stopcovid.extension.hasAverageChart
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.labelStringKey
import com.lunabeestudio.stopcovid.extension.limitLineStringKey
import com.lunabeestudio.stopcovid.extension.safeParseColor
import com.lunabeestudio.stopcovid.fastitem.keyFigureCardChartItem
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.ChartData
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigureChartType
import com.lunabeestudio.stopcovid.model.KeyFigureSeriesItem
import com.lunabeestudio.stopcovid.model.LimitLineData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class KeyFigureChartsFragment : BaseFragment() {

    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val keyFiguresManager: KeyFiguresManager by lazy(LazyThreadSafetyMode.NONE) {
        injectionContainer.keyFiguresManager
    }

    private var keyFigure: KeyFigure? = null
    private lateinit var rootLayout: LinearLayout

    private val minDate: Long by lazy(LazyThreadSafetyMode.NONE) {
        val rangeMs = (arguments?.getSerializable(RANGE_ARG_KEY) as? ChartRange ?: ChartRange.ALL).rangeMs
        System.currentTimeMillis() / 1000 - rangeMs
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootLayout = LinearLayout(inflater.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            id = R.id.keyfigures_charts_container
        }
        return rootLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        keyFiguresManager.figures.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }

        findNavControllerOrNull()?.currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(
            PostalCodeBottomSheetFragment.SHOULD_BE_REFRESHED_KEY
        )?.observe(viewLifecycleOwner) { shouldBeRefreshed ->
            if (shouldBeRefreshed) {
                refreshScreen()
            }
        }

        keyFiguresManager.figures.observe(viewLifecycleOwner) {
            keyFigure = it.peekContent().data?.first { figure ->
                figure.labelKey == arguments?.getString(LABEL_KEY_ARG_KEY)
            }
            refreshScreen()
        }
    }

    override fun refreshScreen() {
        rootLayout.removeAllViewsInLayout()
        getItemsView().forEach(rootLayout::addView)
    }

    fun getItemsView(): List<View> {
        val views = mutableListOf<View>()

        if (keyFigure == null) {
            return emptyList()
        }

        val resources = rootLayout.context.resources

        if (keyFigure?.series?.isEmpty() == true) {
            views += ItemCaptionBinding.inflate(layoutInflater, rootLayout, false).apply {
                root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin += resources.getDimensionPixelSize(R.dimen.spacing_large)
                }
                textView.text = stringsFormat("keyFigureDetailController.nodata", strings[keyFigure?.labelStringKey]).safeEmojiSpanify()
            }.root
        } else {

            keyFigure?.let { figure ->
                val departmentKeyFigure = figure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)

                if (figure.displayOnSameChart) {
                    views += ItemKeyFigureChartCardBinding.inflate(layoutInflater, rootLayout, false).apply {
                        this.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            topMargin += resources.getDimensionPixelSize(R.dimen.spacing_medium)
                            bottomMargin += resources.getDimensionPixelSize(R.dimen.spacing_large)
                        }
                        keyFigureCardChartItem {
                            chartData = arrayOf(
                                localData(figure),
                                globalData(figure, departmentKeyFigure != null)
                            ).filterNotNull().toTypedArray()
                            chartType = KeyFigureChartType.LINES
                            limitLineData = limitLineData(figure)
                            chartExplanationLabel = chartExplanationLabel(figure, chartData)
                            shareContentDescription = strings["accessibility.hint.keyFigure.chart.share"]
                            onShareCard = { binding ->
                                shareChart(binding)
                            }
                        }.bindView(this, emptyList())
                    }.root
                } else {
                    if (departmentKeyFigure != null) {
                        views += ItemKeyFigureChartCardBinding.inflate(layoutInflater, rootLayout, false).apply {
                            this.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                                topMargin += resources.getDimensionPixelSize(R.dimen.spacing_medium)
                                bottomMargin += resources.getDimensionPixelSize(R.dimen.spacing_medium)
                            }
                            keyFigureCardChartItem {
                                chartData = arrayOf(
                                    localData(figure)
                                ).filterNotNull().toTypedArray()
                                chartExplanationLabel = chartExplanationLabel(
                                    figure,
                                    chartData.plus(listOfNotNull(globalData(figure, true)))
                                )
                                shareContentDescription = strings["accessibility.hint.keyFigure.chart.share"]
                                onShareCard = { binding ->
                                    shareChart(binding)
                                }
                                chartType = figure.chartType
                                limitLineData = limitLineData(figure)
                            }.bindView(this, emptyList())
                        }.root
                    }
                    views += ItemKeyFigureChartCardBinding.inflate(layoutInflater, rootLayout, false).apply {
                        this.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            if (departmentKeyFigure == null) {
                                topMargin += resources.getDimensionPixelSize(R.dimen.spacing_medium)
                            }
                            bottomMargin += if (figure.hasAverageChart()) {
                                resources.getDimensionPixelSize(R.dimen.spacing_medium)
                            } else {
                                resources.getDimensionPixelSize(R.dimen.spacing_large)
                            }
                        }
                        keyFigureCardChartItem {
                            chartData = arrayOf(
                                globalData(figure, false)
                            ).filterNotNull().toTypedArray()
                            chartExplanationLabel = chartExplanationLabel(figure, chartData)
                            shareContentDescription = strings["accessibility.hint.keyFigure.chart.share"]
                            onShareCard = { binding ->
                                shareChart(binding)
                            }
                            chartType = figure.chartType
                            limitLineData = limitLineData(figure)
                        }.bindView(this, emptyList())
                    }.root
                }

                if (figure.hasAverageChart()) {
                    views += ItemKeyFigureChartCardBinding.inflate(layoutInflater, rootLayout, false).apply {
                        this.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                            bottomMargin += resources.getDimensionPixelSize(R.dimen.spacing_large)
                        }
                        keyFigureCardChartItem {
                            chartData = arrayOf(
                                avgGlobalData(figure)
                            )
                            chartExplanationLabel = stringsFormat(
                                "keyFigureDetailController.section.evolutionAvg.subtitle",
                                strings["${figure.labelKey}.label"]
                            )
                            shareContentDescription = strings["accessibility.hint.keyFigure.chart.share"]
                            onShareCard = { binding ->
                                shareChart(binding)
                            }
                            limitLineData = limitLineData(figure)
                            chartType = KeyFigureChartType.LINES
                        }.bindView(this, emptyList())
                    }.root
                }
            }
        }

        return views
    }

    private fun shareChart(binding: ItemKeyFigureChartCardBinding) {
        viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
            val uri = ShareManager.getShareCaptureUri(binding, Constants.Chart.SHARE_CHART_FILENAME)
            withContext(Dispatchers.Main) {
                ShareManager.shareImageAndText(requireContext(), uri, null) {
                    strings["common.error.unknown"]?.let { showErrorSnackBar(it) }
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun chartExplanationLabel(figure: KeyFigure, chartData: Array<ChartData>): String? {
        return when {
            chartData.isNotEmpty() && chartData[0].entries.isEmpty() -> stringsFormat(
                "keyFigureDetailController.section.evolution.subtitle.nodata",
                strings["${figure.labelKey}.label"]
            )
            chartData.size > 1 -> stringsFormat(
                "keyFigureDetailController.section.evolution.subtitle2Charts",
                strings["${figure.labelKey}.label"],
                chartData[0].entries.lastOrNull()?.x?.toLong()?.let { Duration.seconds(it) }?.getRelativeDateShortString(requireContext())
                    ?: "",
                chartData[0].currentValueToDisplay?.formatNumberIfNeeded(numberFormat),
                chartData[1].currentValueToDisplay?.formatNumberIfNeeded(numberFormat)
            )
            chartData.isNotEmpty() -> stringsFormat(
                "keyFigureDetailController.section.evolution.subtitle",
                strings["${figure.labelKey}.label"],
                chartData[0].entries.lastOrNull()?.x?.toLong()?.let { Duration.seconds(it) }?.getRelativeDateShortString(requireContext())
                    ?: "",
                chartData[0].currentValueToDisplay?.formatNumberIfNeeded(numberFormat)
            )
            else -> null
        }
    }

    private fun localData(figure: KeyFigure): ChartData? {
        val departmentKeyFigure = figure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)
        return departmentKeyFigure?.let {
            departmentKeyFigure.series?.let { series ->
                ChartData(
                    description = figure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)?.dptLabel,
                    currentValueToDisplay = departmentKeyFigure.valueToDisplay,
                    entries = prepareSerie(series),
                    color = strings[figure.colorStringKey(requireContext().isNightMode())].safeParseColor()
                )
            }
        }
    }

    private fun globalData(figure: KeyFigure, isSecondary: Boolean) = figure.series?.let { series ->
        ChartData(
            description = strings["common.country.france"],
            currentValueToDisplay = figure.valueGlobalToDisplay,
            entries = prepareSerie(series),
            color = if (isSecondary) {
                strings[figure.colorStringKey(requireContext().isNightMode())].safeParseColor().brighterColor()
            } else {
                strings[figure.colorStringKey(requireContext().isNightMode())].safeParseColor()
            }
        )
    }

    private fun avgGlobalData(figure: KeyFigure): ChartData {
        return ChartData(
            description = stringsFormat(
                "keyFigureDetailController.section.evolutionAvg.legendWithLocation",
                strings["common.country.france"]
            ),
            currentValueToDisplay = figure.valueGlobalToDisplay,
            entries = prepareSerie(figure.avgSeries ?: emptyList()),
            color = strings[figure.colorStringKey(requireContext().isNightMode())].safeParseColor()
        )
    }

    private fun prepareSerie(series: List<KeyFigureSeriesItem>) = series
        .filter { it.date > minDate }
        .sortedBy { it.date }
        .map { Entry(it.date.toFloat(), it.value.toFloat()) }

    private fun limitLineData(figure: KeyFigure): LimitLineData? {
        return figure.limitLine?.takeIf { it > 0.0 }?.let {
            LimitLineData(
                it.toFloat(),
                strings[figure.limitLineStringKey],
                strings[figure.colorStringKey(requireContext().isNightMode())].safeParseColor()
            )
        }
    }

    fun setTitle(title: String) {
        appCompatActivity?.supportActionBar?.title = title
    }

    private fun showErrorSnackBar(message: String) {
        (activity as? MainActivity)?.showErrorSnackBar(message)
    }

    enum class ChartRange(val labelKey: String, val rangeMs: Long) {
        THIRTY("keyFigureDetailController.chartRange.segmentTitle.30", 30L * 86400L),
        NINETY("keyFigureDetailController.chartRange.segmentTitle.90", 90L * 86400L),
        ALL("keyFigureDetailController.chartRange.segmentTitle.1000", Long.MAX_VALUE),
    }

    companion object {
        private const val LABEL_KEY_ARG_KEY = "LABEL_KEY_ARG_KEY"
        private const val RANGE_ARG_KEY = "RANGE_ARG_KEY"

        fun newInstance(labelKey: String, range: ChartRange?): KeyFigureChartsFragment {
            return KeyFigureChartsFragment().apply {
                arguments = bundleOf(
                    LABEL_KEY_ARG_KEY to labelKey,
                    RANGE_ARG_KEY to range,
                )
            }
        }
    }
}