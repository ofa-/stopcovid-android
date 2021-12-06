/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/29/10 - for the STOP-COVID project
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
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.refreshLift
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.databinding.FragmentKeyFiguresPagerBinding
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.ratingsKeyFiguresOpening
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.showPostalCodeDialog
import com.lunabeestudio.stopcovid.model.KeyFigureCategory
import com.lunabeestudio.stopcovid.model.UnknownException
import com.lunabeestudio.stopcovid.utils.ExtendedFloatingActionButtonScrollListener

class KeyFiguresPagerFragment : BaseFragment() {

    private lateinit var viewPager: ViewPager2
    private var tabLayoutMediator: TabLayoutMediator? = null
    private var tabSelectedListener: TabLayout.OnTabSelectedListener? = null
    private lateinit var binding: FragmentKeyFiguresPagerBinding

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        sharedPrefs.ratingsKeyFiguresOpening++
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentKeyFiguresPagerBinding.inflate(inflater, container, false)
        viewPager = binding.viewPager
        setupExtendedFab()
        return binding.root
    }

    private fun setupExtendedFab() {
        binding.floatingActionButton.apply {
            text = strings["keyfigures.comparison.screen.title"]
            setOnClickListener {
                findNavControllerOrNull()?.safeNavigate(
                    KeyFiguresPagerFragmentDirections.actionKeyFiguresPagerFragmentToCompareKeyFiguresFragment()
                )
            }
        }
    }

    fun bindFabToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(ExtendedFloatingActionButtonScrollListener(binding.floatingActionButton))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
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
                    KeyFiguresPagerFragmentDirections.actionKeyFiguresPagerFragmentToPostalCodeBottomSheetFragment()
                )
            }
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun refreshScreen() {
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = true
    }

    private fun setupViewPager() {
        viewPager.adapter = KeyFiguresPagerAdapter()
        (activity as? MainActivity)?.binding?.tabLayout?.let { tabLayout ->
            tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
                    KEY_FIGURES_VACCINE_FRAGMENT_POSITION -> strings["keyFiguresController.category.vaccine"]
                    KEY_FIGURES_HEALTH_FRAGMENT_POSITION -> strings["keyFiguresController.category.health"]
                    KEY_FIGURES_APP_FRAGMENT_POSITION -> strings["keyFiguresController.category.app"]
                    else -> "Unknown tab"
                }
            }.also {
                it.attach()
            }
            tabSelectedListener = object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    view?.postDelayed(
                        {
                            val appBarLayout = (activity as? MainActivity)?.binding?.appBarLayout ?: return@postDelayed

                            // Force invalidate cached target view
                            appBarLayout.liftOnScrollTargetViewId = R.id.recycler_view

                            // Refresh current lift state
                            val selectedCategory = when (tab?.position) {
                                KEY_FIGURES_VACCINE_FRAGMENT_POSITION -> KeyFigureCategory.VACCINE
                                KEY_FIGURES_HEALTH_FRAGMENT_POSITION -> KeyFigureCategory.HEALTH
                                KEY_FIGURES_APP_FRAGMENT_POSITION -> KeyFigureCategory.APP
                                else -> null
                            }

                            val fragment = childFragmentManager.fragments.firstOrNull {
                                (it.arguments?.getSerializable(KeyFiguresFragment.CATEGORY_ARG_KEY) as? KeyFigureCategory) ==
                                    selectedCategory
                            }
                            fragment?.view
                                ?.findViewById<RecyclerView>(R.id.recycler_view)?.let { recyclerView ->
                                    appBarLayout.refreshLift(recyclerView)
                                }
                        },
                        100,
                    )
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    /* no-op */
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                    /* no-op */
                }
            }

            tabSelectedListener?.let { tabLayout.addOnTabSelectedListener(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tabLayoutMediator?.detach()
        (activity as? MainActivity)?.binding?.tabLayout?.apply {
            isVisible = false
            tabSelectedListener?.let { removeOnTabSelectedListener(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::viewPager.isInitialized) {
            viewPager.adapter = null
        }
    }

    private inner class KeyFiguresPagerAdapter : FragmentStateAdapter(childFragmentManager, lifecycle) {
        override fun getItemCount(): Int = VIEWPAGER_ITEM_COUNT

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                KEY_FIGURES_VACCINE_FRAGMENT_POSITION -> KeyFiguresFragment.newInstance(KeyFigureCategory.VACCINE)
                KEY_FIGURES_HEALTH_FRAGMENT_POSITION -> KeyFiguresFragment.newInstance(KeyFigureCategory.HEALTH)
                KEY_FIGURES_APP_FRAGMENT_POSITION -> KeyFiguresFragment.newInstance(KeyFigureCategory.APP)
                else -> throw UnknownException("No fragment for position $position")
            }
        }
    }

    companion object {
        private const val VIEWPAGER_ITEM_COUNT = 3

        private const val KEY_FIGURES_VACCINE_FRAGMENT_POSITION = 0
        private const val KEY_FIGURES_HEALTH_FRAGMENT_POSITION = 1
        private const val KEY_FIGURES_APP_FRAGMENT_POSITION = 2
    }
}
