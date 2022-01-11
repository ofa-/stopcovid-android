package com.lunabeestudio.stopcovid.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentCompareKeyfigureBinding
import com.lunabeestudio.stopcovid.extension.getDefaultFigureLabel1
import com.lunabeestudio.stopcovid.extension.getDefaultFigureLabel2
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.keyFigureCompare1
import com.lunabeestudio.stopcovid.extension.keyFigureCompare2
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.manager.ChartManager
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.utils.lazyFast
import kotlin.math.max

class CompareKeyFiguresFragment : BaseFragment() {

    private lateinit var binding: FragmentCompareKeyfigureBinding

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    val keyFiguresManager: KeyFiguresManager by lazyFast {
        injectionContainer.keyFiguresManager
    }

    val configuration: Configuration? by lazy {
        context?.robertManager()?.configuration
    }

    var keyFigure1: KeyFigure? = null
    var keyFigure2: KeyFigure? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentCompareKeyfigureBinding.inflate(inflater, container, false)
            .also { binding = it }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val keyLabel1 = sharedPreferences.keyFigureCompare1 ?: configuration?.getDefaultFigureLabel1()
        val keyLabel2 = sharedPreferences.keyFigureCompare2 ?: configuration?.getDefaultFigureLabel2()

        keyFiguresManager.figures.value?.peekContent()?.let { keyFiguresResult ->
            keyFigure1 = keyFiguresResult.data?.firstOrNull { it.labelKey == keyLabel1 }
            keyFigure2 = keyFiguresResult.data?.firstOrNull { it.labelKey == keyLabel2 }

            // If the selected keyFigure doesn't not exist anymore
            if (keyFigure1 == null || keyFigure2 == null) {
                findNavControllerOrNull()?.safeNavigate(
                    CompareKeyFiguresFragmentDirections.actionCompareKeyFiguresFragmentToChooseKeyFiguresCompareFragment(),
                    NavOptions.Builder().setPopUpTo(R.id.compareKeyFiguresFragment, true).build()
                )
            }
        }
        refreshScreen()
    }

    override fun refreshScreen() {
        appCompatActivity?.supportActionBar?.title = strings["keyfigures.comparison.screen.title"]
        binding.apply {
            infoTextView.text = strings["keyfigures.comparison.evolution.section.subtitle"]

            chooseKeyFiguresButton.text = strings["keyfigures.comparison.keyfiguresChoice.button.title"]
            chooseKeyFiguresButton.setOnClickListener {
                findNavControllerOrNull()?.safeNavigate(
                    CompareKeyFiguresFragmentDirections.actionCompareKeyFiguresFragmentToChooseKeyFiguresCompareFragment()
                )
            }
        }
        setupPager()
    }

    private fun setupPager() {
        val maxMinDate = max(keyFigure1?.series?.get(0)?.date ?: 0L, keyFigure2?.series?.get(0)?.date ?: 0L)
        val diffTimeStamp = (keyFigure1?.series?.lastOrNull()?.date ?: 0L) - maxMinDate
        val itemCount = ChartManager.getItemCount(diffTimeStamp)
        val showTab = itemCount > 1
        binding.detailsTabLayout.isVisible = showTab
        binding.detailsViewPager.adapter = CompareKeyFiguresPagerAdapter(itemCount)
        if (showTab) {
            TabLayoutMediator(
                binding.detailsTabLayout,
                binding.detailsViewPager
            ) { tab, position ->
                val tabTitleKey = ChartManager.getChartRange(position, itemCount)?.labelKey
                tab.text = strings[tabTitleKey]
            }.attach()
        }
    }

    private inner class CompareKeyFiguresPagerAdapter(private val itemCount: Int) :
        FragmentStateAdapter(
            childFragmentManager,
            lifecycle
        ) {
        override fun getItemCount(): Int = itemCount
        override fun createFragment(position: Int): Fragment {
            return CompareKeyFiguresChartsFragment.newInstance(
                keyFigure1?.labelKey, keyFigure2?.labelKey, ChartManager.getChartRange(position, itemCount)
            )
        }
    }
}