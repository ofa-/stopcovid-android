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
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.model.Section
import com.mikepenz.fastadapter.GenericItem

fun List<Section>.fillItems(items: MutableList<GenericItem>) {
    forEach { (section, description, links) ->
        if (items.size > 0) {
            items += spaceItem {
                spaceRes = R.dimen.spacing_medium
            }
        }
        items += titleItem {
            text = section
            identifier = section.hashCode().toLong()
        }
        description?.let { description ->
            items += captionItem {
                text = description
                identifier = description.hashCode().toLong()
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_large
            }
        }
        links?.forEach { link ->
            items += linkItem {
                text = link.label
                url = link.url
                identifier = text.hashCode().toLong()
            }
        }
        items += dividerItem {}
    }
}