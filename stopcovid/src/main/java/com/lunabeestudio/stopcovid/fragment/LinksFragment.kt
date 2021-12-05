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

import android.os.Bundle
import android.view.View
import com.lunabeestudio.stopcovid.extension.fillItems
import com.mikepenz.fastadapter.GenericItem

class LinksFragment : MainFragment() {

    override fun getTitleKey(): String = "linksController.title"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        linksManager.linksSections.observe(viewLifecycleOwner) {
            refreshScreen()
        }
    }

    override suspend fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        linksManager.linksSections.value?.fillItems(items)

        return items
    }
}