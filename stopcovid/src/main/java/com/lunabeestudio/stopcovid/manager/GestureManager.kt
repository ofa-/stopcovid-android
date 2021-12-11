/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.view.Gravity
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.coreui.fastitem.spaceItem
import com.lunabeestudio.stopcovid.coreui.fastitem.titleItem
import com.lunabeestudio.stopcovid.fastitem.logoBodyItem
import com.mikepenz.fastadapter.GenericItem

object GestureManager {

    fun fillItems(items: MutableList<GenericItem>, strings: Map<String, String>) {
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += titleItem {
            text = strings["onboarding.gesturesController.mainMessage.title"]
            gravity = Gravity.CENTER
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_xlarge
            identifier = items.size.toLong()
        }
        items += logoBodyItem {
            imageRes = R.drawable.ic_gesture_mask
            text = strings["onboarding.gesturesController.gesture7"]
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }
        items += logoBodyItem {
            imageRes = R.drawable.ic_gesture_air
            text = strings["onboarding.gesturesController.gesture9"]
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }
        items += logoBodyItem {
            imageRes = R.drawable.ic_gesture_hands
            text = strings["onboarding.gesturesController.gesture1"]
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }
        items += logoBodyItem {
            imageRes = R.drawable.ic_gesture_aircheck
            text = strings["onboarding.gesturesController.gesture4"]
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }
        items += logoBodyItem {
            imageRes = R.drawable.ic_gesture_distance
            text = strings["onboarding.gesturesController.gesture6"]
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }
        items += logoBodyItem {
            imageRes = R.drawable.ic_gesture_cough
            text = strings["onboarding.gesturesController.gesture2"]
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }
        items += logoBodyItem {
            imageRes = R.drawable.ic_gesture_tissue
            text = strings["onboarding.gesturesController.gesture3"]
            identifier = items.size.toLong()
        }
        items += spaceItem {
            spaceRes = R.dimen.spacing_medium
            identifier = items.size.toLong()
        }
    }
}