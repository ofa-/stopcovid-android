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
import com.lunabeestudio.framework.remote.RetrofitClient
import com.lunabeestudio.framework.remote.extension.remoteToRobertException
import com.lunabeestudio.framework.remote.model.ApiDeleteExposureHistoryRQ
import com.lunabeestudio.framework.remote.model.ApiRegisterV2RQ
import com.lunabeestudio.framework.remote.model.ApiReportRQ
import com.lunabeestudio.framework.remote.model.ApiStatusRQ
import com.lunabeestudio.framework.remote.model.ApiUnregisterRQ
import com.lunabeestudio.framework.remote.model.CaptchaRQ
import com.lunabeestudio.framework.remote.model.toDomain
import com.lunabeestudio.framework.remote.server.StopCovidApi
import com.lunabeestudio.framework.utils.RequestHelper
import com.lunabeestudio.robert.datasource.RemoteServiceDataSource
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import java.io.File

class ServiceDataSource(
    private val context: Context,
    baseUrl: String,
    certificateSha256: String
) : RemoteServiceDataSource {

    private var filesDir = context.filesDir
    private var api: StopCovidApi = RetrofitClient.getService(context, baseUrl, certificateSha256, StopCovidApi::class.java, null)
    private var reportProgressUpdate: ((Float) -> Unit)? = null
    private var reportApi: StopCovidApi = RetrofitClient.getService(
        context,
        baseUrl,
        certificateSha256,
        StopCovidApi::class.java,
        null,
    ) {
        reportProgressUpdate?.invoke(it)
    }
    private var fileApi: StopCovidApi = RetrofitClient.getFileService(context, baseUrl, certificateSha256, StopCovidApi::class.java)

    override suspend fun generateCaptcha(apiVersion: String, type: String, language: String): RobertResultData<String> {

        val result = RequestHelper.tryCatchRequestData(context, filesDir, apiVersion, "captcha-$type") {
            api.captcha(apiVersion, CaptchaRQ(type = type, locale = language))
        }
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.id)
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }

    override suspend fun getCaptcha(apiVersion: String, captchaId: String, type: String, path: String): RobertResult {

        val result = RequestHelper.tryCatchRequestData(context, filesDir, apiVersion, "captcha") {
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

        val result = RequestHelper.tryCatchRequestData(context, filesDir, apiVersion, "register") {
            api.registerV2(apiVersion, ApiRegisterV2RQ(captcha = captcha, captchaId = captchaId, clientPublicECDHKey = clientPublicECDHKey))
        }
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.toDomain())
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }

    override suspend fun unregister(apiVersion: String, ssu: ServerStatusUpdate): RobertResult {
        return RequestHelper.tryCatchRequest(context, filesDir, apiVersion, "unregister") {
            api.unregister(apiVersion, ApiUnregisterRQ(ebid = ssu.ebid, epochId = ssu.epochId, time = ssu.time, mac = ssu.mac))
        }
    }

    override suspend fun status(apiVersion: String, ssu: ServerStatusUpdate): RobertResultData<StatusReport> {
        val result = RequestHelper.tryCatchRequestData(context, filesDir, apiVersion, "status") {
            api.status(
                apiVersion,
                ApiStatusRQ(
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

    override suspend fun report(
        apiVersion: String,
        token: String,
        localProximityList: List<LocalProximity>,
        onProgressUpdate: ((Float) -> Unit)?,
    ): RobertResultData<ReportResponse> {

        val result = RequestHelper.tryCatchRequestData(context, filesDir, apiVersion, "report") {
            reportProgressUpdate = onProgressUpdate
            reportApi.report(apiVersion, ApiReportRQ.fromLocalProximityList(token, localProximityList))
        }
        reportProgressUpdate = null
        return when (result) {
            is RobertResultData.Success -> RobertResultData.Success(result.data.toDomain())
            is RobertResultData.Failure -> RobertResultData.Failure(result.error)
        }
    }

    override suspend fun deleteExposureHistory(apiVersion: String, ssu: ServerStatusUpdate): RobertResult {
        return RequestHelper.tryCatchRequest(context, filesDir, apiVersion, "deleteExposureHistory") {
            api.deleteExposureHistory(
                apiVersion,
                ApiDeleteExposureHistoryRQ(ebid = ssu.ebid, epochId = ssu.epochId, time = ssu.time, mac = ssu.mac)
            )
        }
    }
}
