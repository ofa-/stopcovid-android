/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.remote.model

import com.lunabeestudio.domain.model.StatusReport

internal class ApiStatusRS(
    val atRisk: Boolean,
    val lastExposureTimeframe: Int?,
    val message: String?,
    val tuples: String,
    val config: List<ApiConfigurationRS>
)

internal fun ApiStatusRS.toDomain() = StatusReport(
    atRisk = atRisk,
    lastExposureTimeframe = lastExposureTimeframe,
    message = message,
    tuples = tuples,
    config = config.map {
        it.toDomain()
    }
)