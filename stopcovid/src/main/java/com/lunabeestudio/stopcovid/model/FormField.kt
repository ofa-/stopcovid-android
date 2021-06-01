/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

data class FormField(
    val key: String,
    val type: String,
    val value: String,
    val contentType: String?,
    val defaultValue: String?,
    val items: List<FormEntryItem>?,
    val dataKey: String?
) {
    val dataKeyValue: String
        get() = dataKey ?: key
}

data class FormEntryItem(
    val code: String
)