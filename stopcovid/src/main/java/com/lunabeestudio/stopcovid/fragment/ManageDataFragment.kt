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
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.extension.findNavControllerOrNull
import com.lunabeestudio.stopcovid.coreui.extension.getApplicationLanguage
import com.lunabeestudio.stopcovid.coreui.extension.userLanguage
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.lightButtonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.switchItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.areInfoNotificationsEnabled
import com.lunabeestudio.stopcovid.extension.flaggedCountry
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.hideRiskStatus
import com.lunabeestudio.stopcovid.extension.isolationManager
import com.lunabeestudio.stopcovid.extension.listLogFiles
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.extension.secureKeystoreDataSource
import com.lunabeestudio.stopcovid.extension.venuesFeaturedWasActivatedAtLeastOneTime
import com.lunabeestudio.stopcovid.fastitem.dangerButtonItem
import com.lunabeestudio.stopcovid.fastitem.selectionItem
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.model.DeviceSetup
import com.lunabeestudio.stopcovid.viewmodel.ManageDataViewModel
import com.lunabeestudio.stopcovid.viewmodel.ManageDataViewModelFactory
import com.mikepenz.fastadapter.GenericItem

class ManageDataFragment : MainFragment() {

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val isolationManager by lazy {
        requireContext().isolationManager()
    }

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val viewModel: ManageDataViewModel by viewModels {
        ManageDataViewModelFactory(
            requireContext().secureKeystoreDataSource(),
            robertManager,
            isolationManager,
            vaccinationCenterManager,
            venueRepository,
            walletRepository,
            analyticsManager,
            debugManager,
        )
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
        viewModel.eraseLocalSuccess.observe(viewLifecycleOwner) {
            showSnackBar(strings["manageDataController.eraseLocalHistory.success"] ?: "")
        }
        viewModel.eraseRemoteSuccess.observe(viewLifecycleOwner) {
            showSnackBar(strings["manageDataController.eraseRemoteContact.success"] ?: "")
        }
        viewModel.deleteAnalyticsSuccess.observe(viewLifecycleOwner) {
            showSnackBar(strings["manageDataController.analytics.successDialog.message"] ?: "")
        }
        viewModel.quitStopCovidSuccess.observe(viewLifecycleOwner) {
            showSnackBar(strings["manageDataController.quitStopCovid.success"] ?: "")
            sharedPreferences.edit {
                remove(Constants.SharedPrefs.ON_BOARDING_DONE)
            }
            findNavControllerOrNull()?.safeNavigate(ManageDataFragmentDirections.actionGlobalOnBoardingActivity())
            activity?.finishAndRemoveTask()
        }
        viewModel.venuesQrCodeLiveData.observe(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        manageNotificationsItems(items)
        spaceDividerItems(items)
        hideRiskStatusItems(items)
        spaceDividerItems(items)
        userLanguageItems(items)
        spaceDividerItems(items)
        eraseAttestationItems(items)
        spaceDividerItems(items)
        eraseCertificatesItems(items)
        spaceDividerItems(items)
        if (robertManager.configuration.displayIsolation) {
            eraseIsolationItems(items)
            spaceDividerItems(items)
        }
        if (sharedPreferences.venuesFeaturedWasActivatedAtLeastOneTime
            || robertManager.configuration.displayRecordVenues
            || !viewModel.venuesQrCodeLiveData.value.isNullOrEmpty()
        ) {
            eraseVenuesItems(items)
            spaceDividerItems(items)
        }

        val deviceSetup = ProximityManager.getDeviceSetup(requireContext(), robertManager)

        if (deviceSetup != DeviceSetup.NO_BLE) {
            eraseLocalHistoryItems(items)
            spaceDividerItems(items)
            eraseRemoteContactItems(items)
            spaceDividerItems(items)
        }

        if (robertManager.configuration.isAnalyticsOn) {
            optOutAnalyticsItems(items)
            spaceDividerItems(items)
        }

        logsItems(items)
        spaceDividerItems(items)

        if (deviceSetup != DeviceSetup.NO_BLE || robertManager.isRegistered) {
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

    private fun eraseVenuesItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.venuesData.title"]
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.venuesData.subtitle"]
            identifier = items.count().toLong()
        }
        items += lightButtonItem {
            text = strings["manageDataController.venuesData.button"]
            onClickListener = View.OnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["manageDataController.venuesData.confirmationDialog.title"])
                    .setMessage(strings["manageDataController.venuesData.confirmationDialog.message"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        viewModel.eraseVenues(requireContext().applicationContext as RobertApplication)
                    }
                    .show()
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
                        viewModel.eraseAttestations(requireContext())
                    }
                    .show()
            }
            identifier = items.count().toLong()
        }
    }

    private fun eraseCertificatesItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.walletData.title"]
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.walletData.subtitle"]
            identifier = items.count().toLong()
        }
        items += lightButtonItem {
            text = strings["manageDataController.walletData.button"]
            onClickListener = View.OnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["manageDataController.walletData.confirmationDialog.title"])
                    .setMessage(strings["manageDataController.walletData.confirmationDialog.message"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        viewModel.eraseCertificates()
                    }
                    .show()
            }
            identifier = items.count().toLong()
        }
    }

    private fun eraseIsolationItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.isolationData.title"]
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.isolationData.subtitle"]
            identifier = items.count().toLong()
        }
        items += lightButtonItem {
            text = strings["manageDataController.isolationData.button"]
            onClickListener = View.OnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["manageDataController.isolationData.confirmationDialog.title"])
                    .setMessage(strings["manageDataController.isolationData.confirmationDialog.message"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        viewModel.eraseIsolation()
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

    private fun hideRiskStatusItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.hideStatus.title"]
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.hideStatus.subtitle"]
            identifier = items.count().toLong()
        }
        items += switchItem {
            title = strings["manageDataController.hideStatus.button"]
            isChecked = sharedPreferences.hideRiskStatus
            onCheckChange = { isChecked ->
                viewModel.eraseRemoteAlert(requireContext().applicationContext as RobertApplication)
                sharedPreferences.hideRiskStatus = isChecked
            }
            identifier = items.count().toLong()
        }
    }

    private fun userLanguageItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.userLanguage.title"]
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.userLanguage.subtitle"]
            identifier = items.count().toLong()
        }
        UiConstants.SUPPORTED_LOCALES.mapTo(items) { locale ->
            selectionItem {
                caption = locale.flaggedCountry
                showSelection = requireContext().getApplicationLanguage().equals(locale.language, false)
                onClick = {
                    sharedPreferences.userLanguage = locale.language
                }
                identifier = locale.language.hashCode().toLong()
            }
        }
    }

    private fun optOutAnalyticsItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.analytics.title"]
            identifier = items.count().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.analytics.subtitle"]
            identifier = items.count().toLong()
        }
        val isOptIn = analyticsManager.isOptIn()
        items += switchItem {
            title = strings[
                if (isOptIn) {
                    "manageDataController.analytics.switch.on"
                } else {
                    "manageDataController.analytics.switch.off"
                }
            ]
            isChecked = isOptIn
            onCheckChange = { isChecked ->
                analyticsManager.setIsOptIn(isChecked)
                refreshScreen()
            }
            identifier = items.count().toLong()
        }
        items += lightButtonItem {
            text = strings["manageDataController.analytics.button"]
            onClickListener = View.OnClickListener {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["manageDataController.analytics.confirmationDialog.title"])
                    .setMessage(strings["manageDataController.analytics.confirmationDialog.message"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        context?.let {
                            viewModel.requestDeleteAnalytics(requireContext().applicationContext as RobertApplication)
                        }
                    }
                    .show()
            }
            identifier = "manageDataController.analytics.button".hashCode().toLong()
        }
    }

    private fun logsItems(items: MutableList<GenericItem>) {
        items += titleItem {
            text = strings["manageDataController.logFiles.title"]
            identifier = "manageDataController.logFiles.title".hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += captionItem {
            text = strings["manageDataController.logFiles.subtitle"]
            identifier = "manageDataController.logFiles.subtitle".hashCode().toLong()
        }
        if (!debugManager.logsDir.listLogFiles().isNullOrEmpty()) {
            items += captionItem {
                text = stringsFormat("manageDataController.logFiles.logsFilesCount", debugManager.logsDir.listLogFiles()?.count() ?: 0)
                identifier = "manageDataController.logFiles.noLogs".hashCode().toLong()
            }
            items += lightButtonItem {
                text = strings["manageDataController.logFiles.share.button"]
                onClickListener = View.OnClickListener {
                    viewModel.exportLogs(requireContext(), strings)
                }
                identifier = "manageDataController.logs.button".hashCode().toLong()
            }
            items += lightButtonItem {
                text = strings["manageDataController.logFiles.delete.button"]
                onClickListener = View.OnClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(strings["manageDataController.logFiles.delete.confirmationDialog.title"])
                        .setMessage(strings["manageDataController.logFiles.delete.confirmationDialog.message"])
                        .setNegativeButton(strings["common.cancel"], null)
                        .setPositiveButton(strings["common.confirm"]) { _, _ ->
                            context?.let {
                                debugManager.logsDir.deleteRecursively()
                                debugManager.logsDir.mkdir()
                                refreshScreen()
                            }
                        }
                        .show()
                }
                identifier = "manageDataController.logFiles.delete.button".hashCode().toLong()
            }
        } else {
            items += captionItem {
                text = strings["manageDataController.logFiles.noLogs"]
                identifier = "manageDataController.logFiles.noLogs".hashCode().toLong()
            }
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