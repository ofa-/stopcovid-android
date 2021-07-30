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

import com.lunabeestudio.framework.remote.model.ApiConversionRQ
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

internal interface InGroupeApi {

    @POST("/api/client/convertor/decode/decodeDocument")
    suspend fun convertV1(
        @Body apiConversionRQ: ApiConversionRQ,
    ): Response<ResponseBody>

    @POST("/api/v2/client/convertor/decode/decodeDocument")
    suspend fun convertV2(
        @Query("publicKey") publicKey: String,
        @Query("keyAlias") keyId: String,
        @Body apiConversionRQ: ApiConversionRQ,
    ): Response<ResponseBody>
}