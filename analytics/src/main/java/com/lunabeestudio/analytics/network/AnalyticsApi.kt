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

import com.lunabeestudio.analytics.network.model.SendAppAnalyticsRQ
import com.lunabeestudio.analytics.network.model.SendHealthAnalyticsRQ
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface AnalyticsApi {

    @POST("api/{apiVersion}/analytics")
    suspend fun sendAppAnalytics(
        @Path("apiVersion") apiVersion: String,
        @Body body: SendAppAnalyticsRQ,
        @Header("Authorization") bearerToken: String,
    ): Response<ResponseBody>

    @POST("api/{apiVersion}/analytics")
    suspend fun sendHealthAnalytics(
        @Path("apiVersion") apiVersion: String,
        @Body body: SendHealthAnalyticsRQ,
        @Header("Authorization") bearerToken: String,
    ): Response<ResponseBody>

    @DELETE("api/{apiVersion}/analytics")
    suspend fun deleteAnalytics(
        @Path("apiVersion") apiVersion: String,
        @Query("installationUuid") installationUuid: String,
        @Header("Authorization") bearerToken: String,
    ): Response<ResponseBody>
}