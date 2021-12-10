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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuItemCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLanguage
import com.lunabeestudio.stopcovid.coreui.extension.setTextOrHide
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentKeyfigureDetailsBinding
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.getKeyFigureForPostalCode
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.itemForFigure
import com.lunabeestudio.stopcovid.extension.labelShortStringKey
import com.lunabeestudio.stopcovid.extension.labelStringKey
import com.lunabeestudio.stopcovid.extension.learnMoreStringKey
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.showPostalCodeDialog
import com.lunabeestudio.stopcovid.manager.KeyFiguresManager
import com.lunabeestudio.stopcovid.manager.ShareManager
import com.lunabeestudio.stopcovid.manager.VaccinationCenterManager
import com.lunabeestudio.stopcovid.model.KeyFigure
import com.lunabeestudio.stopcovid.model.KeyFiguresNotAvailableException
import com.lunabeestudio.domain.model.TacResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class KeyFigureDetailsFragment : BaseFragment() {

    private lateinit var binding: FragmentKeyfigureDetailsBinding

    private val args: KeyFigureDetailsFragmentArgs by navArgs()

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val keyFiguresManager: KeyFiguresManager by lazy(LazyThreadSafetyMode.NONE) {
        injectionContainer.keyFiguresManager
    }
    private val vaccinationCenterManager: VaccinationCenterManager by lazy(LazyThreadSafetyMode.NONE) {
        injectionContainer.vaccinationCenterManager
    }

    private var keyFigure: KeyFigure? = null
    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentKeyfigureDetailsBinding.inflate(inflater, container, false).also { detailsBinding ->
            binding = detailsBinding
            binding.detailsEvolutionTitle.textView.text = strings["keyFigureDetailController.section.evolution.title"]
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        keyFiguresManager.figures.observe(viewLifecycleOwner) { figureEvent ->
            keyFigure = figureEvent.peekContent().data?.firstOrNull { figure ->
                figure.labelKey == args.labelKey
            }

            binding.emptyLayout.emptyButton.setOnClickListener {
                (activity as? MainActivity)?.showProgress(true)
                lifecycleScope.launch(Dispatchers.IO) {
                    keyFiguresManager.onAppForeground(requireContext())
                    withContext(Dispatchers.Main) {
                        (activity as? MainActivity)?.showProgress(false)
                        refreshScreen()
                    }
                }
            }

            appCompatActivity?.supportActionBar?.title = strings[keyFigure?.labelShortStringKey] ?: keyFigure?.labelStringKey ?: ""
            refreshScreen()
            setupPager()

            binding.emptyLayout.root.isVisible = keyFigure == null
            binding.contentLayout.isVisible = keyFigure != null
        }
    }

    override fun refreshScreen() {
        binding.emptyLayout.emptyTitleTextView.text = strings["infoCenterController.noInternet.title"]
        binding.emptyLayout.emptyDescriptionTextView.text = strings["infoCenterController.noInternet.subtitle"]
        binding.emptyLayout.emptyButton.text = strings["common.retry"]

        val error = (keyFiguresManager.figures.value?.peekContent() as? TacResult.Failure)?.throwable
        binding.errorExplanationCard.root.isVisible = error is KeyFiguresNotAvailableException
        if (error is KeyFiguresNotAvailableException) {
            binding.errorExplanationCard.explanationTextView.setTextOrHide(error.getString(strings))
            binding.errorExplanationCard.bottomActionTextView.setTextOrHide(strings["keyFiguresController.fetchError.button"])
            binding.errorExplanationCard.bottomActionTextView.setOnClickListener {
                viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
                    (activity as? MainActivity)?.showProgress(true)
                    keyFiguresManager.onAppForeground(requireContext())
                    vaccinationCenterManager.postalCodeDidUpdate(
                        requireContext(),
                        sharedPrefs,
                        sharedPrefs.chosenPostalCode,
                    )
                    (activity as? MainActivity)?.showProgress(false)
                    refreshScreen()
                }
            }
        }

        keyFigure?.itemForFigure(
            requireContext(),
            sharedPrefs,
            keyFigure?.getKeyFigureForPostalCode(sharedPrefs.chosenPostalCode),
            numberFormat,
            SimpleDateFormat(UiConstants.DAY_MONTH_DATE_PATTERN, Locale(requireContext().getApplicationLanguage())),
            strings
        ) {
            shareContentDescription = stringsFormat("accessibility.hint.keyFigure.share.withLabel", strings[keyFigure?.labelStringKey])
            onShareCard = { binding ->
                viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
                    val uri = ShareManager.getShareCaptureUri(binding, "$label")
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
            this.bindView(binding.keyfigureCard, emptyList())
        }

        val showLearnMore = strings[keyFigure?.learnMoreStringKey] != null
        binding.learnMoreTitle.root.isVisible = showLearnMore
        binding.learnMoreCard.root.isVisible = showLearnMore

        if (showLearnMore) {
            binding.learnMoreTitle.textView.text = strings["keyFigureDetailController.section.learnmore.title"]
            cardWithActionItem {
                mainBody = strings[keyFigure?.learnMoreStringKey]
            }.bindView(binding.learnMoreCard, emptyList())
        }
    }

    private fun setupPager() {
        val keyFigure = keyFigure ?: return
        val serieSize = keyFigure.series?.size ?: 0

        val itemCount = when (serieSize) {
            in Int.MIN_VALUE..PAGER_FIRST_TAB_THRESHOLD -> 1
            in PAGER_FIRST_TAB_THRESHOLD..PAGER_SECOND_TAB_THRESHOLD -> 2
            else -> 3
        }

        val showTab = itemCount > 1
        binding.detailsTabLayout.isVisible = showTab
        binding.detailsViewPager.adapter = KeyFigureDetailsPagerAdapter(itemCount, keyFigure.labelKey)
        if (showTab) {
            TabLayoutMediator(binding.detailsTabLayout, binding.detailsViewPager) { tab, position ->
                val tabTitleKey = getChartRange(position, itemCount)?.labelKey
                tab.text = strings[tabTitleKey]
            }.attach()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.postal_code_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        MenuItemCompat.setContentDescription(menu.findItem(R.id.item_map), strings["home.infoSection.newPostalCode.button"])
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.item_map) {
            if (sharedPrefs.chosenPostalCode == null) {
                MaterialAlertDialogBuilder(requireContext()).showPostalCodeDialog(
                    layoutInflater = layoutInflater,
                    strings = strings,
                    baseFragment = this,
                    sharedPrefs = sharedPrefs,
                )
            } else {
                findNavControllerOrNull()?.safeNavigate(
                    KeyFigureDetailsFragmentDirections.actionKeyFigureDetailsFragmentToPostalCodeBottomSheetFragment()
                )
            }
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private inner class KeyFigureDetailsPagerAdapter(private val itemCount: Int, private val labelKey: String) : FragmentStateAdapter(
        childFragmentManager,
        lifecycle
    ) {
        override fun getItemCount(): Int = itemCount
        override fun createFragment(position: Int): Fragment {
            return KeyFigureChartsFragment.newInstance(labelKey, getChartRange(position, itemCount))
        }
    }

    private fun getChartRange(position: Int, itemCount: Int) = when {
        position == 0 && itemCount == 1 -> KeyFigureChartsFragment.ChartRange.ALL
        position == 0 && itemCount == 2 -> KeyFigureChartsFragment.ChartRange.NINETY
        position == 1 && itemCount == 2 -> KeyFigureChartsFragment.ChartRange.THIRTY
        position == 0 && itemCount == 3 -> KeyFigureChartsFragment.ChartRange.ALL
        position == 1 && itemCount == 3 -> KeyFigureChartsFragment.ChartRange.NINETY
        position == 2 && itemCount == 3 -> KeyFigureChartsFragment.ChartRange.THIRTY
        else -> {
            Timber.e("Unexpected range at $position with $itemCount items")
            null
        }
    }

    private fun showErrorSnackBar(message: String) {
        (activity as? MainActivity)?.showErrorSnackBar(message)
    }

    companion object {
        private const val PAGER_FIRST_TAB_THRESHOLD = 31
        private const val PAGER_SECOND_TAB_THRESHOLD = 91
    }
}