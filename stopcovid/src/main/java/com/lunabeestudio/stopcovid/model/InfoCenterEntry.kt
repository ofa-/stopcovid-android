/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/21/09 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

data class InfoCenterEntry(
    val titleKey: String,
    val descriptionKey: String,
    val buttonLabelKey: String?,
    val urlKey: String?,
    val timestamp: Long,
    val tagIds: List<String>?
)