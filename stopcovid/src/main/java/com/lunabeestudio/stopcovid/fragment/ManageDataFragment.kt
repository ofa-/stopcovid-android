/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
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
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.lightButtonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.fastitem.dangerButtonItem
import com.lunabeestudio.stopcovid.viewmodel.ManageDataViewModel
import com.lunabeestudio.stopcovid.viewmodel.ManageDataViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class ManageDataFragment : MainFragment() {

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val viewModel: ManageDataViewModel by viewModels { ManageDataViewModelFactory(requireContext().robertManager()) }

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
            }
            WorkManager.getInstance(requireContext().applicationContext).cancelUniqueWork(Constants.WorkerNames.NOTIFICATION)
            try {
                findNavController()
                    .navigate(ManageDataFragmentDirections.actionGlobalOnBoardingActivity())
                activity?.finishAndRemoveTask()
            } catch (e: IllegalArgumentException) {
                // If user leave the screen before logout is done
            }
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

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
        items += dividerItem {}
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
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
                        viewModel.eraseRemoteExposureHistory()
                    }
                    .show()
            }
            identifier = items.count().toLong()
        }
        items += dividerItem {}
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
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
                        viewModel.eraseRemoteAlert()
                    }
                    .show()
            }
            identifier = items.count().toLong()
        }
        items += dividerItem {}
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
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
        items += dividerItem {}

        return items
    }
}