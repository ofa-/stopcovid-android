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
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.fastitem.dynamicCardItem
import com.lunabeestudio.stopcovid.model.Section
import com.mikepenz.fastadapter.GenericItem

fun List<Section>.fillItems(items: MutableList<GenericItem>) {
    forEach { (section, description, links) ->
        if (items.size > 0) {
            items += spaceItem {
                spaceRes = R.dimen.spacing_medium
            }
        }
        items += dynamicCardItem {
            title = section
            caption = description
            this.links = links
            identifier = section.hashCode().toLong()
        }
    }
    items += spaceItem {
        spaceRes = R.dimen.spacing_large
        identifier = items.count().toLong()
    }
}