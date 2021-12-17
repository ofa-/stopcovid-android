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
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.framework.ble.extension.toLocalProximity
import com.lunabeestudio.framework.extension.localProximityFromString
import com.lunabeestudio.framework.extension.localProximityToString
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManagerImpl
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.manager.ProximityManager
import com.lunabeestudio.stopcovid.service.ProximityService
import com.mikepenz.fastadapter.GenericItem
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import kotlin.math.min


class TuneProximityFragment : MainFragment(), RobertApplication.Listener {

    private var localProximityItems = mutableListOf<LocalProximity>()
    private var localEbids = mutableListOf<EphemeralBluetoothIdentifier>()

    override fun getTitleKey(): String = "proximityController.tuneProximity"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTopBarOnclick()
        updateLastNotification()
        refreshItems()
        initLocalProximityItems()
    }

    private suspend fun initLocalEbids() {
        localEbids.addAll(robertManager.getLocalEbids())
        application.registerListener(this)
    }

    private fun initLocalProximityItems() {
        val anim = loadingAnimation()
        CoroutineScope(Dispatchers.Default).launch {
            initLocalEbids()
            val items = getLocalProximityData()
            anim.cancel()
            localProximityItems = items
            refreshItems()
        }
    }

    private fun getLocalProximityData(): MutableList<LocalProximity> {
        return if (robertManager.localProximityFile.exists()) {
            readFromLocalFile()
        }
        else {
            robertManager.getLocalProximityItems(0)
                .sortedBy { it.collectedTime }
                .also { saveToLocalFile(it) }
        }
            .reversed().toMutableList()
    }

    private fun readFromLocalFile(): List<LocalProximity> {
        val file = robertManager.localProximityFile
        synchronized(file) {
            return file.readLines().mapNotNull {
                try {
                    localProximityFromString(it)
                } catch (e: Throwable) {
                    null
                }
            }
        }
    }

    private fun saveToLocalFile(items: List<LocalProximity>) {
        items.forEach {
            robertManager.localProximityFile.appendText(
                localProximityToString(it).plus("\n")
            )
        }
    }

    private fun loadingAnimation(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            val anim = mutableListOf("  .  ", " o ", " O ", "😺", " O ", " o ")
            while (true) {
                nbItemsCaption.text = "ebids: -  |  pairs: %s  |  pings: -"
                    .format(
                        anim.first()
                    )
                refresh()
                delay(200)
                anim.add(anim.removeAt(0))
            }
        }
    }

    private lateinit var robertManager: RobertManagerImpl
    private lateinit var application: RobertApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        application = context?.applicationContext as RobertApplication
        robertManager = application.robertManager as RobertManagerImpl
    }

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
        }
        refresh()
    }

    private fun refresh() {
        CoroutineScope(Dispatchers.Main).launch {
            // if (binding?.recyclerView?.isComputingLayout == false)
            binding?.recyclerView?.adapter?.notifyDataSetChanged()
        }
    }

    private fun updateNbItems() {
        nbItemsCaption.text = "ebids: %d  |  pairs: %d  |  pings: %d"
            .format(
                localEbids.filter { it.ntpEndTimeS > ntpNow }.count(),
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
        updateLastNotification("${notification.shortEbid} / ${notification.calibratedRssi}dBm")
    }

    private fun updateLastNotification(text: String = "-") {
        lastNotificationCaption.text = when (isProximityActive) {
            true -> text
            false -> deactivated
        }
    }

    private fun updateTopBar() {
        val title = "%s  ".format(strings[getTitleKey()])
        val ebid = when(isProximityActive) {
            true -> "(%s)".format(currentEbidAsString())
            false -> ""
        }
        getActivityBinding()?.toolbar?.title =
            SpannableString(title + ebid).also {
                it.setSpan(RelativeSizeSpan(.6f), title.length, it.length,
                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            }
    }

    private fun currentEbidAsString(): String {
        return robertManager.getCurrentEbid()?.short
            ?: "❣️  " + (localEbids.lastOrNull()?.short ?: " 🐭 ")
    }

    private val nbDisplayedItems = 300
    private fun fullList(): String {
        return localProximityItems
            .slice(0 until min(nbDisplayedItems, localProximityItems.size))
            .groupBy { it.collectedTime.shortDate }
            .map { (day, group) ->
                dayHeader(day, "_____") +
                group.joinToString("\n") {
                    "%s  %s  %d".format(
                        it.collectedTime.longTime,
                        it.shortEbid,
                        it.calibratedRssi
                    )
                }
            }
            .joinToString("\n")
    }

    private val minPings = 10
    private val nbConsideredItems = 20000
    private fun compactList(): String {
        var currentDay = ""
        var currentSlot = 0L
        var nbEbidPerDay = 0
        var nbLowPingEbids = 0
        return localProximityItems
            .slice(0 until min(nbConsideredItems, localProximityItems.size))
            .groupBy { it.ebidBase64 }
            .map { (_, group) ->
                val it = group.last()
                val slot = it.collectedTime / (60*15)
                val duration = group.first().collectedTime - it.collectedTime
                val day = it.collectedTime.shortDate
                if (day != currentDay) {
                    currentDay = day
                    val stats = statsLine(nbLowPingEbids, nbEbidPerDay)
                    nbLowPingEbids = 0
                    nbEbidPerDay = 1
                    currentSlot = 0
                    stats + dayHeader(day, "________")
                } else { nbEbidPerDay += 1; "" } +
                if (group.count() < minPings) {
                    nbLowPingEbids += 1
                    ""
                }
                else {
                (if (currentSlot != slot) {
                    val firstTime = currentSlot == 0L
                    currentSlot = slot
                    if (!firstTime)
                        "\n_\n" else ""
                } else "") +
                "%s  [%d'%02d\"]  %s  (%d)".format(
                    it.collectedTime.shortTime,
                    duration / 60,
                    duration % 60,
                    it.shortEbid,
                    group.count()
                )
                }
            }
            .filterNot { it == "" }
            .joinToString("\n")
    }

    private fun statsLine(lpe: Int, pde: Int): String {
        val long = strings["proximityController.stats.long"]
        val short = strings["proximityController.stats.short"]

        return if (pde == 0) ""
        else
            (if (pde != lpe) "\n" else "") +
            "(%d ebids, %d ${long}, %d ${short})\n\n"
                .format(pde, pde-lpe, lpe)
    }

    private fun showRemainingEbids() {
        proximityInfoList.text = localEbids
            .filter { it.ntpEndTimeS > ntpNow }
            .run { ebidsListToString(this) }
        refresh()
        showCompactList = false
    }

    private fun ebidsListToString(list: List<EphemeralBluetoothIdentifier>) =
        list.groupBy { it.ntpStartTimeS.shortDate }
            .map { (day, group) ->
                dayHeader(day) +
                group.joinToString("\n") {
                    "%s  %s".format(
                        it.ntpStartTimeS.shortTime,
                        it.short
                    )
                }
            }
            .joinToString("\n")

    private fun dayHeader(day: String, header: String = "____") =
        "\n%s  %s  %s\n\n".format(header, day, header)

    private var isLoading = false
    private fun showDisseminatedEbids() {
        val file = robertManager.disseminatedEbidsFile
        if (!file.exists()) return toast("no file")
        if (isLoading) return toast("loading...")
        isLoading = true
        CoroutineScope(Dispatchers.Default).launch {
            proximityInfoList
                .text = loadDisseminatedEbids(file)
                    .reversed()
                    .run { ebidsListToString(this) }
            refresh()
            isLoading = false
            showCompactList = false
        }
    }

    private fun loadDisseminatedEbids(file: File): List<EphemeralBluetoothIdentifier> {
        return synchronized(file) {
            file.readLines().mapNotNull {
                try {
                    EphemeralBluetoothIdentifier.fromString(it)
                }
                catch (e: Throwable) { null }
            }
        }
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

        showCompactList = true
        if (isProximityActive) {
            robertManager.deactivateProximity(application)
            notificationObsoleter?.cancel()
            resetLastNotification(deactivated)
        }
        else CoroutineScope(Dispatchers.Default).launch {
            robertManager.activateProximity(application, false)
            resetLastNotification()
        }
    }

    private val isProximityActive get() = robertManager.isProximityActive

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("StopCovid proximity data", text)
        clipboard.setPrimaryClip(clip)
        toast(strings["proximityController.toast.copied"])
    }

    private fun toast(text: String?) {
        toast(requireContext(), text)
    }

    override fun refreshScreen() {
        super.refreshScreen()
        updateLastNotification()
    }

    private val deactivated get() = "(%s)".format(
        strings["proximityController.switch.subtext.deactivated"]
    )

    private val nbItemsCaption = captionItem {
        gravity = Gravity.CENTER
        onClick = {
            showDisseminatedEbids()
        }
        onLongClick = {
            showRemainingEbids()
        }
        ripple = true
    }

    private val lastNotificationCaption = captionItem {
        gravity = Gravity.CENTER
        onLongClick = {
            toggleProximityScanner()
        }
        ripple = true
    }

    private val proximityInfoList = captionItem {
        onClick = {
            toggleListDisplay()
        }
        onLongClick = {
            copyToClipboard(this.text.toString())
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
            try { delay(ProximityService.proximityNotificationTimeout) }
            catch (e: CancellationException) { return@launch }
            resetLastNotification()
        }
    }

    override suspend fun getItems(): List<GenericItem> {
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
private fun String.formatDate(date: Long)
        = SimpleDateFormat(this).format(date)

private val Long.longTime
    get() = "HH:mm:ss".formatDate(
            this.ntpTimeSToUnixTimeMs())

private val Long.shortTime
    get() = "HH:mm".formatDate(
            this.ntpTimeSToUnixTimeMs() + if (this % 60 > 30) 30000 else 0)

private val Long.shortDate
    get() = "E d MMM".formatDate(
            this.ntpTimeSToUnixTimeMs())

private val EphemeralBluetoothIdentifier.base64
    get() = Base64.encodeToString(this.ebid, Base64.NO_WRAP)

private val EphemeralBluetoothIdentifier.short
    get() = this.base64.substring(0..5)

private val LocalProximity.shortEbid
    get() = this.ebidBase64.substring(0..5)

private val ntpNow
    get() = System.currentTimeMillis().unixTimeMsToNtpTimeS()
