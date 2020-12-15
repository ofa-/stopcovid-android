/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote.model

import com.lunabeestudio.domain.model.ReportResponse

internal class ApiReportRS(
    val success: Boolean,
    val message: String?,
    val reportValidationToken: String?,
)

internal fun ApiReportRS.toDomain() = ReportResponse(
    reportValidationToken = reportValidationToken
)