/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.content.edit
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.lightButtonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.switchItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.areInfoNotificationsEnabled
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.fastitem.dangerButtonItem
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.model.DeviceSetup
import com.lunabeestudio.stopcovid.viewmodel.ManageDataViewModel
import com.lunabeestudio.stopcovid.viewmodel.ManageDataViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class ManageDataFragment : MainFragment() {

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val deviceSetup by lazy {
        ProximityManager.getDeviceSetup(requireContext())
    }

    private val viewModel: ManageDataViewModel by viewModels {
        ManageDataViewModelFactory(requireContext().secureKeystoreDataSource(),
            requireContext().robertManager())
    }

    override fun getTitleKey(): String = "manageDataController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModelObserver()
    }

    private fun initViewModelObserver() {
        viewModel.loadingInProgress.observe(viewLifecycleOwner) { loadingInProgress ->
            (activity as? MainActivity)?.showProgress(loadingInProgress)
        }
        viewModel.covidException.observe(viewLifecycleOwner) { covidException ->
            showErrorSnackBar(covidException.getString(strings))
        }
        viewModel.eraseAttestationsSuccess.observe(viewLifecycleOwner) {
            showSnackBar(strings["manageDataController.eraseLocalHistory.success"] ?: "")
        }
        viewModel.eraseLocalSuccess.observe(viewLifecycleOwner) {
            showSnackBar(strings["manageDataController.eraseLocalHistory.success"] ?: "")
        }
        viewModel.eraseRemoteSuccess.observe(viewLifecycleOwner) {
            showSnackBar(strings["manageDataController.eraseRemoteContact.success"] ?: "")
        }
        viewModel.eraseAlertSuccess.observe(viewLifecycleOwner) {
            showSnackBar(strings["manageDataController.eraseRemoteAlert.success"] ?: "")
        }
        viewModel.quitStopCovidSuccess.observe(viewLifecycleOwner) {
            showSnackBar(strings["manageDataController.quitStopCovid.success"] ?: "")
            sharedPreferences.edit {
                remove(Constants.SharedPrefs.ON_BOARDING_DONE)
                remove(Constants.SharedPrefs.IS_ADVERTISEMENT_AVAILABLE)
            }
            findNavController().safeNavigate(ManageDataFragmentDirections.actionGlobalOnBoardingActivity())
            activity?.finishAndRemoveTask()
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        manageNotificationsItems(items)
        spaceDividerItems(items)
        eraseAttestationItems(items)
        spaceDividerItems(items)

        if (deviceSetup != DeviceSetup.NO_BLE) {
            eraseLocalHistoryItems(items)
            spaceDividerItems(items)
            eraseRemoteContactItems(items)
            spaceDividerItems(items)
            eraseRemoteAlertItems(items)
            spaceDividerItems(items)
            quitStopCovidItems(items)
        }

        return items
    }

    private fun spaceDividerItems(items: MutableList<GenericItem>) {
        items += dividerItem {}
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
    }

    private fun manageNotificationsItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.showInfoNotifications.title"]
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.showInfoNotifications.subtitle"]
            identifier = items.count().toLong()
        }
        items += switchItem {
            title = strings["manageDataController.showInfoNotifications.button"]
            isChecked = sharedPreferences.areInfoNotificationsEnabled
            onCheckChange = { isChecked ->
                sharedPreferences.areInfoNotificationsEnabled = isChecked
            }
            identifier = items.count().toLong()
        }
    }

    private fun eraseAttestationItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.attestationsData.title"]
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.attestationsData.subtitle"]
            identifier = items.count().toLong()
        }
        items += lightButtonItem {
            text = strings["manageDataController.attestationsData.button"]
            onClickListener = View.OnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["manageDataController.attestationsData.confirmationDialog.title"])
                    .setMessage(strings["manageDataController.attestationsData.confirmationDialog.message"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        viewModel.eraseAttestations()
                    }
                    .show()
            }
            identifier = items.count().toLong()
        }
    }

    private fun eraseLocalHistoryItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.eraseLocalHistory.title"]
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.eraseLocalHistory.subtitle"]
            identifier = items.count().toLong()
        }
        items += lightButtonItem {
            text = strings["manageDataController.eraseLocalHistory.button"]
            onClickListener = View.OnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["manageDataController.eraseLocalHistory.confirmationDialog.title"])
                    .setMessage(strings["manageDataController.eraseLocalHistory.confirmationDialog.message"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        viewModel.eraseLocalHistory()
                    }
                    .show()
            }
            identifier = items.count().toLong()
        }
    }

    private fun eraseRemoteContactItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.eraseRemoteContact.title"]
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.eraseRemoteContact.subtitle"]
            identifier = items.count().toLong()
        }
        items += lightButtonItem {
            text = strings["manageDataController.eraseRemoteContact.button"]
            onClickListener = View.OnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["manageDataController.eraseRemoteContact.confirmationDialog.title"])
                    .setMessage(strings["manageDataController.eraseRemoteContact.confirmationDialog.message"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        viewModel.eraseRemoteExposureHistory(requireContext().applicationContext as RobertApplication)
                    }
                    .show()
            }
            identifier = items.count().toLong()
        }
    }

    private fun eraseRemoteAlertItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.eraseRemoteAlert.title"]
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.eraseRemoteAlert.subtitle"]
            identifier = items.count().toLong()
        }
        items += lightButtonItem {
            text = strings["manageDataController.eraseRemoteAlert.button"]
            onClickListener = View.OnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["manageDataController.eraseRemoteAlert.confirmationDialog.title"])
                    .setMessage(strings["manageDataController.eraseRemoteAlert.confirmationDialog.message"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        viewModel.eraseRemoteAlert(requireContext().applicationContext as RobertApplication)
                    }
                    .show()
            }
            identifier = items.count().toLong()
        }
    }

    private fun quitStopCovidItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.quitStopCovid.title"]
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.quitStopCovid.subtitle"]
            identifier = items.count().toLong()
        }
        items += dangerButtonItem {
            text = strings["manageDataController.quitStopCovid.button"]
            onClickListener = View.OnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["manageDataController.quitStopCovid.confirmationDialog.title"])
                    .setMessage(strings["manageDataController.quitStopCovid.confirmationDialog.message"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        viewModel.quitStopCovid(requireContext().applicationContext as RobertApplication)
                    }
                    .show()
            }
            identifier = items.count().toLong()
        }
    }
}