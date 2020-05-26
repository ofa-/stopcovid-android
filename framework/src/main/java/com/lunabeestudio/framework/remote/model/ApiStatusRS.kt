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

import com.lunabeestudio.domain.RobertConstant
import com.lunabeestudio.domain.model.StatusReport

internal class ApiStatusRS(
    val atRisk: Boolean,
    val lastExposureTimeframe: Int?,
    val message: String?,
    val idsForEpochs: List<ApiIdListRS>,
    val filteringAlgoConfig: List<ApiClientFilteringAlgorithmConfigurationRS>
)

internal fun ApiStatusRS.toDomain(ntpInitialTimeS: Long) = StatusReport(
    atRisk,
    lastExposureTimeframe,
    message,
    idsForEpochs.map { apiIdListRS ->
        apiIdListRS.toDomain(ntpInitialTimeS, RobertConstant.EPOCH_DURATION_S)
    },
    filteringAlgoConfig.map {
        it.toDomain()
    }
)