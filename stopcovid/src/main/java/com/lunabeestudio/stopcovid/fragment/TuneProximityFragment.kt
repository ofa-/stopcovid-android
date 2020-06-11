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

import com.lunabeestudio.robert.RobertManagerImpl
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.stopcovid.extension.robertManager

import android.util.Log

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class TuneProximityFragment : MainFragment() {

    lateinit var localProximityItems: List<LocalProximity>

    fun initLocalProximityItems() {
        localProximityItems = (requireContext().robertManager() as RobertManagerImpl)
            .getLocalProximityItems(0)
    }

    override fun getTitleKey(): String = "tuneProximityController.title"


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CoroutineScope(Dispatchers.Default).launch {
	    Log.d("OFA", "initLocalProximityItems - begin")
            initLocalProximityItems()
            refreshItems()
            Log.d("OFA", "initLocalProximityItems - end")
        }
    }

    private fun refreshItems() {
    CoroutineScope(Dispatchers.Main).launch {
            nbItemsCaption.text = "nb local proximity items: " + localProximityItems.count()

            if (binding?.recyclerView?.isComputingLayout == false)
                binding?.recyclerView?.adapter?.notifyDataSetChanged()
        }
    }

    val nbItemsCaption = captionItem {
            text = "nb stored proximity items: ..."
            gravity = Gravity.CENTER
    }

    override fun getItems(): List<GenericItem> {
            Log.d("OFA", "getItems")
        val items = ArrayList<GenericItem>()

        items += dividerItem {}

        items += nbItemsCaption

        items += dividerItem {}

        return items
    }
}
