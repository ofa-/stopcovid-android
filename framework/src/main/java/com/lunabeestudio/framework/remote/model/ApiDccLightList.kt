/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/23/9 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote.model

internal class ApiDccLightList(
    val complete: Boolean,
    val lightCertificates: List<ApiDccLight>,
)

internal class ApiDccLight(
    val exp: Long,
    val dcc: String,
)