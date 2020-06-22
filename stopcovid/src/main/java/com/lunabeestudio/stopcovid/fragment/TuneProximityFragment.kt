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
import android.os.SystemClock
import android.text.*
import android.text.style.RelativeSizeSpan
import android.util.Base64
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.collections.MutableList
import kotlin.math.min


class TuneProximityFragment : MainFragment(), RobertApplication.Listener {

    private var localProximityItems = mutableListOf<LocalProximity>()
    private var localEbids = mutableListOf<EphemeralBluetoothIdentifier>()

    private fun initLocalProximityItems() {
        val robertManager = (requireContext().robertManager() as RobertManagerImpl)

        localEbids = robertManager
            .getLocalEbids()
            .toMutableList()
        localProximityItems = robertManager
            .getLocalProximityItems(0)
            .toMutableList()
        localProximityItems.sortByDescending { it.collectedTime }

        if (! robertManager.isProximityActive)
            lastNotificationCaption.text = deactivated

        application.registerListener(this)
    }

    override fun getTitleKey(): String = "tuneProximityController.title"

    private val deactivated = "(%s)".format(
        strings["accessibility.hint.proximity.buttonState.deactivated"])

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CoroutineScope(Dispatchers.Default).launch {
            try { initLocalProximityItems() }
            catch (e: IllegalStateException) { return@launch }
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
            synchronized(localProximityItems) {
                nbItemsCaption.text = "ebids: %d  |  pairs: %d  |  pings: %d"
                    .format(
                        localEbids.count(),
                        localProximityItems.groupBy { it.ebidBase64 }.count(),
                        localProximityItems.count()
                    )
                proximityInfoList.text = localProximityItemsToString()
                updateTopBar()
            }

            if (binding?.recyclerView?.isComputingLayout == false)
                binding?.recyclerView?.adapter?.notifyDataSetChanged()
        }
    }

    private fun updateTopBar() {
        val title = "%s  ".format(strings[getTitleKey()])
        val ebid = "(%s)".format(getCurrentEbidBase64())
        (activity as AppCompatActivity).supportActionBar?.title =
            SpannableString(title + ebid).also {
                it.setSpan(RelativeSizeSpan(.6f), title.length, it.length,
                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            }
    }

    private fun getCurrentEbidBase64(): String {
        val currentEbid = (requireContext().robertManager() as RobertManagerImpl).getCurrentEbid()
        return Base64.encodeToString(currentEbid?.ebid, Base64.NO_WRAP) ?: "no ebid"
    }

    private val nbDisplayedItems = 300
    private val dateFormatter = SimpleDateFormat("E d MMM HH:mm:ss")
    private fun localProximityItemsToString(): String {
        return if (showFullList)
            fullList()
        else
            compactList()
    }

    private fun fullList(): String {
        return localProximityItems
            .slice(0 until min(nbDisplayedItems, localProximityItems.size))
            .joinToString("\n") {
                listOf(
                    formatDate(it),
                    it.ebidBase64,
                    it.calibratedRssi
                ).joinToString(", ")
            }
    }

    private fun compactList(): String {
        var prev = LocalProximity("","","",0,0,0,0)
        var count = 0
        return localProximityItems
            .fold(mutableListOf(), {
                acc: MutableList<String>, it: LocalProximity ->
                if (prev.ebidBase64 != it.ebidBase64) {
                    if (count > 1)
                        acc.add(formatSummary(prev, count))
                    acc.add(formatMainLine(it))
                    count = 0
                }
                count += 1
                prev = it
                acc
            })
            .joinToString("\n")
            .plus(if (count > 1)
                "\n" + formatSummary(prev, count) else "")
    }

    private fun formatDate(it: LocalProximity): String {
        return dateFormatter.format(Date(
            it.collectedTime.ntpTimeSToUnixTimeMs()
        ))
    }

    private fun formatMainLine(it: LocalProximity): String {
        return "%s, %s".format(
            formatDate(it),
            it.ebidBase64
        )
    }

    private fun formatSummary(prev: LocalProximity, count: Int): String {
        return "%s, ... (%d)".format(
            formatDate(prev),
            count
        )
    }

    private var showFullList = false
    private fun toggleListDisplay() {
        showFullList = ! showFullList
        refreshItems()
    }

    private var lastClick = 0L
    private fun toggleProximityScanner() {
        if (lastClick + 1000 > SystemClock.elapsedRealtime())
            return
        lastClick = SystemClock.elapsedRealtime()

        val robertManager = requireContext().robertManager()
        if (robertManager.isProximityActive) {
            robertManager.deactivateProximity(requireContext().applicationContext as RobertApplication)
            resetLastNotification(deactivated)
        }
        else CoroutineScope(Dispatchers.Default).launch {
            robertManager.activateProximity(requireContext().applicationContext as RobertApplication, false)
            resetLastNotification()
        }
    }

    private val nbItemsCaption = captionItem {
        text = "..."
        gravity = Gravity.CENTER
    }

    private val lastNotificationCaption = captionItem {
        gravity = Gravity.CENTER
        text = "-"
        onClick = {
            toggleProximityScanner()
        }
    }

    private val proximityInfoList = captionItem {
        selectableText = true
        onClick = {
            toggleListDisplay()
        }
    }

    override fun notify(notification: Any) {
        (notification as com.orange.proximitynotification.ProximityInfo)
        .toLocalProximity()?.let {
            synchronized(lastNotificationCaption) {
                lastNotificationCaption.text = "rssi: " + it.calibratedRssi + "dBm"
            }
            synchronized(localProximityItems) {
                localProximityItems.add(0, it)
            }
            refreshItems()
        }
    }

    private fun resetLastNotification(text: String = "-") {
        synchronized(lastNotificationCaption) {
            lastNotificationCaption.text = text
        }
        refreshItems()
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
