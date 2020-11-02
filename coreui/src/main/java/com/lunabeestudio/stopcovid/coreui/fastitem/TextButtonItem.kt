/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/15/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui.fastitem

import com.lunabeestudio.stopcovid.coreui.R

class TextButtonItem : ButtonItem(
    layout = R.layout.item_text_button,
    id = R.id.item_text_button
)

fun textButtonItem(block: (TextButtonItem.() -> Unit)): TextButtonItem = TextButtonItem()
    .apply(block)