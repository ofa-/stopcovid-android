package com.lunabeestudio.framework.utils

import android.content.Context
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.framework.remote.datasource.ServiceDataSource
import com.lunabeestudio.framework.remote.extension.remoteToRobertException
import com.lunabeestudio.framework.remote.model.ApiCommonRS
import com.lunabeestudio.robert.model.BackendException
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.io.File

internal object RequestHelper {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun tryCatchRequest(
        context: Context, filesDir: File, apiVersion: String,
        analyticsServiceName: String?, doRequest: suspend () -> Response<ApiCommonRS>
    ): RobertResult {
        return try {
            val result = doRequest()
            if (result.isSuccessful) {
                if (result.body()?.success == true) {
                    RobertResult.Success()
                } else {
                    analyticsServiceName?.let { AnalyticsManager.reportWSError(context, filesDir, it, apiVersion, result.code()) }
                    RobertResult.Failure(
                        BackendException(
                            result.body()?.message!!
                        )
                    )
                }
            } else {
                analyticsServiceName?.let { AnalyticsManager.reportWSError(context, filesDir, it, apiVersion, result.code()) }
                RobertResult.Failure(HttpException(result).remoteToRobertException())
            }
        } catch (e: Exception) {
            Timber.e(ServiceDataSource::class.java.simpleName, e.message ?: "")
            analyticsServiceName?.let {
                AnalyticsManager.reportWSError(
                    context, filesDir,
                    it,
                    apiVersion,
                    (e as? HttpException)?.code() ?: 0,
                    e.message
                )
            }
            RobertResult.Failure(error = e.remoteToRobertException())
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun <T> tryCatchRequestData(
        context: Context, filesDir: File, apiVersion: String,
        analyticsServiceName: String?, doRequest: suspend () -> Response<T>
    ): RobertResultData<T> {
        return try {
            val result = doRequest()
            if (result.isSuccessful) {
                RobertResultData.Success(result.body()!!)
            } else {
                analyticsServiceName?.let { AnalyticsManager.reportWSError(context, filesDir, it, apiVersion, result.code()) }
                RobertResultData.Failure(HttpException(result).remoteToRobertException())
            }
        } catch (e: Exception) {
            Timber.e(e)
            analyticsServiceName?.let {
                AnalyticsManager.reportWSError(
                    context, filesDir,
                    it,
                    apiVersion,
                    (e as? HttpException)?.code() ?: 0,
                    e.message
                )
            }
            RobertResultData.Failure(error = e.remoteToRobertException())
        }
    }
}