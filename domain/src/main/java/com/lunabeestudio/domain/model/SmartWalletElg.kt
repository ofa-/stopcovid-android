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

class SmartWalletElg(
    val vacc22DosesNbDays: Int,
    val vaccJan11DosesNbDays: Int,
    val recNbDays: Int,
    val vacc22DosesNbDaysLow: Int,
    val vaccJan11DosesNbDaysLow: Int,
    val recNbDaysLow: Int,
    val displayElgDays: Int,
    val vaccJan22DosesNbDays: Int,
    val vaccJan22DosesNbDaysLow: Int,
)