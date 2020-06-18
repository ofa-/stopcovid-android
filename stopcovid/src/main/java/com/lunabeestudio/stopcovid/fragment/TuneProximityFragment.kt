/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Copied by Ofa from Lunabee Studio / Date - 2020/06/11
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.Gravity
import android.view.View
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeMs
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.framework.ble.extension.toLocalProximity
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManagerImpl
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.extension.robertManager
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.collections.MutableList
import kotlin.math.min


class TuneProximityFragment : MainFragment(), RobertApplication.Listener {

    private lateinit var localProximityItems: MutableList<LocalProximity>
    private lateinit var localEbids: MutableList<EphemeralBluetoothIdentifier>

    private fun initLocalProximityItems() {
        val robertManager = (requireContext().robertManager() as RobertManagerImpl)

        localEbids = robertManager
            .getLocalEbids()
            .toMutableList()
        localProximityItems = robertManager
            .getLocalProximityItems(0)
            .toMutableList()
        localProximityItems.sortByDescending({ it.collectedTime })
        application.registerListener(this)
    }

    override fun getTitleKey(): String = "tuneProximityController.title"


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CoroutineScope(Dispatchers.Default).launch {
            initLocalProximityItems()
            refreshItems()
        }
    }

    private val application: RobertApplication
        get() = (requireContext().applicationContext as RobertApplication)

    override fun onDestroyView() {
        application.registerListener(null)
        super.onDestroyView()
    }

    private fun refreshItems() {
    CoroutineScope(Dispatchers.Main).launch {
            nbItemsCaption.text = "ebids: %d  |  pairs: %d  |  pings: %d"
                .format(
                    localEbids.count(),
                    localProximityItems.groupBy { it.ebidBase64 }.count(),
                    localProximityItems.count())
            proximityInfoList.text = localProximityItemsToString()

            if (binding?.recyclerView?.isComputingLayout == false)
                binding?.recyclerView?.adapter?.notifyDataSetChanged()
        }
    }

    private val nbDisplayedItems = 100
    private val dateFormatter = SimpleDateFormat("E d MMM HH:mm:ss")
    private fun localProximityItemsToString(): String {
        return localProximityItems
            .slice(0 until min(nbDisplayedItems, localProximityItems.size))
            .joinToString("\n") {
                listOf(
                    dateFormatter.format(Date(
                        it.collectedTime.ntpTimeSToUnixTimeMs() )),
                    it.ebidBase64,
                    it.calibratedRssi
                ).joinToString(", ")
            }
    }

    private val nbItemsCaption = captionItem {
            text = "..."
            gravity = Gravity.CENTER
    }

    private val lastNotificationCaption = captionItem {
        gravity = Gravity.CENTER
        text = "-"
    }

    private val proximityInfoList = captionItem {
    }

    override fun notify(notification: Any) {
        (notification as com.orange.proximitynotification.ProximityInfo)
        .toLocalProximity()?.let {
            lastNotificationCaption.text = "rssi: " + it.calibratedRssi + "dBm"
            localProximityItems.add(0, it)
            refreshItems()
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += dividerItem {}
        items += nbItemsCaption
        items += dividerItem {}
        items += lastNotificationCaption
        items += dividerItem {}
        items += proximityInfoList
        items += dividerItem {}

        return items
    }
}
