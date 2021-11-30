/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/10/04 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.extension.callPhone
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.lunabeestudio.stopcovid.fastitem.phoneSupportItem
import com.lunabeestudio.stopcovid.fastitem.videoPlayerItem
import com.mikepenz.fastadapter.GenericItem

class UrgentInfoFragment : MainFragment() {
    override fun getTitleKey(): String = "dgsUrgentController.title"

    private var hideMediaController: (() -> Unit)? = null

    private var onScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            hideMediaController?.invoke()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // To avoid media controller glitch on scroll
        binding?.recyclerView?.addOnScrollListener(onScrollListener)
    }

    override suspend fun getItems(): List<GenericItem> {

        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
        }

        strings["dgsUrgentController.videoUrl"]?.let {
            items += videoPlayerItem {
                url = it
                hideMediaController = this::hideMediaController
                autoPlay = true
                retryContentDescription = strings["common.tryAgain"]
                identifier = "dgsUrgentController.videoUrl".hashCode().toLong()
            }

            items += spaceItem {
                spaceRes = R.dimen.spacing_large
            }
        }

        items += cardWithActionItem {
            mainTitle = strings["dgsUrgentController.section.title"]
            mainBody = strings["dgsUrgentController.section.desc"]
            actions = strings["dgsUrgentController.section.url"]?.let { url ->
                listOf(
                    Action(label = strings["dgsUrgentController.section.labelUrl"]) {
                        context?.let(url::openInExternalBrowser)
                    }
                )
            }
            identifier = "dgsUrgentController.section.title".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
        }

        strings["dgsUrgentController.phone.number"]?.let { number ->
            items += phoneSupportItem {
                title = strings["dgsUrgentController.phone.title"]
                subtitle = strings["dgsUrgentController.phone.subtitle"]
                onClick = {
                    context?.let { number.callPhone(it) }
                }
                identifier = "dgsUrgentController.phone.title".hashCode().toLong()
            }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
        }

        return items
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.recyclerView?.removeOnScrollListener(onScrollListener)
    }
}