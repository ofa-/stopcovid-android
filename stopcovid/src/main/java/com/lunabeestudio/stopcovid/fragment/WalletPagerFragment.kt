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
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.appCompatActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.fragment.BaseFragment
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.navGraphWalletViewModels
import com.lunabeestudio.stopcovid.extension.doOnFragmentAttached
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
            injectionContainer.getSmartWalletMapUseCase,
            injectionContainer.getSmartWalletStateUseCase,
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
        viewPager.setUserInputEnabled(false)
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
        viewPager.setCurrentItem(walletCertificateFragmentPosition, false)
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

                    viewPager.setUserInputEnabled(
                        when (tabLayout.selectedTabPosition) {
                            walletCertificateFragmentPosition -> false
                            else -> true
                        })

                    val block: (FragmentManager, Fragment) -> Unit = { _, fragment ->
                        (fragment as? PagerTabFragment)?.onTabSelected()
                    }

                    when (tab?.position) {
                        walletCertificateFragmentPosition -> childFragmentManager.doOnFragmentAttached<WalletCertificateFragment>(block)
                        walletMultipassFragmentPosition -> {
                            injectionContainer.analyticsManager.reportAppEvent(AppEventName.e23, null)
                            childFragmentManager.doOnFragmentAttached<WalletMultipassFragment>(block)
                        }
                        walletInfoFragmentPosition -> childFragmentManager.doOnFragmentAttached<WalletInfoFragment>(block)
                        else -> throw UnknownException("No fragment for position ${tab?.position}")
                    }
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
        childFragmentManager.doOnFragmentAttached<Fragment> { _, fragment ->
            (fragment as? PagerTabFragment)?.onTabSelected()
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

    private val multipassEnabled: Boolean
        get() = true || (robertManager.configuration.multipassConfig?.isEnabled == true)

    private val viewpagerItemCount: Int
        get() = if (multipassEnabled) {
            3
        } else {
            2
        }

    private val walletMultipassFragmentPosition: Int
        get() = 0

    private val walletCertificateFragmentPosition: Int
        get() = if (multipassEnabled) {
            1
        } else {
            -1
        }

    private val walletInfoFragmentPosition: Int
        get() = if (multipassEnabled) {
            2
        } else {
            1
        }
}
