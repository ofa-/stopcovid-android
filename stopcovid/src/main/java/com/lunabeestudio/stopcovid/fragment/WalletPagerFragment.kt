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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.extension.refreshLift
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.model.UnknownException
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory

class WalletPagerFragment : BaseFragment() {

    private lateinit var viewPager: ViewPager2
    private var tabLayoutMediator: TabLayoutMediator? = null
    private var tabSelectedListener: TabLayout.OnTabSelectedListener? = null

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val keystoreDataSource by lazy {
        requireContext().secureKeystoreDataSource()
    }

    private val viewModel: WalletViewModel by viewModels(
        {
            findParentFragmentByType<WalletContainerFragment>() ?: requireParentFragment()
        },
        {
            WalletViewModelFactory(robertManager, keystoreDataSource)
        }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewPager = ViewPager2(inflater.context)
        viewPager.id = R.id.wallet_pager
        return viewPager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = true
        setupViewPager()
        viewModel.certificatesCount.observe(viewLifecycleOwner) { certificateCount ->
            if (certificateCount == 0) {
                findNavControllerOrNull()?.safeNavigate(WalletPagerFragmentDirections.actionWalletPagerFragmentToWalletInfoFragment())
            } else {
                (activity as? MainActivity)?.binding?.tabLayout?.getTabAt(WALLET_CERTIFICATE_FRAGMENT_POSITION)?.text =
                    stringsFormat("walletController.mode.myCertificates", certificateCount)
            }
        }
    }

    override fun refreshScreen() {
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = true
    }

    private fun setupViewPager() {
        viewPager.adapter = WalletPagerAdapter()
        (activity as? MainActivity)?.binding?.tabLayout?.let { tabLayout ->
            tabLayoutMediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                if (position == WALLET_INFO_FRAGMENT_POSITION) {
                    tab.text = strings["walletController.mode.info"]
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
                                WALLET_CERTIFICATE_FRAGMENT_POSITION ->
                                    childFragmentManager.fragments.firstOrNull { it is WalletCertificateFragment }
                                WALLET_INFO_FRAGMENT_POSITION ->
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
        viewPager.adapter = null
        tabLayoutMediator?.detach()
        (activity as? MainActivity)?.binding?.tabLayout?.apply {
            isVisible = false
            tabSelectedListener?.let { removeOnTabSelectedListener(it) }
        }
    }

    private inner class WalletPagerAdapter : FragmentStateAdapter(childFragmentManager, lifecycle) {
        override fun getItemCount(): Int = VIEWPAGER_ITEM_COUNT

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                WALLET_CERTIFICATE_FRAGMENT_POSITION -> WalletCertificateFragment()
                WALLET_INFO_FRAGMENT_POSITION -> WalletInfoFragment()
                else -> throw UnknownException("No fragment for position $position")
            }
        }
    }

    companion object {
        private const val VIEWPAGER_ITEM_COUNT = 2

        private const val WALLET_CERTIFICATE_FRAGMENT_POSITION = 0
        private const val WALLET_INFO_FRAGMENT_POSITION = 1
    }
}
