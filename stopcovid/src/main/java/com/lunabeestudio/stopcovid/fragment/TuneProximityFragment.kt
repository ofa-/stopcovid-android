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
import android.view.View
import android.view.Gravity
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.lightButtonItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.mikepenz.fastadapter.GenericItem

import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManagerImpl
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.stopcovid.extension.robertManager
import com.lunabeestudio.framework.ble.extension.toLocalProximity

import android.util.Log

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.util.Date


class TuneProximityFragment : MainFragment(), RobertApplication.Listener {

    lateinit var localProximityItems: MutableList<LocalProximity>

    fun initLocalProximityItems() {
        localProximityItems = (requireContext().robertManager() as RobertManagerImpl)
            .getLocalProximityItems(0)
            .toMutableList()
    }

    override fun getTitleKey(): String = "tuneProximityController.title"


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CoroutineScope(Dispatchers.Default).launch {
            initLocalProximityItems()
            refreshItems()
        }

        application.registerListener(this)
    }

    private val application: RobertApplication
        get() = (requireContext().applicationContext as RobertApplication)

    override fun onDestroyView() {
        application.registerListener(NullListener())
        super.onDestroyView()
    }

    class NullListener: RobertApplication.Listener {
        override fun notify(notification: Any) {}
    }

    private fun refreshItems() {
    CoroutineScope(Dispatchers.Main).launch {
            nbItemsCaption.text = nbItemsTxt + localProximityItems.count()
            proximityInfoList.text = localProximityItemsToString()

            if (binding?.recyclerView?.isComputingLayout == false)
                binding?.recyclerView?.adapter?.notifyDataSetChanged()
        }
    }

    private fun localProximityItemsToString(): String {
        return localProximityItems
            .reversed()
            .slice(0..99)
            .map { it -> listOf(
                Date((it.collectedTime - 2208988800) * 1000),
                it.calibratedRssi
              ).joinToString(", ") }
            .joinToString("\n")
    }

    val nbItemsTxt = "nb local proximity items: "
    val nbItemsCaption = captionItem {
            text = nbItemsTxt + "..."
            gravity = Gravity.CENTER
    }

    val proximityInfoList = captionItem {
    }

    override fun notify(notification: Any) {
        (notification as com.orange.proximitynotification.ProximityInfo)
        .toLocalProximity()?.let {
            localProximityItems.add(it)
            refreshItems()
        }
    }

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += dividerItem {}

        items += nbItemsCaption

        items += dividerItem {}
        items += proximityInfoList
        items += dividerItem {}

        return items
    }
}
