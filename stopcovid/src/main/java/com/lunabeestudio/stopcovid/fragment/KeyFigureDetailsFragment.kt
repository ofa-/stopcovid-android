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

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.data.Entry
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureChartCardBinding
import com.lunabeestudio.stopcovid.extension.brighterColor
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.colorStringKey
import com.lunabeestudio.stopcovid.extension.formatNumberIfNeeded
import com.lunabeestudio.stopcovid.extension.getKeyFigureForPostalCode
import com.lunabeestudio.stopcovid.extension.getRelativeDateShortString
import com.lunabeestudio.stopcovid.extension.hasAverageChart
import com.lunabeestudio.stopcovid.extension.itemForFigure
import com.lunabeestudio.stopcovid.extension.labelShortStringKey
import com.lunabeestudio.stopcovid.extension.labelStringKey
import com.lunabeestudio.stopcovid.extension.learnMoreStringKey
import com.lunabeestudio.stopcovid.extension.limitLineStringKey
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.safeParseColor
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.keyFigureCardChartItem
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.ChartData
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFigureChartType
import com.lunabeestudio.stopcovid.model.LimitLineData
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class KeyFigureDetailsFragment : KeyFigureGenericFragment() {

    private val args: KeyFigureDetailsFragmentArgs by navArgs()
    private var keyFigure: KeyFigure? = null

    override fun getTitleKey(): String {
        return keyFigure?.labelShortStringKey?.takeIf { !strings[it].isNullOrBlank() } ?: keyFigure?.labelStringKey ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        KeyFiguresManager.figures.observe(viewLifecycleOwner) {
            keyFigure = it.peekContent().first { figure ->
                figure.labelKey == args.labelKey
            }
            refreshScreen()
        }
    }

    override fun getItems(): List<GenericItem> {
        if (keyFigure == null) {
            return emptyList()
        }
        if (keyFigure?.series?.isEmpty() == true) {
            return getNoDataItems()
        }

        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
        keyFigure?.let { figure ->
            figure.itemForFigure(
                context = requireContext(),
                sharedPrefs = sharedPrefs,
                numberFormat = numberFormat,
                strings = strings,
                useDateTime = false
            ) {
                shareContentDescription = strings["accessibility.hint.keyFigure.share"]
                onShareCard = { binding ->
                    viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
                        val uri = getShareCaptureUri(binding, "$label")
                        withContext(Dispatchers.Main) {
                            val shareString = if (rightLocation == null) {
                                stringsFormat("keyFigure.sharing.national", label, leftValue)
                            } else {
                                stringsFormat("keyFigure.sharing.department", label, leftLocation, leftValue, label, rightValue)
                            }
                            ShareManager.shareImageAndText(requireContext(), uri, shareString) {
                                strings["common.error.unknown"]?.let { showErrorSnackBar(it) }
                            }
                        }
                    }
                }
            }?.let {
                items += it
            }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = "after_card_space".hashCode().toLong()
            }

            items += bigTitleItem {
                text = strings["keyFigureDetailController.section.evolution.title"]
                identifier = "evolution_title".hashCode().toLong()
            }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = "after_evolution_title_space".hashCode().toLong()
            }

            val departmentKeyFigure = figure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)
            if (figure.displayOnSameChart) {
                items += keyFigureCardChartItem {
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
                }
            } else {
                if (departmentKeyFigure != null) {
                    items += keyFigureCardChartItem {
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
                    }
                    items += spaceItem {
                        spaceRes = R.dimen.spacing_medium
                        identifier = "after_evolution_title_space".hashCode().toLong()
                    }
                }
                items += keyFigureCardChartItem {
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
                }
            }

            if (figure.hasAverageChart()) {
                items += keyFigureCardChartItem {
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
                }
            }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = "after_chart_space".hashCode().toLong()
            }

            if (strings[figure.learnMoreStringKey] != null) {
                items += bigTitleItem {
                    text = strings["keyFigureDetailController.section.learnmore.title"]
                    identifier = "learnmore_title".hashCode().toLong()
                }
                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = "after_learnmore_title_space".hashCode().toLong()
                }
                items += cardWithActionItem {
                    mainBody = strings[figure.learnMoreStringKey]
                }
                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = "after_learnmore_space".hashCode().toLong()
                }
            }
        }

        return items
    }

    private fun getNoDataItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = stringsFormat("keyFigureDetailController.nodata", strings[keyFigure?.labelStringKey])
            identifier = text.hashCode().toLong()
        }

        return items
    }

    private fun shareChart(binding: ItemKeyFigureChartCardBinding) {
        viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
            val uri = getShareCaptureUri(binding, Constants.Chart.SHARE_CHART_FILENAME)
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
                    entries = series
                        .sortedBy { it.date }
                        .map { Entry(it.date.toFloat(), it.value.toFloat()) },
                    color = strings[figure.colorStringKey(requireContext().isNightMode())].safeParseColor()
                )
            }
        }
    }

    private fun globalData(figure: KeyFigure, isSecondary: Boolean) = figure.series?.let { series ->
        ChartData(
            description = strings["common.country.france"],
            currentValueToDisplay = figure.valueGlobalToDisplay,
            entries = series
                .sortedBy { it.date }
                .map { Entry(it.date.toFloat(), it.value.toFloat()) },
            color = if (isSecondary) {
                strings[figure.colorStringKey(requireContext().isNightMode())].safeParseColor().brighterColor()
            } else {
                strings[figure.colorStringKey(requireContext().isNightMode())].safeParseColor()
            }
        )
    }

    private fun avgGlobalData(figure: KeyFigure) = ChartData(
        description = stringsFormat("keyFigureDetailController.section.evolutionAvg.legendWithLocation", strings["common.country.france"]),
        currentValueToDisplay = figure.valueGlobalToDisplay,
        entries = figure.avgSeries
            ?.sortedBy { it.date }
            ?.map { Entry(it.date.toFloat(), it.value.toFloat()) }
            ?: emptyList(),
        color = strings[figure.colorStringKey(requireContext().isNightMode())].safeParseColor()
    )

    private fun limitLineData(figure: KeyFigure): LimitLineData? {
        return figure.limitLine?.let {
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

    override fun showPostalCodeBottomSheet() {
        findNavControllerOrNull()?.safeNavigate(
            KeyFigureDetailsFragmentDirections.actionKeyFigureDetailsFragmentToPostalCodeBottomSheetFragment()
        )
    }
}