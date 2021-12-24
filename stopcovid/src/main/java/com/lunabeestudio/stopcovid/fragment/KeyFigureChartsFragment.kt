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
import androidx.annotation.DimenRes
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.preference.PreferenceManager
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.databinding.ItemCaptionBinding
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.extension.safeEmojiSpanify
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureChartCardBinding
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.getKeyFigureForPostalCode
import com.lunabeestudio.stopcovid.extension.hasAverageChart
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.labelStringKey
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.keyFigureCardChartItem
import com.lunabeestudio.stopcovid.manager.ChartManager
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.ChartInformation
import com.lunabeestudio.stopcovid.model.KeyFigure

class KeyFigureChartsFragment : BaseFragment() {

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val keyFiguresManager: KeyFiguresManager by lazy(LazyThreadSafetyMode.NONE) {
        injectionContainer.keyFiguresManager
    }

    private var keyFigure: KeyFigure? = null
    private lateinit var rootLayout: LinearLayout

    private val minDate: Long by lazy(LazyThreadSafetyMode.NONE) {
        val rangeMs = (arguments?.getSerializable(RANGE_ARG_KEY) as? ChartManager.ChartRange ?: ChartManager.ChartRange.ALL).rangeMs
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

            context?.let { safeContext ->
                keyFigure?.let { figure ->
                    val departmentKeyFigure = figure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)
                    val chartsToDisplay = getChartTypesToDisplay(figure)

                    views += chartsToDisplay.mapIndexed { index, chartDataType ->
                        @DimenRes val marginTopRes: Int? = if (index == 0) R.dimen.spacing_medium else null
                        @DimenRes val marginBottomRes: Int =
                            if (index < chartsToDisplay.count() - 1) R.dimen.spacing_medium
                            else R.dimen.spacing_large
                        val chartInfo = ChartInformation(
                            context = safeContext,
                            strings = strings,
                            keyFigure = figure,
                            chartDataType = chartDataType,
                            departmentKeyFigure = departmentKeyFigure,
                            minDate = minDate,
                        )

                        ItemKeyFigureChartCardBinding.inflate(layoutInflater, rootLayout, false).apply {
                            this.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                                marginTopRes?.let { topMargin += resources.getDimensionPixelSize(marginTopRes) }
                                bottomMargin += resources.getDimensionPixelSize(marginBottomRes)
                            }
                            keyFigureCardChartItem {
                                chartData = chartInfo.chartData
                                chartType = chartInfo.chartType
                                limitLineData = chartInfo.limitLineData
                                chartExplanationLabel = chartInfo.chartExplanationLabel
                                shareContentDescription = strings["accessibility.hint.keyFigure.chart.share"]
                                onShareCard = { binding ->
                                    ShareManager.shareChart(this@KeyFigureChartsFragment, binding)
                                }
                                onClickListener = getChartOnClickListener(figure.labelKey, chartDataType)
                            }.bindView(this, emptyList())
                        }.root
                    }
                }
            }
        }

        return views
    }

    private fun getChartTypesToDisplay(figure: KeyFigure): List<ChartDataType> {
        val departmentKeyFigure = figure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)
        val chartsToDisplay: MutableList<ChartDataType> = mutableListOf()

        if (figure.displayOnSameChart) {
            chartsToDisplay += ChartDataType.MULTI
        } else {
            if (departmentKeyFigure != null) {
                chartsToDisplay += ChartDataType.LOCAL
            }
            chartsToDisplay += ChartDataType.GLOBAL
        }
        if (figure.hasAverageChart()) {
            chartsToDisplay += ChartDataType.AVERAGE
        }
        return chartsToDisplay
    }

    fun setTitle(title: String) {
        appCompatActivity?.supportActionBar?.title = title
    }

    private fun getChartOnClickListener(labelKey: String, chartDataType: ChartDataType): View.OnClickListener = View.OnClickListener {
        findParentFragmentByType<KeyFigureDetailsFragment>()?.findNavControllerOrNull()?.safeNavigate(
            KeyFigureDetailsFragmentDirections.actionKeyFigureDetailsFragmentToChartFullScreenActivity(
                keyFiguresKey = labelKey,
                chartDataType = chartDataType,
                minDate = minDate,
            )
        )
    }

    companion object {
        private const val LABEL_KEY_ARG_KEY = "LABEL_KEY_ARG_KEY"
        private const val RANGE_ARG_KEY = "RANGE_ARG_KEY"

        fun newInstance(labelKey: String, range: ChartManager.ChartRange?): KeyFigureChartsFragment {
            return KeyFigureChartsFragment().apply {
                arguments = bundleOf(
                    LABEL_KEY_ARG_KEY to labelKey,
                    RANGE_ARG_KEY to range,
                )
            }
        }
    }
}