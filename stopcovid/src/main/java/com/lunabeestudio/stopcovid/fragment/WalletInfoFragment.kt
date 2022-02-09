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
import android.view.View
import androidx.core.view.isVisible
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.extension.setLiftOnScrollTargetView
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.navGraphWalletViewModels
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.defaultPhoneSupportItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.fastitem.walletDoubleDocumentCardItem
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class WalletInfoFragment : MainFragment(), PagerTabFragment {

    private val robertManager by lazy {
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

    override fun getTitleKey(): String = "walletController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.certificatesCount.observe(viewLifecycleOwner) { certificatesCount ->
            if (certificatesCount == 0 || parentFragment is WalletPagerFragment) {
                refreshScreen()

                if (certificatesCount == 0) {
                    onTabSelected() // init bottom button
                }
            } else if (certificatesCount != null) {
                findNavControllerOrNull()?.safeNavigate(WalletInfoFragmentDirections.actionWalletInfoFragmentToWalletPagerFragment())
            }
        }
    }

    override fun refreshScreen() {
        super.refreshScreen()
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = parentFragment is WalletPagerFragment
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.wallet
            identifier = R.drawable.wallet.toLong()
        }

        items += cardWithActionItem {
            mainTitle = strings["walletController.howDoesItWork.title"]
            mainBody = strings["walletController.howDoesItWork.subtitle"]
            identifier = "walletController.howDoesItWork.subtitle".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        items += walletDoubleDocumentCardItem {
            mainTitle = strings["walletController.documents.title"]
            mainBody = strings["walletController.documents.subtitle"]
            identifier = "walletController.documents.subtitle".hashCode().toLong()

            vaccinCertificateCaption = strings["walletController.documents.vaccin"]
            onVaccinCertificateClick = {
                findParentFragmentByType<WalletContainerFragment>()?.findNavControllerOrNull()
                    ?.safeNavigate(
                        WalletContainerFragmentDirections.actionWalletContainerFragmentToCertificateDocumentExplanationFragment(
                            WalletCertificateType.VACCINATION_EUROPE
                        )
                    )
            }

            testCertificateCaption = strings["walletController.documents.test"]
            onTestCertificateClick = {
                findParentFragmentByType<WalletContainerFragment>()?.findNavControllerOrNull()
                    ?.safeNavigate(
                        WalletContainerFragmentDirections.actionWalletContainerFragmentToCertificateDocumentExplanationFragment(
                            WalletCertificateType.SANITARY_EUROPE
                        )
                    )
            }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        items += cardWithActionItem {
            mainTitle = strings["walletController.whenToUse.title"]
            mainBody = strings["walletController.whenToUse.subtitle"]
            actions = listOf(
                Action(label = strings["walletController.whenToUse.button"]) {
                    strings["walletController.whenToUse.url"]?.let { url ->
                        context?.let(url::openInExternalBrowser)
                    }
                }
            )
            identifier = "walletController.whenToUse.subtitle".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        items += cardWithActionItem {
            mainTitle = strings["walletController.info.fraud.title"]
            mainBody = strings["walletController.info.fraud.explanation"]
            actions = listOf(
                Action(label = strings["walletController.info.fraud.button"]) {
                    strings["walletController.info.fraud.url"]?.let { fraudUrl ->
                        context?.let(fraudUrl::openInExternalBrowser)
                    }
                }
            )
            identifier = "walletController.info.fraud.explanation".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }
        context?.let {
            items += defaultPhoneSupportItem(strings, it)
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        return items
    }

    override fun onTabSelected() {
        binding?.recyclerView?.let { (activity as? MainActivity)?.binding?.appBarLayout?.setLiftOnScrollTargetView(it) }

        findParentFragmentByType<WalletContainerFragment>()?.let { walletContainerFragment ->
            if (robertManager.configuration.multipassConfig?.isEnabled == true && (viewModel.certificatesCount.value ?: 0) > 0) {
                walletContainerFragment.setupBottomAction(null, null)
            } else {
                walletContainerFragment.setupBottomAction(strings["walletController.addCertificate"]) {
                    walletContainerFragment.findNavControllerOrNull()
                        ?.safeNavigate(WalletContainerFragmentDirections.actionWalletContainerFragmentToWalletQRCodeFragment())
                }
            }
        }
    }
}
