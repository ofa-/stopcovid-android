/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.extension

import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.captionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.dividerItem
import com.lunabeestudio.stopcovid.fastitem.linkItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.model.PrivacySection
import com.mikepenz.fastadapter.GenericItem

fun List<PrivacySection>.fillItems(items: MutableList<GenericItem>) {
    forEach { privacySection ->
        if (items.size > 0) {
            items += spaceItem {
                spaceRes = R.dimen.spacing_medium
            }
        }
        items += titleItem {
            text = privacySection.section
            identifier = privacySection.section.hashCode().toLong()
        }
        privacySection.description?.let { description ->
            items += captionItem {
                text = description
                identifier = privacySection.description.hashCode().toLong()
            }
            items += spaceItem {
                spaceRes = R.dimen.spacing_large
            }
        }
        privacySection.links?.forEach { link ->
            items += linkItem {
                text = link.label
                url = link.url
                identifier = text.hashCode().toLong()
            }
        }
        items += dividerItem {}
    }
}