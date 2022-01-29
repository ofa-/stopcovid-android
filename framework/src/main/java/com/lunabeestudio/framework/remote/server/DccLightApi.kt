/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/22/9 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote.server

import com.lunabeestudio.framework.remote.model.ApiAggregateRQ
import com.lunabeestudio.framework.remote.model.ApiAggregateRS
import com.lunabeestudio.framework.remote.model.ApiGenerateRQ
import com.lunabeestudio.framework.remote.model.ApiGenerateRS
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface DccLightApi {
    @POST("/api/v1/generate")
    suspend fun generate(
        @Body apiGenerateRQ: ApiGenerateRQ,
    ): Response<ApiGenerateRS>

    @POST("/api/v1/aggregate")
    suspend fun aggregate(
        @Body apiAggregateRQ: ApiAggregateRQ,
    ): Response<ApiAggregateRS>
}