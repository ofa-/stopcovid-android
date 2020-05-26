/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import com.lunabeestudio.stopcovid.manager.GestureManager
import com.mikepenz.fastadapter.GenericItem

class GestureFragment : MainFragment() {

    override fun getTitleKey(): String = "onboarding.gesturesController.title"

    override fun getItems(): List<GenericItem> {
        val items = arrayListOf<GenericItem>()

        GestureManager.fillItems(items, strings)

        return items
    }
}