/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Ofa (mimicking Lunabee Studio) / 2020-06-11
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.text.*
import android.text.style.RelativeSizeSpan
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeMs
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.framework.ble.extension.toLocalProximity
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManagerImpl
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.min


class TuneProximityFragment : MainFragment(), RobertApplication.Listener {

    private var localProximityItems = mutableListOf<LocalProximity>()
    private var localEbids = mutableListOf<EphemeralBluetoothIdentifier>()

    private fun initLocalProximityItems() {
        localEbids = robertManager
            .getLocalEbids()
            .toMutableList()
        localProximityItems = robertManager
            .getLocalProximityItems(0)
            .toMutableList()
        localProximityItems.sortByDescending { it.collectedTime }

        application.registerListener(this)
    }

    override fun getTitleKey(): String = "tuneProximityController.title"

    private val deactivated = "(%s)".format(
        strings["accessibility.hint.proximity.buttonState.deactivated"])

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        application = context?.applicationContext as RobertApplication
        robertManager = application.robertManager as RobertManagerImpl
        setTopBarOnclick()
        updateLastNotification()
        refreshItems()

        CoroutineScope(Dispatchers.Default).launch {
            try { initLocalProximityItems() }
            catch (e: IllegalStateException) { return@launch }
            refreshItems()
        }
    }

    private lateinit var robertManager: RobertManagerImpl
    private lateinit var application: RobertApplication

    private fun setTopBarOnclick() {
        getActivityBinding()?.toolbar?.setOnClickListener {
            binding?.recyclerView?.smoothScrollToPosition(0)
        }
    }

    override fun onDestroyView() {
        notificationObsoleter?.cancel()
        application.registerListener(null)
        getActivityBinding()?.toolbar?.setOnClickListener(null)
        super.onDestroyView()
    }

    private fun refreshItems() {
        CoroutineScope(Dispatchers.Main).launch {
            synchronized(localProximityItems) {
                updateNbItems()
                updateProximityList()
                updateTopBar()
            }
            if (binding?.recyclerView?.isComputingLayout == false)
                binding?.recyclerView?.adapter?.notifyDataSetChanged()
        }
    }

    private fun updateNbItems() {
        nbItemsCaption.text = "ebids: %d  |  pairs: %d  |  pings: %d"
            .format(
                localEbids.count(),
                localProximityItems.groupBy { it.ebidBase64 }.count(),
                localProximityItems.count()
            )
    }

    private fun updateProximityList() {
        proximityInfoList.text = when (showCompactList) {
            true -> compactList()
            false -> fullList()
        }
    }

    private fun updateLastNotification(notification: LocalProximity) {
        updateLastNotification("rssi: " + notification.calibratedRssi + "dBm")
    }

    private fun updateLastNotification(text: String = "-") {
        lastNotificationCaption.text = when (robertManager.isProximityActive) {
            true -> text
            false -> deactivated
        }
    }

    private fun updateTopBar() {
        val title = "%s  ".format(strings[getTitleKey()])
        val ebid = when(robertManager.isProximityActive) {
            true -> "(%s)".format(getCurrentEbidBase64())
            false -> ""
        }
        getActivityBinding()?.toolbar?.title =
            SpannableString(title + ebid).also {
                it.setSpan(RelativeSizeSpan(.6f), title.length, it.length,
                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            }
    }

    private fun getCurrentEbidBase64(): String {
        val currentEbid = robertManager.getCurrentEbid()
            ?: return " 🐭 "
        return Base64.encodeToString(currentEbid.ebid, Base64.NO_WRAP)
    }

    private val nbDisplayedItems = 300
    private fun fullList(): String {
        return localProximityItems
            .slice(0 until min(nbDisplayedItems, localProximityItems.size))
            .joinToString("\n") {
                listOf(
                    it.collectedTime.string,
                    it.ebidBase64,
                    it.calibratedRssi
                ).joinToString(", ")
            }
    }

    private fun compactList(): String {
        return localProximityItems
            .groupBy { it.ebidBase64 }
            .map { (_, group) ->
                formatMainLine(group.first()) +
                if (group.size > 1)
                    "\n" + formatSummary(group.last(), group.size)
                else
                    ""
            }
            .joinToString("\n")
    }

    private fun formatMainLine(it: LocalProximity): String {
        return "%s, %s".format(
            it.collectedTime.string,
            it.ebidBase64
        )
    }

    private fun formatSummary(prev: LocalProximity, count: Int): String {
        return "%s, ... (%d)".format(
            prev.collectedTime.string,
            count
        )
    }

    private var showCompactList = true
    private fun toggleListDisplay() {
        showCompactList = ! showCompactList
        refreshItems()
    }

    private var lastClick = 0L
    private fun toggleProximityScanner() {
        if (lastClick + 1000 > SystemClock.elapsedRealtime())
            return
        lastClick = SystemClock.elapsedRealtime()

        if (robertManager.isProximityActive) {
            robertManager.deactivateProximity(application)
            notificationObsoleter?.cancel()
            resetLastNotification(deactivated)
        }
        else CoroutineScope(Dispatchers.Default).launch {
            robertManager.activateProximity(application, false)
            resetLastNotification()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("StopCovid proximity data", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, strings["proximityController.toast.copied"], Toast.LENGTH_SHORT).also {
            it.setGravity(Gravity.CENTER, 0, 0)
        }.show()
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
        onClick = {
            toggleListDisplay()
        }
        onLongClick = {
            copyToClipboard(this.text ?: "")
        }
    }

    override fun notify(notification: Any) {
        (notification as com.orange.proximitynotification.ProximityInfo)
        .toLocalProximity()?.let {
            synchronized(lastNotificationCaption) {
                updateLastNotification(it)
            }
            synchronized(localProximityItems) {
                localProximityItems.add(0, it)
            }
            refreshItems()
            spawnNotificationObsoleter()
        }
    }

    private fun resetLastNotification(text: String = "-") {
        synchronized(lastNotificationCaption) {
            updateLastNotification(text)
        }
        refreshItems()
    }

    private var notificationObsoleter: Job? = null
    private fun spawnNotificationObsoleter() {
        notificationObsoleter?.cancel()
        notificationObsoleter = CoroutineScope(Dispatchers.Default).launch {
            try { delay(20000) }
            catch (e: CancellationException) { return@launch }
            resetLastNotification()
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

@android.annotation.SuppressLint("SimpleDateFormat")
val dateFormatter = SimpleDateFormat("E d MMM HH:mm:ss")
private val Long.string: String
    get() {
        return dateFormatter.format(Date(this.ntpTimeSToUnixTimeMs()))
    }
