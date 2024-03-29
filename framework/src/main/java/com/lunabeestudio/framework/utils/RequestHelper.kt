package com.lunabeestudio.framework.utils

import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.framework.remote.datasource.ServiceDataSource
import com.lunabeestudio.framework.remote.extension.remoteToRobertException
import com.lunabeestudio.framework.remote.model.ApiCommonRS
import com.lunabeestudio.robert.model.BackendException
import com.lunabeestudio.robert.model.NoInternetException
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber

internal object RequestHelper {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun tryCatchRequest(
        apiVersion: String,
        analyticsServiceName: String?,
        analyticsManager: AnalyticsManager,
        doRequest: suspend () -> Response<ApiCommonRS>,
    ): RobertResult {
        return try {
            val result = doRequest()
            val httpCode = result.code()
            if (result.isSuccessful) {
                if (result.body()?.success == true) {
                    RobertResult.Success()
                } else {
                    analyticsServiceName?.let { analyticsManager.reportWSError(it, apiVersion, httpCode) }
                    RobertResult.Failure(BackendException(result.body()?.message!!, httpCode))
                }
            } else {
                analyticsServiceName?.let { analyticsManager.reportWSError(it, apiVersion, httpCode) }
                RobertResult.Failure(HttpException(result).remoteToRobertException())
            }
        } catch (e: Exception) {
            Timber.e(ServiceDataSource::class.java.simpleName, e.message ?: "")
            val robertException = e.remoteToRobertException()
            if (robertException !is NoInternetException) {
                analyticsServiceName?.let {
                    analyticsManager.reportWSError(
                        it,
                        apiVersion,
                        (e as? HttpException)?.code() ?: 0,
                        e.message
                    )
                }
            }
            RobertResult.Failure(error = robertException)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun <T> tryCatchRequestData(
        apiVersion: String,
        analyticsServiceName: String?,
        analyticsManager: AnalyticsManager,
        doRequest: suspend () -> Response<T>,
    ): RobertResultData<T> {
        return try {
            val result = doRequest()
            if (result.isSuccessful) {
                RobertResultData.Success(result.body()!!)
            } else {
                analyticsServiceName?.let { analyticsManager.reportWSError(it, apiVersion, result.code()) }
                RobertResultData.Failure(HttpException(result).remoteToRobertException())
            }
        } catch (e: Exception) {
            Timber.e(e)
            val robertException = e.remoteToRobertException()
            if (robertException !is NoInternetException) {
                analyticsServiceName?.let {
                    analyticsManager.reportWSError(
                        it,
                        apiVersion,
                        (e as? HttpException)?.code() ?: 0,
                        e.message
                    )
                }
            }
            RobertResultData.Failure(error = robertException)
        }
    }
}