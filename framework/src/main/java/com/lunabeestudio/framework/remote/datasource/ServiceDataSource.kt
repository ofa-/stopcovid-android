/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote.datasource

import android.content.Context
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.domain.model.RegisterReport
import com.lunabeestudio.domain.model.ReportResponse
import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.domain.model.StatusReport
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.domain.model.WStatusReport
import com.lunabeestudio.framework.remote.RetrofitClient
import com.lunabeestudio.framework.remote.extension.remoteToRobertException
import com.lunabeestudio.framework.remote.model.ApiCommonRS
import com.lunabeestudio.framework.remote.model.ApiDeleteExposureHistoryRQ
import com.lunabeestudio.framework.remote.model.ApiRegisterV2RQ
import com.lunabeestudio.framework.remote.model.ApiReportRQ
import com.lunabeestudio.framework.remote.model.ApiStatusRQ
import com.lunabeestudio.framework.remote.model.ApiUnregisterRQ
import com.lunabeestudio.framework.remote.model.ApiWReportRQ
import com.lunabeestudio.framework.remote.model.ApiWStatusRQ
import com.lunabeestudio.framework.remote.model.CaptchaRQ
import com.lunabeestudio.framework.remote.model.toDomain
import com.lunabeestudio.framework.remote.server.StopCovidApi
import com.lunabeestudio.framework.remote.server.StopCovidWarningApi
import com.lunabeestudio.robert.datasource.RemoteServiceDataSource
import com.lunabeestudio.robert.model.BackendException
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.File

class ServiceDataSource(
    context: Context,
    baseUrl: String,
    warningBaseUrl: String,
    certificateSha256: String,
    warningCertificateSha256: String,
) : RemoteServiceDataSource {

    private var api: StopCovidApi = RetrofitClient.getService(context, baseUrl, certificateSha256, StopCovidApi::class.java)
    private var warningApi: StopCovidWarningApi = RetrofitClient.getWarningService(
        context,
        warningBaseUrl,
        warningCertificateSha256,
        StopCovidWarningApi::class.java
    )
    private var fileApi: StopCovidApi = RetrofitClient.getFileService(context, baseUrl, certificateSha256, StopCovidApi::class.java)

    override suspend fun generateCaptcha(apiVersion: String, type: String, language: String): RobertResultData<String> {
        val result = tryCatchRequestData {
            api.captcha(apiVersion, CaptchaRQ(type = type, locale = language))
        }
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.id)
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }

    override suspend fun getCaptcha(apiVersion: String, captchaId: String, type: String, path: String): RobertResult {
        val result = tryCatchRequestData {
            fileApi.getCaptcha(apiVersion, captchaId, type)
        }
        return when (result) {
            is RobertResultData.Success -> {
                try {
                    result.data.byteStream().use { inputStream ->
                        File(path).outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream, 4 * 1024)
                        }
                    }
                    RobertResult.Success()
                } catch (e: Exception) {
                    RobertResult.Failure(e.remoteToRobertException())
                }
            }
            is RobertResultData.Failure -> RobertResult.Failure(result.error)
        }
    }

    override suspend fun registerV2(
        apiVersion: String,
        captcha: String,
        captchaId: String,
        clientPublicECDHKey: String,
    ): RobertResultData<RegisterReport> {
        val result = tryCatchRequestData {
            api.registerV2(apiVersion, ApiRegisterV2RQ(captcha = captcha, captchaId = captchaId, clientPublicECDHKey = clientPublicECDHKey))
        }
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.toDomain())
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }

    override suspend fun unregister(apiVersion: String, ssu: ServerStatusUpdate): RobertResult {
        return tryCatchRequest {
            api.unregister(apiVersion, ApiUnregisterRQ(ebid = ssu.ebid, epochId = ssu.epochId, time = ssu.time, mac = ssu.mac))
        }
    }

    override suspend fun status(apiVersion: String, ssu: ServerStatusUpdate): RobertResultData<StatusReport> {
        val result = tryCatchRequestData {
            api.status(
                apiVersion, ApiStatusRQ(
                ebid = ssu.ebid,
                epochId = ssu.epochId,
                time = ssu.time,
                mac = ssu.mac
            )
            )
        }
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.toDomain())
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }

    override suspend fun wstatus(
        warningApiVersion: String,
        venueQrCodeList: List<VenueQrCode>,
    ): RobertResultData<WStatusReport> {
        val result = tryCatchRequestData {
            warningApi.wstatus(
                warningApiVersion,
                ApiWStatusRQ.fromVenueQrCodeList(venueQrCodeList)
            )
        }
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.toDomain())
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }

    override suspend fun report(
        apiVersion: String,
        token: String,
        localProximityList: List<LocalProximity>,
    ): RobertResultData<ReportResponse> {
        val result = tryCatchRequestData {
            api.report(apiVersion, ApiReportRQ.fromLocalProximityList(token, localProximityList))
        }
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.toDomain())
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }

    override suspend fun wreport(
        warningApiVersion: String,
        token: String,
        venueQrCodeList: List<VenueQrCode>,
    ): RobertResult {
        return tryCatchRequest {
            warningApi.wreport(warningApiVersion, "Bearer $token", ApiWReportRQ.fromVenueQrCodeList(venueQrCodeList))
        }
    }

    override suspend fun deleteExposureHistory(apiVersion: String, ssu: ServerStatusUpdate): RobertResult {
        return tryCatchRequest {
            api.deleteExposureHistory(
                apiVersion,
                ApiDeleteExposureHistoryRQ(ebid = ssu.ebid, epochId = ssu.epochId, time = ssu.time, mac = ssu.mac)
            )
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun tryCatchRequest(doRequest: suspend () -> Response<ApiCommonRS>): RobertResult {
        return try {
            val result = doRequest()
            if (result.isSuccessful) {
                if (result.body()?.success == true) {
                    RobertResult.Success()
                } else {
                    RobertResult.Failure(
                        BackendException(
                            result.body()?.message!!
                        )
                    )
                }
            } else {
                RobertResult.Failure(HttpException(result).remoteToRobertException())
            }
        } catch (e: Exception) {
            Timber.e(ServiceDataSource::class.java.simpleName, e.message ?: "")
            RobertResult.Failure(error = e.remoteToRobertException())
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
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
