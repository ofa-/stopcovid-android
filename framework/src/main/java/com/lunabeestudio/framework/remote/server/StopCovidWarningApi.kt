/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote.server

import com.lunabeestudio.framework.remote.model.ApiCommonRS
import com.lunabeestudio.framework.remote.model.ApiWReportRQ
import com.lunabeestudio.framework.remote.model.ApiWStatusRQ
import com.lunabeestudio.framework.remote.model.ApiWStatusRS
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

internal interface StopCovidWarningApi {

    @POST("/api/{warningApiVersion}/wstatus")
    suspend fun wstatus(
        @Path("warningApiVersion") apiVersion: String,
        @Body statusRQ: ApiWStatusRQ,
    ): Response<ApiWStatusRS>

    @POST("/api/{warningApiVersion}/wreport")
    suspend fun wreport(
        @Path("warningApiVersion") apiVersion: String,
        @Header("Authorization") bearer: String,
        @Body reportRQ: ApiWReportRQ,
    ): Response<ApiCommonRS>
}