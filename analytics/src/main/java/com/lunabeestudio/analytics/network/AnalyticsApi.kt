/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/01/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.analytics.network

import com.lunabeestudio.analytics.network.model.SendAnalyticsRQ
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

internal interface AnalyticsApi {

    @POST("api/{apiVersion}/analytics")
    suspend fun sendAnalytics(
        @Path("apiVersion") apiVersion: String,
        @Body body: SendAnalyticsRQ,
    ): Response<ResponseBody>
}