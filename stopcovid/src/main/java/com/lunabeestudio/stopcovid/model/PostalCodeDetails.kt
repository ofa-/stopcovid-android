/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/04/02 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.model

import com.google.gson.annotations.SerializedName

class PostalCodeDetails(
    @SerializedName("dept")
    val department: String,
    @SerializedName("lat")
    val latitude: Double?,
    @SerializedName("long")
    val longitude: Double?,
)