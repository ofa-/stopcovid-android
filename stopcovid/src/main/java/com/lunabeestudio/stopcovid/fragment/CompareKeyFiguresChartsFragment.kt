package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.github.mikephil.charting.data.CombinedData
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.ItemKeyFigureChartCardBinding
import com.lunabeestudio.stopcovid.extension.generateCombinedData
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.compareFigureCardChartItem
import com.lunabeestudio.stopcovid.manager.ChartManager
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.model.KeyFigure

class CompareKeyFiguresChartsFragment : BaseFragment() {

    private val minDate: Long by lazy(LazyThreadSafetyMode.NONE) {
        val rangeMs = (arguments?.getSerializable(RANGE_ARG_KEY) as? ChartManager.ChartRange ?: ChartManager.ChartRange.ALL).rangeMs
        System.currentTimeMillis() / 1000 - rangeMs
    }

    val keyFiguresManager: KeyFiguresManager by lazy(LazyThreadSafetyMode.NONE) {
        injectionContainer.keyFiguresManager
    }

    var keyFigure1: KeyFigure? = null
    var keyFigure2: KeyFigure? = null

    private fun getChartOnClickListener(labelKey: String, labelKey2: String): View.OnClickListener =
        View.OnClickListener {
            parentFragment?.findNavControllerOrNull()?.safeNavigate(
                CompareKeyFiguresFragmentDirections.actionCompareKeyFiguresFragmentToChartFullScreenActivity(
                    keyFiguresKey = labelKey,
                    chartDataType = ChartDataType.GLOBAL,
                    minDate = minDate,
                    keyFiguresKey2 = labelKey2
                )
            )
        }

    override fun refreshScreen() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        keyFiguresManager.figures.value?.peekContent()?.let { keyFiguresResult ->
            keyFigure1 = keyFiguresResult.data?.firstOrNull {
                it.labelKey == arguments?.getString(
                    LABEL_KEY_ARG_KEY1
                )
            }
            keyFigure2 = keyFiguresResult.data?.firstOrNull {
                it.labelKey == arguments?.getString(
                    LABEL_KEY_ARG_KEY2
                )
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FrameLayout(inflater.context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            id = R.id.keyfigures_charts_container
        }.also {
            it.addView(getChartItem(it))
        }
    }

    private fun getChartItem(container: ViewGroup?): View {
        return ItemKeyFigureChartCardBinding.inflate(layoutInflater, container, false).apply {
            keyFigureCombinedChart.isVisible = true
            compareFigureCardChartItem {
                shareContentDescription = strings["accessibility.hint.keyFigure.chart.share"]
                chartExplanationLabel = strings["keyfigures.comparison.chart.footer"]
                keyFigure1?.let { figure1 ->
                    keyFigure2?.let { figure2 ->
                        onClickListener = getChartOnClickListener(figure1.labelKey, figure2.labelKey)
                    }
                }
                onShareCard = { binding ->
                    ShareManager.shareChart(
                        this@CompareKeyFiguresChartsFragment,
                        binding
                    )
                }
                areMagnitudeTheSame = areMagnitudesTheSame()
                chartData = getCombinedData()
            }.bindView(this, emptyList())
        }.root
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

    private fun areMagnitudesTheSame(): Boolean {
        return keyFigure1?.magnitude == keyFigure2?.magnitude
    }

    companion object {
        private const val LABEL_KEY_ARG_KEY1 = "LABEL_KEY_ARG_KEY1"
        private const val LABEL_KEY_ARG_KEY2 = "LABEL_KEY_ARG_KEY2"
        private const val RANGE_ARG_KEY = "RANGE_ARG_KEY"

        fun newInstance(labelKey1: String?, labelKey2: String?, range: ChartManager.ChartRange?): CompareKeyFiguresChartsFragment {
            return CompareKeyFiguresChartsFragment().apply {
                arguments = bundleOf(
                    LABEL_KEY_ARG_KEY1 to labelKey1,
                    LABEL_KEY_ARG_KEY2 to labelKey2,
                    RANGE_ARG_KEY to range,
                )
            }
        }
    }
}