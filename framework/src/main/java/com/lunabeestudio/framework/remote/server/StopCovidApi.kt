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

import com.lunabeestudio.framework.remote.model.ApiCaptchaRS
import com.lunabeestudio.framework.remote.model.ApiCommonRS
import com.lunabeestudio.framework.remote.model.ApiDeleteExposureHistoryRQ
import com.lunabeestudio.framework.remote.model.ApiRegisterRS
import com.lunabeestudio.framework.remote.model.ApiRegisterV2RQ
import com.lunabeestudio.framework.remote.model.ApiReportRQ
import com.lunabeestudio.framework.remote.model.ApiStatusRQ
import com.lunabeestudio.framework.remote.model.ApiStatusRS
import com.lunabeestudio.framework.remote.model.ApiUnregisterRQ
import com.lunabeestudio.framework.remote.model.CaptchaRQ
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface StopCovidApi {

    @POST("/api/{apiVersion}/captcha")
    suspend fun captcha(@Path("apiVersion") apiVersion: String, @Body captchaRQ: CaptchaRQ): Response<ApiCaptchaRS>

    @GET("/api/{apiVersion}/captcha/{captchaId}/{type}")
    suspend fun getCaptcha(@Path("apiVersion") apiVersion: String,
        @Path("captchaId") captchaId: String,
        @Path("type") type: String): Response<ResponseBody>

    @POST("/api/{apiVersion}/register")
    suspend fun registerV2(@Path("apiVersion") apiVersion: String, @Body registerRQ: ApiRegisterV2RQ): Response<ApiRegisterRS>

    @POST("/api/{apiVersion}/unregister")
    suspend fun unregister(@Path("apiVersion") apiVersion: String, @Body unregisterRQ: ApiUnregisterRQ): Response<ApiCommonRS>

    @POST("/api/{apiVersion}/status")
    suspend fun status(@Path("apiVersion") apiVersion: String, @Body statusRQ: ApiStatusRQ): Response<ApiStatusRS>

    @POST("/api/{apiVersion}/report")
    suspend fun report(@Path("apiVersion") apiVersion: String, @Body reportRQ: ApiReportRQ): Response<ApiCommonRS>

    @POST("/api/{apiVersion}/deleteExposureHistory")
    suspend fun deleteExposureHistory(@Path("apiVersion") apiVersion: String,
        @Body deleteExposureHistoryRQ: ApiDeleteExposureHistoryRQ): Response<ApiCommonRS>
}