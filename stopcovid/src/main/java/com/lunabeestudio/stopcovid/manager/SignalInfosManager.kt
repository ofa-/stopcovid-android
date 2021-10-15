/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/16/06 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import android.view.Gravity
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.cardWithActionItem
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.manager.LocalizedStrings
import com.lunabeestudio.stopcovid.fastitem.defaultPhoneSupportItem
import com.lunabeestudio.stopcovid.fastitem.logoItem
import com.mikepenz.fastadapter.GenericItem

object SignalInfosManager {
    fun getInfosItems(context: Context, strings: LocalizedStrings): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        items += logoItem {
            imageRes = R.drawable.signal
            identifier = items.size.toLong()
        }
        items += cardWithActionItem {
            mainTitle = strings["venuesRecording.onboardingController.mainMessage.title"]
            mainBody = strings["venuesRecording.onboardingController.mainMessage.message"]
            mainGravity = Gravity.CENTER
            identifier = "venuesRecording.onboardingController.mainMessage.title".hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
        }
        items += cardWithActionItem {
            mainTitle = strings["venuesRecording.whenToUse.title"]
            mainBody = strings["venuesRecording.whenToUse.subtitle"]
            mainGravity = Gravity.CENTER
            identifier = "venuesRecording.whenToUse.title".hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
        }
        items += cardWithActionItem {
            mainTitle = strings["venuesRecording.alert.title"]
            mainBody = strings["venuesRecording.alert.subtitle"]
            mainGravity = Gravity.CENTER
            identifier = "venuesRecording.alert.title".hashCode().toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_large
        }

        items += defaultPhoneSupportItem(strings, context)

        items += spaceItem {
            spaceRes = R.dimen.spacing_large
            identifier = items.size.toLong()
        }

        return items
    }
}