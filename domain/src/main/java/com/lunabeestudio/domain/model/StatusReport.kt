/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.domain.model

class StatusReport(
    val riskLevel: Float,
    val ntpLastContactS: Long?,
    val ntpLastRiskScoringS: Long?,
    val message: String?,
    val tuples: String?,
    val declarationToken: String?,
)
