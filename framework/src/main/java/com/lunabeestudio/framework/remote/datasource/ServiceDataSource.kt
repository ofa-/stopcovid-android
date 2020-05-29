/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.remote.datasource

import android.content.Context
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.domain.model.RegisterReport
import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.domain.model.StatusReport
import com.lunabeestudio.framework.BuildConfig
import com.lunabeestudio.framework.remote.RetrofitClient
import com.lunabeestudio.framework.remote.extension.remoteToRobertException
import com.lunabeestudio.framework.remote.model.ApiCommonRS
import com.lunabeestudio.framework.remote.model.ApiDeleteExposureHistoryRQ
import com.lunabeestudio.framework.remote.model.ApiRegisterRQ
import com.lunabeestudio.framework.remote.model.ApiReportRQ
import com.lunabeestudio.framework.remote.model.ApiStatusRQ
import com.lunabeestudio.framework.remote.model.ApiUnregisterRQ
import com.lunabeestudio.framework.remote.model.toDomain
import com.lunabeestudio.framework.remote.server.StopCovidApi
import com.lunabeestudio.robert.datasource.RemoteServiceDataSource
import com.lunabeestudio.robert.model.BackendException
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import kotlinx.coroutines.delay
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ServiceDataSource(context: Context, baseUrl: String = BuildConfig.BASE_URL) : RemoteServiceDataSource {

    private var api: StopCovidApi = RetrofitClient.getService(context, StopCovidApi::class.java, baseUrl.toHttpUrl())

    override suspend fun register(captcha: String, clientPublicECDHKey: String): RobertResultData<RegisterReport> {
        val result = tryCatchRequestData {
            api.register(ApiRegisterRQ(captcha = captcha, clientPublicECDHKey = clientPublicECDHKey))
        }
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.toDomain())
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }

    override suspend fun unregister(ssu: ServerStatusUpdate): RobertResult {
        return tryCatchRequest {
            api.unregister(ApiUnregisterRQ(ebid = ssu.ebid, epochId = ssu.epochId, time = ssu.time, mac = ssu.mac))
        }
    }

    override suspend fun status(ssu: ServerStatusUpdate): RobertResultData<StatusReport> {
        val result = tryCatchRequestData {
            api.status(ApiStatusRQ(ebid = ssu.ebid,
                epochId = ssu.epochId,
                time = ssu.time,
                mac = ssu.mac))
        }
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.toDomain())
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }

    override suspend fun report(token: String, localProximityList: List<LocalProximity>): RobertResult {
        return tryCatchRequest {
            api.report(ApiReportRQ.fromLocalProximityList(token, localProximityList))
        }
    }

    override suspend fun deleteExposureHistory(ssu: ServerStatusUpdate): RobertResult {
        return tryCatchRequest {
            api.deleteExposureHistory(ApiDeleteExposureHistoryRQ(ebid = ssu.ebid, epochId = ssu.epochId, time = ssu.time, mac = ssu.mac))
        }
    }

    private suspend fun tryCatchRequest(doRequest: suspend () -> Response<ApiCommonRS>): RobertResult {
        return try {
            val result = doRequest()
            if (result.isSuccessful) {
                if (result.body()?.success == true) {
                    RobertResult.Success()
                } else {
                    RobertResult.Failure(BackendException(
                        result.body()?.message!!))
                }
            } else {
                RobertResult.Failure(HttpException(result).remoteToRobertException())
            }
        } catch (e: Exception) {
            Timber.e(ServiceDataSource::class.java.simpleName, e.message ?: "")
            RobertResult.Failure(error = e.remoteToRobertException())
        }
    }

    private suspend fun <T> tryCatchRequestData(doRequest: suspend () -> Response<T>): RobertResultData<T> {
        return try {
            val result = doRequest()
            if (result.isSuccessful) {
                RobertResultData.Success(result.body()!!)
            } else {
                RobertResultData.Failure(HttpException(result).remoteToRobertException())
            }
        } catch (e: Exception) {
            Timber.e(e)
            RobertResultData.Failure(error = e.remoteToRobertException())
        }
    }
}
