package com.lunabeestudio.stopcovid.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.preference.PreferenceManager
import com.github.mikephil.charting.data.CombinedData
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.buttonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.extension.generateCombinedData
import com.lunabeestudio.stopcovid.extension.getDefaultFigureLabel1
import com.lunabeestudio.stopcovid.extension.getDefaultFigureLabel2
import com.lunabeestudio.stopcovid.extension.keyFigureCompare1
import com.lunabeestudio.stopcovid.extension.keyFigureCompare2
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.compareFigureCardChartItem
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.mikepenz.fastadapter.GenericItem

class CompareKeyFiguresFragment : MainFragment() {

    override fun getTitleKey(): String = "keyfigures.comparison.screen.title"

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    val configuration: Configuration? by lazy {
        context?.robertManager()?.configuration
    }

    private val minDate: Long

    init {
        val rangeMs = KeyFigureChartsFragment.ChartRange.ALL.rangeMs
        minDate = System.currentTimeMillis() / 1000 - rangeMs
    }

    var keyFigure1: KeyFigure? = null
    var keyFigure2: KeyFigure? = null

    private fun getChartOnClickListener(labelKey: String, labelKey2: String): View.OnClickListener = View.OnClickListener {
        findNavControllerOrNull()?.safeNavigate(
            CompareKeyFiguresFragmentDirections.actionCompareKeyFiguresFragmentToChartFullScreenActivity(
                keyFiguresKey = labelKey,
                chartDataType = ChartDataType.GLOBAL,
                minDate = minDate,
                keyFiguresKey2 = labelKey2
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val keyLabel1 = sharedPreferences.keyFigureCompare1 ?: configuration?.getDefaultFigureLabel1()
        val keyLabel2 = sharedPreferences.keyFigureCompare2 ?: configuration?.getDefaultFigureLabel2()

        // If problem fetching config.json
        if (keyLabel1 == null || keyLabel2 == null) {
            findNavControllerOrNull()?.safeNavigate(
                CompareKeyFiguresFragmentDirections.actionCompareKeyFiguresFragmentToChooseKeyFiguresCompareFragment(),
                NavOptions.Builder().setPopUpTo(R.id.compareKeyFiguresFragment, true).build()
            )
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val keyLabel1 = sharedPreferences.keyFigureCompare1 ?: configuration?.getDefaultFigureLabel1()
        val keyLabel2 = sharedPreferences.keyFigureCompare2 ?: configuration?.getDefaultFigureLabel2()

        keyFiguresManager.figures.value?.peekContent()?.let { keyFiguresResult ->
            keyFigure1 = keyFiguresResult.data?.firstOrNull { it.labelKey == keyLabel1 }
            keyFigure2 = keyFiguresResult.data?.firstOrNull { it.labelKey == keyLabel2 }

            // If the selected keyFigure doesn't not exist anymore
            if (keyFigure1 == null || keyFigure2 == null) {
                findNavControllerOrNull()?.safeNavigate(
                    CompareKeyFiguresFragmentDirections.actionCompareKeyFiguresFragmentToChooseKeyFiguresCompareFragment()
                )
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override suspend fun getItems(): List<GenericItem> {

        val items = arrayListOf<GenericItem>()
        items += captionItem {
            text = strings["keyfigures.comparison.evolution.section.subtitle"]
            identifier = "keyfigures.comparison.evolution.section.subtitle".hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
        items += compareFigureCardChartItem {
            identifier = "keyfigures.comparison.chart.footer".hashCode().toLong()
            shareContentDescription = strings["accessibility.hint.keyFigure.chart.share"]
            chartExplanationLabel = strings["keyfigures.comparison.chart.footer"]
            keyFigure1?.let { figure1 ->
                keyFigure2?.let { figure2 ->
                    onClickListener = getChartOnClickListener(figure1.labelKey, figure2.labelKey)
                }
            }
            onShareCard = { binding -> ShareManager.shareChart(this@CompareKeyFiguresFragment, binding) }
            areMagnitudeTheSame = ::areMagnitudesTheSame
            getChartData = ::getCombinedData
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }
        items += buttonItem {
            text = strings["keyfigures.comparison.keyfiguresChoice.button.title"]
            width = ViewGroup.LayoutParams.MATCH_PARENT
            onClickListener = View.OnClickListener {
                findNavControllerOrNull()?.safeNavigate(
                    CompareKeyFiguresFragmentDirections.actionCompareKeyFiguresFragmentToChooseKeyFiguresCompareFragment()
                )
            }
            identifier = "keyfigures.comparison.keyfiguresChoice.button.title".hashCode().toLong()
        }
        return items
    }

    private fun areMagnitudesTheSame(): Boolean {
        return keyFigure1?.magnitude == keyFigure2?.magnitude
    }

    private fun getCombinedData(): CombinedData? {
        keyFigure1?.let { key1 ->
            keyFigure2?.let { key2 ->
                context?.let { context ->
                    val pair = Pair(key1, key2)
                    return pair.generateCombinedData(context, strings, minDate)
                }
            }
        }
        return null
    }
}