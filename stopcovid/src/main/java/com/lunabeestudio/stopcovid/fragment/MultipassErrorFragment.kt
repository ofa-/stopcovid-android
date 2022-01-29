/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2022/1/18 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import androidx.navigation.fragment.navArgs
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.fastitem.defaultPhoneSupportItem
import com.mikepenz.fastadapter.GenericItem

class MultipassErrorFragment : MainFragment() {

    private val args by navArgs<MultipassErrorFragmentArgs>()

    override fun getTitleKey(): String = "multiPass.generation.errorScreen.title"

    override suspend fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }
        if (args.notEnoughDcc) {
            items += cardWithActionItem {
                mainTitle = strings["multiPass.selectionScreen.error.explanation.title"]
                mainBody = stringsFormat("multiPass.selectionScreen.error.explanation.subtitle", args.displayName)
                identifier = "multiPass.selectionScreen.error.explanation.title".hashCode().toLong()
            }
        } else {
            val error = args.errorKeys.mapNotNull { key ->
                ("\n\n" + strings["multiPass.errors.$key"].orEmpty()).takeIf { it.isNotBlank() }
            }.joinToString("")
            items += cardWithActionItem {
                mainTitle = strings["multiPass.generation.errorScreen.explanation.title"]
                mainBody = stringsFormat("multiPass.generation.errorScreen.explanation.subtitle", error)
                identifier = "multiPass.selectionScreen.error.explanation.title".hashCode().toLong()
            }
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        context?.let { ctx -> items += defaultPhoneSupportItem(strings, ctx) }

        return items
    }
}