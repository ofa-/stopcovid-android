/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/01/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.coreui.model.CardTheme
import com.lunabeestudio.stopcovid.extension.openInExternalBrowser
import com.mikepenz.fastadapter.GenericItem

class VaccinationFragment : MainFragment() {

    override fun getTitleKey(): String = "vaccinationController.title"

    override fun getItems(): List<GenericItem> {
        val items = ArrayList<GenericItem>()

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        items += cardWithActionItem(CardTheme.Default) {
            mainTitle = strings["vaccinationController.eligibility.title"]
            mainBody = strings["vaccinationController.eligibility.subtitle"]
            actions = listOf(
                Action(label = strings["vaccinationController.eligibility.buttonTitle"]) {
                    strings["vaccinationController.eligibility.url"]?.openInExternalBrowser(it.context)
                }
            )
            identifier = "vaccinationController.eligibility.title".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }

        items += cardWithActionItem(CardTheme.Default) {
            mainTitle = strings["vaccinationController.vaccinationLocation.title"]
            mainBody = strings["vaccinationController.vaccinationLocation.subtitle"]
            actions = listOf(
                Action(label = strings["vaccinationController.vaccinationLocation.buttonTitle"]) {
                    strings["vaccinationController.vaccinationLocation.url"]?.openInExternalBrowser(it.context)
                }
            )
            identifier = "vaccinationController.vaccinationLocation.title".hashCode().toLong()
        }

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.count().toLong()
        }

        return items
    }

}