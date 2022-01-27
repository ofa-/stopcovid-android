/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/18 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.findParentFragmentByType
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.extension.injectionContainer
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.model.MultipassProfile
import com.lunabeestudio.stopcovid.model.MultipassProfileSelectionItemData
import com.lunabeestudio.stopcovid.viewmodel.WalletMultipassViewModel
import com.lunabeestudio.stopcovid.viewmodel.WalletMultipassViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class WalletMultipassFragment : MainFragment(), PagerTabFragment {

    private val viewModel by viewModels<WalletMultipassViewModel> {
        WalletMultipassViewModelFactory(
            injectionContainer.getMultipassProfilesUseCase,
            injectionContainer.getCloseMultipassProfilesUseCase,
        )
    }

    private var closeProfiles: List<MultipassProfile> = emptyList()
        set(value) {
            field = value
            refreshScreen()
        }

    override fun getTitleKey(): String = "walletController.title"

    override fun refreshScreen() {
        super.refreshScreen()
        (activity as? MainActivity)?.binding?.tabLayout?.isVisible = true
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        items += cardWithActionItem {
            mainTitle = strings["multiPass.tab.explanation.title"]
            mainBody = strings["multiPass.tab.explanation.subtitle"]
            actions = listOfNotNull(
                strings["multiPass.tab.explanation.url"]?.takeIf { it.isNotEmpty() }?.let { url ->
                    Action(label = strings["common.readMore"]) {
                        context?.let { ctx -> url.openInExternalBrowser(ctx) }
                    }
                }
            )
            identifier = "multiPass.tab.explanation.subtitle".hashCode().toLong()
        }

        if (closeProfiles.isNotEmpty()) {
            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.size.toLong()
            }

            items += cardWithActionItem {
                mainTitle = strings["multiPass.tab.similarProfile.title"]
                mainBody = stringsFormat("multiPass.tab.similarProfile.subtitle", closeProfiles.joinToString { it.displayName })
                actions = listOfNotNull(
                    strings["multiPass.tab.similarProfile.url"]?.takeIf { it.isNotEmpty() }?.let { url ->
                        Action(label = strings["multiPass.tab.similarProfile.linkButton.title"]) {
                            context?.let { ctx -> url.openInExternalBrowser(ctx) }
                        }
                    }
                )
                identifier = "multiPass.tab.similarProfile.title".hashCode().toLong()
            }
        }

        return items
    }

    override fun onTabSelected() {
        findParentFragmentByType<WalletContainerFragment>()?.let { walletContainerFragment ->
            val multipassProfiles = viewModel.getMultipassProfiles()
            closeProfiles = viewModel.getCloseMultipassProfiles(multipassProfiles)

            walletContainerFragment.setupBottomAction(strings["multiPass.tab.generation.button.title"]) {
                when (multipassProfiles.size) {
                    0 -> context?.let { ctx ->
                        MaterialAlertDialogBuilder(ctx)
                            .setTitle(strings["multiPass.noProfile.alert.title"])
                            .setMessage(strings["multiPass.noProfile.alert.subtitle"])
                            .setPositiveButton(strings["common.ok"], null)
                            .show()
                    }
                    1 -> walletContainerFragment.findNavControllerOrNull()?.safeNavigate(
                        WalletContainerFragmentDirections.actionWalletContainerFragmentToMultipassCertificatesPickerFragment(
                            multipassProfiles.first().id
                        )
                    )
                    else -> {
                        walletContainerFragment.setFragmentResultListener(
                            ChooseMultipassProfileBottomSheetDialogFragment.CHOOSE_MULTIPASS_PROFILE_RESULT_KEY
                        ) { _, data ->
                            data.getString(
                                ChooseMultipassProfileBottomSheetDialogFragment.CHOOSE_MULTIPASS_PROFILE_BUNDLE_KEY_ID_SELECTED
                            )?.let { profileId ->
                                walletContainerFragment.findNavControllerOrNull()?.addOnDestinationChangedListener(
                                    object : NavController.OnDestinationChangedListener {
                                        override fun onDestinationChanged(
                                            controller: NavController,
                                            destination: NavDestination,
                                            arguments: Bundle?
                                        ) {
                                            if (controller.currentDestination?.id == R.id.walletContainerFragment) {
                                                walletContainerFragment.findNavControllerOrNull()?.safeNavigate(
                                                    WalletContainerFragmentDirections
                                                        .actionWalletContainerFragmentToMultipassCertificatesPickerFragment(profileId)
                                                )
                                                controller.removeOnDestinationChangedListener(this)
                                            }
                                        }
                                    })
                            }
                        }

                        walletContainerFragment.findNavControllerOrNull()?.safeNavigate(
                            WalletContainerFragmentDirections
                                .actionWalletContainerFragmentToChooseMultipassProfileBottomSheetDialogFragment(
                                    multipassProfiles.map {
                                        MultipassProfileSelectionItemData(
                                            id = it.id,
                                            displayText = it.displayName,
                                        )
                                    }.toTypedArray()
                                )
                        )
                    }
                }
            }
        }
    }
}
