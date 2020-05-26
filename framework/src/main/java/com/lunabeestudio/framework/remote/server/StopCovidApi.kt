/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.remote.server

import com.lunabeestudio.framework.BuildConfig
import com.lunabeestudio.framework.remote.model.ApiCommonRS
import com.lunabeestudio.framework.remote.model.ApiDeleteExposureHistoryRQ
import com.lunabeestudio.framework.remote.model.ApiRegisterRQ
import com.lunabeestudio.framework.remote.model.ApiRegisterRS
import com.lunabeestudio.framework.remote.model.ApiReportRQ
import com.lunabeestudio.framework.remote.model.ApiStatusRQ
import com.lunabeestudio.framework.remote.model.ApiStatusRS
import com.lunabeestudio.framework.remote.model.ApiUnregisterRQ
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface StopCovidApi {

    @POST("${BuildConfig.URL_PATH}/register")
    suspend fun register(@Body registerRQ: ApiRegisterRQ): Response<ApiRegisterRS>

    @POST("${BuildConfig.URL_PATH}/unregister")
    suspend fun unregister(@Body unregisterRQ: ApiUnregisterRQ): Response<ApiCommonRS>

    @POST("${BuildConfig.URL_PATH}/status")
    suspend fun status(@Body statusRQ: ApiStatusRQ): Response<ApiStatusRS>

    @POST("${BuildConfig.URL_PATH}/report")
    suspend fun report(@Body reportRQ: ApiReportRQ): Response<ApiCommonRS>

    @POST("${BuildConfig.URL_PATH}/deleteExposureHistory")
    suspend fun deleteExposureHistory(@Body deleteExposureHistoryRQ: ApiDeleteExposureHistoryRQ): Response<ApiCommonRS>
}