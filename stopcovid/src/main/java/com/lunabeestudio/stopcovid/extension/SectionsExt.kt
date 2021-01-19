/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.model.Section
import com.mikepenz.fastadapter.GenericItem

fun List<Section>.fillItems(items: MutableList<GenericItem>) {
    items += spaceItem {
        spaceRes = R.dimen.spacing_large
        identifier = items.count().toLong()
    }
    forEach { (section, description, links) ->
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
        }
        items += cardWithActionItem {
            mainTitle = section
            mainBody = description
            actions = links?.map { link ->
                Action(label = link.label) {
                    link.url.openInExternalBrowser(it.context)
                }
            }
            identifier = section.hashCode().toLong()
        }
    }
    items += spaceItem {
        spaceRes = R.dimen.spacing_large
        identifier = items.count().toLong()
    }
}