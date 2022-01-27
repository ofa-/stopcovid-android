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
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.refreshLift
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.navGraphWalletViewModels
import com.lunabeestudio.stopcovid.extension.onFirstFragmentAttached
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.model.UnknownException
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory

class WalletPagerFragment : BaseFragment() {

    private lateinit var viewPager: ViewPager2
    private var tabLayoutMediator: TabLayoutMediator? = null
    private var tabSelectedListener: TabLayout.OnTabSelectedListener? = null

    private val robertManager: RobertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel by navGraphWalletViewModels<WalletContainerFragment> {
        WalletViewModelFactory(
            requireContext().robertManager(),
            injectionContainer.blacklistDCCManager,
            injectionContainer.blacklist2DDOCManager,
            injectionContainer.walletRepository,
            injectionContainer.generateActivityPassUseCase,
            injectionContainer.getSmartWalletCertificateUseCase,
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewPager = ViewPager2(inflater.context)
        viewPager.id = R.id.wallet_pager
        return viewPager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        viewModel.certificatesCount.observe(viewLifecycleOwner) { certificatesCount ->
            if (certificatesCount == 0) {
                (activity as? MainActivity)?.showProgress(false)
                findNavControllerOrNull()?.safeNavigate(WalletPagerFragmentDirections.actionWalletPagerFragmentToWalletInfoFragment())
            } else if (certificatesCount != null) {
                (activity as? MainActivity)?.binding?.tabLayout?.getTabAt(walletCertificateFragmentPosition)?.text =
                    stringsFormat("walletController.mode.myCertificates", certificatesCount)
            }
        }
    }

    override fun refreshScreen() {
        appCompatActivity?.supportActionBar?.title = strings["walletController.title"]
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = true
    }

    private fun setupViewPager() {
        viewPager.adapter = WalletPagerAdapter()
        (activity as? MainActivity)?.binding?.tabLayout?.let { tabLayout ->
            tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
                    walletInfoFragmentPosition -> strings["walletController.mode.info"]
                    walletMultipassFragmentPosition -> strings["multiPass.tab.title"]
                    else -> null
                }
            }.also {
                it.attach()
            }

            tabSelectedListener = object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    view?.postDelayed(
                        {
                            val fragment = getTabFragmentForPosition(tabLayout.selectedTabPosition)
                            (fragment as? PagerTabFragment)?.onTabSelected()

                            val appBarLayout = (activity as? MainActivity)?.binding?.appBarLayout ?: return@postDelayed

                            // Force invalidate cached target view
                            appBarLayout.liftOnScrollTargetViewId = R.id.recycler_view

                            // Refresh current lift state
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

            tabSelectedListener?.let {
                tabLayout.addOnTabSelectedListener(it)
            }
        }

        // Call onTabSelected on first fragment attached to emulate the tab selection
        childFragmentManager.onFirstFragmentAttached { _, fragment ->
            (fragment as? PagerTabFragment)?.onTabSelected()
        }
    }

    private fun getTabFragmentForPosition(selectedTabPosition: Int): Fragment? {
        return when (selectedTabPosition) {
            walletCertificateFragmentPosition ->
                childFragmentManager.fragments.firstOrNull { it is WalletCertificateFragment }
            walletMultipassFragmentPosition ->
                childFragmentManager.fragments.firstOrNull { it is WalletMultipassFragment }
            walletInfoFragmentPosition ->
                childFragmentManager.fragments.firstOrNull { it is WalletInfoFragment }
            else -> throw UnknownException("No fragment for position $selectedTabPosition")
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

    private inner class WalletPagerAdapter : FragmentStateAdapter(childFragmentManager, lifecycle) {
        override fun getItemCount(): Int = viewpagerItemCount

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                walletCertificateFragmentPosition -> WalletCertificateFragment()
                walletMultipassFragmentPosition -> WalletMultipassFragment()
                walletInfoFragmentPosition -> WalletInfoFragment()
                else -> throw UnknownException("No fragment for position $position")
            }
        }
    }

    private val viewpagerItemCount: Int
        get() = if (robertManager.configuration.multipassConfig?.isEnabled == true) {
            3
        } else {
            2
        }

    private val walletCertificateFragmentPosition: Int
        get() = 0

    private val walletMultipassFragmentPosition: Int
        get() = if (robertManager.configuration.multipassConfig?.isEnabled == true) {
            1
        } else {
            -1
        }

    private val walletInfoFragmentPosition: Int
        get() = if (robertManager.configuration.multipassConfig?.isEnabled == true) {
            2
        } else {
            1
        }
}
