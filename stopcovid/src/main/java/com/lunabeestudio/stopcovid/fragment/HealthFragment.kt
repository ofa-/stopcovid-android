/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lunabeestudio.robert.utils.EventObserver
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.extension.getString
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.stopcovid.extension.safeNavigate
import com.lunabeestudio.stopcovid.fastitem.contactItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.model.DeviceSetup
import com.lunabeestudio.stopcovid.viewmodel.HealthViewModel
import com.lunabeestudio.stopcovid.viewmodel.HealthViewModelFactory
import com.mikepenz.fastadapter.GenericItem
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

class HealthFragment : TimeMainFragment() {

    private val robertManager by lazy {
        requireContext().robertManager()
    }

    private val deviceSetup by lazy {
        ProximityManager.getDeviceSetup(requireContext())
    }

    private val viewModel: HealthViewModel by viewModels { HealthViewModelFactory(robertManager) }

    private val dateFormat: DateFormat = SimpleDateFormat("dd LLLL", Locale.getDefault())

    override fun getTitleKey(): String = "myHealthController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModelObserver()
    }

    private fun initViewModelObserver() {
        viewModel.loadingInProgress.observe(viewLifecycleOwner) { loadingInProgress ->
            (activity as? MainActivity)?.showProgress(loadingInProgress)
        }
        viewModel.eraseNotificationSuccess.observe(viewLifecycleOwner) {
            refreshScreen()
        }
        viewModel.covidException.observe(viewLifecycleOwner) { covidException ->
            showErrorSnackBar(covidException.getString(strings))
        }
        robertManager.atRiskStatus.observe(viewLifecycleOwner, EventObserver(this.javaClass.name.hashCode()) {
            refreshScreen()
        })
    }

    override fun getItems(): List<GenericItem> {
        return when {
            deviceSetup == DeviceSetup.NO_BLE -> noBleItems()
            !robertManager.isRegistered -> notRegisteredItems()
            else -> registeredItems()
        }
    }

    private fun showMenu(v: View) {
        PopupMenu(requireContext(), v).apply {
            setOnMenuItemClickListener(::onMenuItemClick)

            inflate(R.menu.notification_menu)

            menu.findItem(R.id.notification_menu_delete).title = strings["sickController.state.deleteNotification"]
            menu.findItem(R.id.notification_menu_learnmore).title = strings["myHealthController.alert.atitudeToAdopt"]

            show()
        }
    }

    private fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.notification_menu_delete -> {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(strings["sickController.state.deleteNotification"])
                    .setMessage(strings["sickController.state.deleteNotification.alert.title"])
                    .setNegativeButton(strings["common.cancel"], null)
                    .setPositiveButton(strings["common.confirm"]) { _, _ ->
                        viewModel.clearNotification()
                    }
                    .show()
                true
            }
            R.id.notification_menu_learnmore -> {
                findNavController().safeNavigate(HealthFragmentDirections.actionHealthFragmentToInformationFragment())
                true
            }
            else -> false
        }
    }

    private fun noBleItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.diagnosis
            identifier = items.count().toLong()
        }

        // Covid Advices
        items += titleItem {
            text = strings["myHealthController.covidAdvices.title"]
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["myHealthController.covidAdvices.subtitle"]
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += linkItem {
            text = strings["myHealthController.alert.atitudeToAdopt"]
            onClickListener = View.OnClickListener {
                findNavController().safeNavigate(HealthFragmentDirections.actionHealthFragmentToInformationFragment())
            }
            identifier = items.size.toLong()
        }
        items += linkItem {
            text = strings["myHealthController.covidAdvices.buttonTitle"]
            url = strings["myHealthController.covidAdvices.url"]
            identifier = items.size.toLong()
        }
        items += dividerItem {}
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        // Testing sites
        items += titleItem {
            text = strings["myHealthController.testingSites.title"]
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["myHealthController.testingSites.subtitle"]
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += linkItem {
            text = strings["myHealthController.testingSites.buttonTitle"]
            url = strings["myHealthController.testingSites.url"]
            identifier = items.size.toLong()
        }
        items += dividerItem {}
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        // Your department
        items += titleItem {
            text = strings["myHealthController.yourDepartment.title"]
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["myHealthController.yourDepartment.subtitle"]
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += linkItem {
            text = strings["myHealthController.yourDepartment.buttonTitle"]
            url = strings["myHealthController.yourDepartment.url"]
            identifier = items.size.toLong()
        }

        return items
    }

    private fun notRegisteredItems(): ArrayList<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.diagnosis
            identifier = items.count().toLong()
        }

        items += titleItem {
            text = strings["myHealthController.notRegistered.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["myHealthController.notRegistered.mainMessage.subtitle"]
            gravity = Gravity.CENTER
            identifier = items.count().toLong()
        }

        return items
    }

    @OptIn(ExperimentalTime::class)
    private fun registeredItems(): ArrayList<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.diagnosis
            identifier = items.count().toLong()
        }

        when (robertManager.isAtRisk) {
            true -> {
                val endExposureCalendar = Calendar.getInstance()
                endExposureCalendar.add(Calendar.DAY_OF_YEAR, robertManager.quarantinePeriod - robertManager.lastExposureTimeframe)
                val endExposureDate = endExposureCalendar.time

                items += contactItem(R.layout.item_contact) {
                    header = stringsFormat(
                        "myHealthController.notification.update",
                        robertManager.atRiskLastRefresh?.milliseconds?.getRelativeDateTimeString(requireContext())
                    )
                    title = strings["sickController.state.contact.title"]
                    caption = stringsFormat("sickController.state.contact.subtitle", dateFormat.format(endExposureDate))
                    more = strings["myHealthController.alert.atitudeToAdopt"]
                    moreClickListener = View.OnClickListener {
                        findNavController().safeNavigate(HealthFragmentDirections.actionHealthFragmentToInformationFragment())
                    }
                    actionClickListener = View.OnClickListener {
                        showMenu(it)
                    }
                    actionContentDescription = strings["accessibility.hint.otherActions"]
                    identifier = items.count().toLong()
                }
            }
            false -> {
                items += contactItem(R.layout.item_no_contact) {
                    header = stringsFormat(
                        "myHealthController.notification.update",
                        robertManager.atRiskLastRefresh?.milliseconds?.getRelativeDateTimeString(requireContext())
                    )
                    title = strings["sickController.state.nothing.title"]
                    caption = strings["sickController.state.nothing.subtitle"]
                    more = strings["myHealthController.alert.atitudeToAdopt"]
                    moreClickListener = View.OnClickListener {
                        findNavController().safeNavigate(HealthFragmentDirections.actionHealthFragmentToInformationFragment())
                    }
                    actionClickListener = View.OnClickListener {
                        showMenu(it)
                    }
                    actionContentDescription = strings["accessibility.hint.otherActions"]
                    identifier = items.count().toLong()
                }

                items += spaceItem {
                    spaceRes = R.dimen.spacing_xlarge
                    identifier = items.count().toLong()
                }
                items += titleItem {
                    text = strings["myHealthController.notification.title"]
                    gravity = Gravity.START
                    identifier = items.count().toLong()
                }
                items += captionItem {
                    text = strings["myHealthController.notification.subtitle"]
                    gravity = Gravity.START
                    identifier = items.count().toLong()
                }
                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }
                items += dividerItem {}
            }
            else -> {
                items += titleItem {
                    text = strings["myHealthController.notification.title"]
                    gravity = Gravity.START
                    identifier = items.count().toLong()
                }
                items += captionItem {
                    text = strings["myHealthController.notification.subtitle"]
                    gravity = Gravity.START
                    identifier = items.count().toLong()
                }
                items += spaceItem {
                    spaceRes = R.dimen.spacing_large
                    identifier = items.count().toLong()
                }
                items += dividerItem {}
            }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        // Covid Advices
        items += titleItem {
            text = strings["myHealthController.covidAdvices.title"]
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["myHealthController.covidAdvices.subtitle"]
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += linkItem {
            text = strings["myHealthController.covidAdvices.buttonTitle"]
            url = strings["myHealthController.covidAdvices.url"]
            identifier = items.size.toLong()
        }
        items += dividerItem {}
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        // Testing sites
        items += titleItem {
            text = strings["myHealthController.testingSites.title"]
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["myHealthController.testingSites.subtitle"]
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += linkItem {
            text = strings["myHealthController.testingSites.buttonTitle"]
            url = strings["myHealthController.testingSites.url"]
            identifier = items.size.toLong()
        }
        items += dividerItem {}
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        // Your department
        items += titleItem {
            text = strings["myHealthController.yourDepartment.title"]
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += captionItem {
            text = strings["myHealthController.yourDepartment.subtitle"]
            gravity = Gravity.START
            identifier = items.count().toLong()
        }
        items += linkItem {
            text = strings["myHealthController.yourDepartment.buttonTitle"]
            url = strings["myHealthController.yourDepartment.url"]
            identifier = items.size.toLong()
        }

        return items
    }

    override fun timeRefresh() {
        refreshScreen()
    }
}
