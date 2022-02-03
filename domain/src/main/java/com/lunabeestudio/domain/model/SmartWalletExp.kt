/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/29/11 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.domain.model

class SmartWalletExp(
    val pivot1: String,
    val pivot2: String,
    val pivot3: String,
    val vacc22DosesNbDays: Int,
    val vacc11DosesNbDays: Int,
    val vacc22DosesNbNewDays: Int,
    val vacc11DosesNbNewDays: Int,
    val recNbDays: Int,
    val vaccJan11DosesNbDays: Int,
    val displayExpOnAllDcc: Int,
    val displayExpDays: Int,
    val vaccJan22DosesNbDays: Int,
    val vaccJan22DosesNbNewDays: Int,
    val recNbNewDays: Int,
)