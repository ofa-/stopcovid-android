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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.refreshLift
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.extension.activityPassValidFuture
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.model.UnknownException
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import kotlinx.coroutines.launch

class WalletFullscreenPagerFragment : BaseFragment() {

    private val args: WalletFullscreenPagerFragmentArgs by navArgs()

    private val viewModel by navGraphViewModels<WalletViewModel>(R.id.nav_wallet) {
        WalletViewModelFactory(
            requireContext().robertManager(),
            injectionContainer.blacklistDCCManager,
            injectionContainer.blacklist2DDOCManager,
            injectionContainer.walletRepository,
            injectionContainer.generateActivityPassUseCase,
        )
    }

    private lateinit var viewPager: ViewPager2
    private var tabLayoutMediator: TabLayoutMediator? = null
    private var tabSelectedListener: TabLayout.OnTabSelectedListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewPager = ViewPager2(inflater.context)
        viewPager.id = R.id.wallet_pager
        return viewPager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshPager()
    }

    override fun refreshScreen() {
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = true
    }

    private fun setupViewPager(pagerState: PagerState) {
        viewPager.adapter = WalletPagerAdapter(pagerState)
        (activity as? MainActivity)?.binding?.tabLayout?.let { tabLayout ->
            tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
                    WALLET_FULLSCREEN_ACTIVITY_PASS_POSITION -> strings["europeanCertificate.fullscreen.type13.activityPass"]
                    WALLET_FULLSCREEN_BORDER_POSITION -> strings["europeanCertificate.fullscreen.type13.border"]
                    else -> throw UnknownException("No fragment for position $position")
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
                            val fragment = when (tabLayout.selectedTabPosition) {
                                WALLET_FULLSCREEN_ACTIVITY_PASS_POSITION ->
                                    childFragmentManager.fragments.firstOrNull { it is WalletCertificateFragment }
                                WALLET_FULLSCREEN_BORDER_POSITION ->
                                    childFragmentManager.fragments.firstOrNull { it is WalletInfoFragment }
                                else -> throw UnknownException("No fragment for position ${tabLayout.selectedTabPosition}")
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
        (activity as? MainActivity)?.binding?.tabLayout?.let { tabLayout ->
            tabSelectedListener?.let { tabLayout.removeOnTabSelectedListener(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::viewPager.isInitialized) {
            viewPager.adapter = null
        }
    }

    fun openGenerateActivityPassBottomSheet() {
        findNavControllerOrNull()?.safeNavigate(
            WalletFullscreenPagerFragmentDirections.actionWalletFullscreenPagerFragmentToGenerateActivityPassBottomSheetFragment()
        )
    }

    fun refreshPager() {
        lifecycleScope.launch {
            val activityPass = viewModel.getNotExpiredActivityPass(args.id)
            val pagerState: PagerState = when {
                activityPass == null || activityPass.activityPassValidFuture() -> PagerState.EXPLANATION
                else -> PagerState.ACTIVITY_PASS
            }
            if ((viewPager.adapter as? WalletPagerAdapter)?.pagerState != pagerState) {
                setupViewPager(pagerState)
            }
        }
    }

    fun selectBorderTab() {
        (activity as? MainActivity)?.binding?.tabLayout?.let { tabLayout ->
            tabLayout.getTabAt(WALLET_FULLSCREEN_BORDER_POSITION)?.let(tabLayout::selectTab)
        }
    }

    private inner class WalletPagerAdapter(val pagerState: PagerState) : FragmentStateAdapter(childFragmentManager, lifecycle) {
        override fun getItemCount(): Int = VIEWPAGER_ITEM_COUNT

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                WALLET_FULLSCREEN_ACTIVITY_PASS_POSITION -> {
                    when (pagerState) {
                        PagerState.EXPLANATION -> WalletFullscreenActivityPassExplanationFragment.newInstance(args.id)
                        PagerState.ACTIVITY_PASS -> WalletFullscreenActivityPassFragment.newInstance(args.id)
                    }
                }
                WALLET_FULLSCREEN_BORDER_POSITION -> WalletFullscreenBorderFragment.newInstance(args.id)
                else -> throw UnknownException("No fragment for position $position")
            }
        }
    }

    enum class PagerState {
        EXPLANATION, ACTIVITY_PASS
    }

    companion object {
        private const val VIEWPAGER_ITEM_COUNT = 2

        private const val WALLET_FULLSCREEN_ACTIVITY_PASS_POSITION = 1
        private const val WALLET_FULLSCREEN_BORDER_POSITION = 0
    }
}
