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
import androidx.fragment.app.viewModels
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.fastitem.phoneSupportItem
import com.lunabeestudio.stopcovid.fastitem.walletDoubleDocumentCardItem
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import kotlin.time.ExperimentalTime

class WalletInfoFragment : MainFragment() {

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

    override fun getTitleKey(): String = "walletController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.certificates.observe(viewLifecycleOwner) {
            if (it.isNullOrEmpty() || parentFragment is WalletPagerFragment) {
                refreshScreen()
            } else {
                findNavControllerOrNull()?.safeNavigate(WalletInfoFragmentDirections.actionWalletInfoFragmentToWalletPagerFragment())
            }
        }
    }

    override fun refreshScreen() {
        super.refreshScreen()
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = parentFragment is WalletPagerFragment
    }

    @OptIn(ExperimentalTime::class)
    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.wallet
            identifier = R.drawable.wallet.toLong()
        }

        items += cardWithActionItem {
            mainTitle = strings["walletController.howDoesItWork.title"]
            mainBody = strings["walletController.howDoesItWork.subtitle"]
            identifier = mainBody.hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        items += walletDoubleDocumentCardItem {
            mainTitle = strings["walletController.documents.title"]
            mainBody = strings["walletController.documents.subtitle"]
            identifier = mainBody.hashCode().toLong()

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
                    strings["walletController.whenToUse.url"]?.openInExternalBrowser(requireContext())
                }
            )
            identifier = mainBody.hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        items += phoneSupportItem {
            title = strings["walletController.phone.title"]
            subtitle = strings["walletController.phone.subtitle"]
            onClick = {
                strings["walletController.phone.number"]?.callPhone(requireContext())
            }
            identifier = title.hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        return items
    }
}
