/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.extension

import com.lunabeestudio.robert.model.AtRiskStatus

fun Boolean?.toAtRiskStatus(): AtRiskStatus = when (this) {
    true -> AtRiskStatus.AT_RISK
    false -> AtRiskStatus.NOT_AT_RISK
    null -> AtRiskStatus.UNKNOWN
}