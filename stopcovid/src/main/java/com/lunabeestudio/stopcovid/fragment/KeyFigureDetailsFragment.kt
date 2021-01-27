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

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.data.Entry
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.isNightMode
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.brighterColor
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.colorStringKey
import com.lunabeestudio.stopcovid.extension.formatNumberIfNeeded
import com.lunabeestudio.stopcovid.extension.getKeyFigureForPostalCode
import com.lunabeestudio.stopcovid.extension.getRelativeDateShortString
import com.lunabeestudio.stopcovid.extension.itemForFigure
import com.lunabeestudio.stopcovid.extension.labelShortStringKey
import com.lunabeestudio.stopcovid.extension.labelStringKey
import com.lunabeestudio.stopcovid.extension.learnMoreStringKey
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.keyFigureCardChartItem
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.ChartData
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

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

    @ExperimentalTime
    override fun getItems(): List<GenericItem> {
        if (keyFigure == null) {
            return emptyList()
        }

        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
        keyFigure?.let { figure ->
            items += figure.itemForFigure(
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
                    chartExplanationLabel = chartExplanationLabel(figure, chartData)
                }
            } else {
                if (departmentKeyFigure != null) {
                    items += keyFigureCardChartItem {
                        chartData = arrayOf(
                            localData(figure)
                        ).filterNotNull().toTypedArray()
                        chartExplanationLabel = chartExplanationLabel(figure, chartData.plus(globalData(figure, true)))
                    }
                    items += spaceItem {
                        spaceRes = R.dimen.spacing_medium
                        identifier = "after_evolution_title_space".hashCode().toLong()
                    }
                }
                items += keyFigureCardChartItem {
                    chartData = arrayOf(
                        globalData(figure, departmentKeyFigure != null)
                    )
                    chartExplanationLabel = chartExplanationLabel(figure, chartData)
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

    @ExperimentalTime
    private fun chartExplanationLabel(figure: KeyFigure, chartData: Array<ChartData>): String? {
        return when {
            chartData.size > 1 -> stringsFormat(
                "keyFigureDetailController.section.evolution.subtitle2Charts",
                strings["${figure.labelKey}.label"],
                chartData[0].entries.last().x.toLong().seconds.getRelativeDateShortString(requireContext()),
                chartData[0].currentValueToDisplay?.formatNumberIfNeeded(numberFormat),
                chartData[1].currentValueToDisplay?.formatNumberIfNeeded(numberFormat)
            )
            else -> stringsFormat(
                "keyFigureDetailController.section.evolution.subtitle",
                strings["${figure.labelKey}.label"],
                chartData[0].entries.last().x.toLong().seconds.getRelativeDateShortString(requireContext()),
                chartData[0].currentValueToDisplay?.formatNumberIfNeeded(numberFormat)
            )
        }
    }

    private fun localData(figure: KeyFigure): ChartData? {
        val departmentKeyFigure = figure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)

        return departmentKeyFigure?.let {
            ChartData(
                description = figure.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode)?.dptLabel,
                currentValueToDisplay = departmentKeyFigure.valueToDisplay,
                entries = departmentKeyFigure.series.map {
                    Entry(it.date.toFloat(), it.value.toFloat())
                },
                color = Color.parseColor(strings[figure.colorStringKey(requireContext().isNightMode())])
            )
        }
    }

    private fun globalData(figure: KeyFigure, isSecondary: Boolean) = ChartData(
        description = if (isSecondary) {
            strings["common.country.france"]
        } else {
            null
        },
        currentValueToDisplay = figure.valueGlobalToDisplay,
        entries = figure.series.map {
            Entry(it.date.toFloat(), it.value.toFloat())
        },
        color = if (isSecondary) {
            Color.parseColor(strings[figure.colorStringKey(requireContext().isNightMode())]).brighterColor()
        } else {
            Color.parseColor(strings[figure.colorStringKey(requireContext().isNightMode())])
        }
    )

    fun setTitle(title: String) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }

    override fun showPostalCodeBottomSheet() {
        findNavControllerOrNull()?.safeNavigate(KeyFigureDetailsFragmentDirections.actionKeyFigureDetailsFragmentToPostalCodeBottomSheetFragment())
    }

}