/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/15/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fastitem

import com.lunabeestudio.stopcovid.coreui.R

class LightButtonItem : ButtonItem(
    layout = R.layout.item_light_button,
    id = R.id.item_light_button
)

fun lightButtonItem(block: (LightButtonItem.() -> Unit)): LightButtonItem = LightButtonItem()
    .apply(block)