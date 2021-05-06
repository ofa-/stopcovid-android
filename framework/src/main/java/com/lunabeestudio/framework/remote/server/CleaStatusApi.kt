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

import com.lunabeestudio.framework.remote.model.ApiCluster
import com.lunabeestudio.framework.remote.model.ApiClusterIndex
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

internal interface CleaStatusApi {

    @GET("{apiVersion}/clusterIndex.json")
    suspend fun getClusterIndex(
        @Path("apiVersion") apiVersion: String
    ): Response<ApiClusterIndex>

    @GET("{apiVersion}/{iteration}/{prefix}.json")
    suspend fun getClusterList(
        @Path("apiVersion") apiVersion: String,
        @Path("iteration") iterationNumber: String,
        @Path("prefix") prefix: String,
    ): Response<List<ApiCluster>>
}
