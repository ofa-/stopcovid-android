/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/22/03 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.domain.model

data class Attestation(
    val id: String,
    val qrCode: String,
    val footer: String,
    val qrCodeString: String,
    val timestamp: Long,
    val reason: String,
    val widgetString: String
)