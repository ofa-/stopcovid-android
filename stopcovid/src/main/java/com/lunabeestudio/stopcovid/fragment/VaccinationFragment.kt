/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/01/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.extension.observeEventAndConsume
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.viewLifecycleOwnerOrNull
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.coreui.model.CardTheme
import com.lunabeestudio.stopcovid.extension.chosenPostalCode
import com.lunabeestudio.stopcovid.extension.hasChosenPostalCode
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.showPostalCodeDialog
import com.lunabeestudio.stopcovid.extension.toItem
import com.lunabeestudio.stopcovid.fastitem.bigTitleItem
import com.lunabeestudio.stopcovid.fastitem.changePostalCodeItem
import com.lunabeestudio.stopcovid.fastitem.linkCardItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

class VaccinationFragment : MainFragment() {

    private val sharedPrefs: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    override fun getTitleKey(): String = "vaccinationController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vaccinationCenterManager.vaccinationCenters.observeEventAndConsume(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        items += cardWithActionItem(CardTheme.Default) {
            mainTitle = strings["vaccinationController.eligibility.title"]
            mainBody = strings["vaccinationController.eligibility.subtitle"]
            actions = listOf(
                Action(label = strings["vaccinationController.eligibility.buttonTitle"]) {
                    strings["vaccinationController.eligibility.url"]?.openInExternalBrowser(it.context)
                }
            )
            identifier = "vaccinationController.eligibility.title".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        items += bigTitleItem {
            text = strings["vaccinationController.vaccinationLocation.section.title"]
            identifier = "vaccinationController.vaccinationLocation.section.title".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_small
            identifier = items.count().toLong()
        }

        items += if (sharedPrefs.hasChosenPostalCode) {
            getPostalCodeItems()
        } else {
            getNoPostalCodeItems()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        return items
    }

    private fun getNoPostalCodeItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += captionItem {
            text = stringsFormat(
                "vaccinationController.vaccinationLocation.explanation",
                robertManager.configuration.vaccinationCentersCount
            )
            identifier = "vaccinationController.vaccinationLocation.explanation".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_small
            identifier = items.count().toLong()
        }

        items += linkCardItem {
            label = strings["vaccinationController.vaccinationLocation.newPostalCode"]
            iconRes = R.drawable.ic_map
            onClickListener = View.OnClickListener {
                showPostalCodeDialog()
            }
            identifier = "vaccinationController.vaccinationLocation.newPostalCode".hashCode().toLong()
        }

        return items
    }

    private fun getPostalCodeItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += changePostalCodeItem {
            label = stringsFormat("common.updatePostalCode", sharedPrefs.chosenPostalCode)
            endLabel = strings["common.updatePostalCode.end"]
            iconRes = R.drawable.ic_map
            onClickListener = View.OnClickListener {
                findNavControllerOrNull()
                    ?.safeNavigate(VaccinationFragmentDirections.actionVaccinationFragmentToPostalCodeBottomSheetFragment())
            }
            identifier = "common.updatePostalCode".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        val vaccinationCenters = vaccinationCenterManager.vaccinationCentersToDisplay(robertManager, sharedPrefs)
        if (vaccinationCenters.isNullOrEmpty()) {
            items += titleItem {
                text = strings["vaccinationController.vaccinationLocation.vaccinationCenterNotFound"]
                gravity = Gravity.CENTER
                identifier = "vaccinationController.vaccinationLocation.vaccinationCenterNotFound".hashCode().toLong()
            }
            items += linkCardItem {
                label = strings["common.retry"]
                iconRes = R.drawable.ic_refresh
                onClickListener = View.OnClickListener {
                    viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
                        (activity as? MainActivity)?.showProgress(true)
                        val refreshStart = System.currentTimeMillis()
                        vaccinationCenterManager.onAppForeground(requireContext(), sharedPrefs)
                        if (vaccinationCenterManager.vaccinationCentersToDisplay(robertManager, sharedPrefs).isNullOrEmpty()) {
                            // Force display of a loading
                            delay(max(0, Constants.Android.FORCE_LOADING_DELAY - (System.currentTimeMillis() - refreshStart)))
                        }
                        (activity as? MainActivity)?.showProgress(false)
                    }
                }
                identifier = "common.retry".hashCode().toLong()
            }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
                identifier = items.count().toLong()
            }
        } else {
            vaccinationCenters.forEach { vaccinationCenter ->
                items += vaccinationCenter.toItem(strings) {
                    findNavControllerOrNull()
                        ?.safeNavigate(
                            VaccinationFragmentDirections.actionVaccinationFragmentToVaccinationActionsBottomSheetFragment(
                                vaccinationCenter
                            )
                        )
                }

                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }
            }
        }

        items += captionItem {
            text = strings["vaccinationController.vaccinationLocation.footer"]
            identifier = "vaccinationController.vaccinationLocation.footer".hashCode().toLong()
        }

        items += linkItem {
            text = strings["vaccinationController.vaccinationLocation.buttonTitle"]
            url = strings["vaccinationController.vaccinationLocation.url"]
            identifier = "vaccinationController.vaccinationLocation.buttonTitle".hashCode().toLong()
        }

        return items
    }

    private fun showPostalCodeDialog() {
        MaterialAlertDialogBuilder(requireContext()).showPostalCodeDialog(
            layoutInflater,
            strings,
            keyFiguresManager,
        ) { postalCode ->
            sharedPrefs.chosenPostalCode = postalCode
            viewLifecycleOwnerOrNull()?.lifecycleScope?.launch {
                (activity as? MainActivity)?.showProgress(true)
                vaccinationCenterManager.postalCodeDidUpdate(requireContext(), sharedPrefs, postalCode)
                (activity as? MainActivity)?.showProgress(false)
                refreshScreen()
            }
        }
    }
}