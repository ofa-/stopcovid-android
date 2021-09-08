/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/01/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.analytics.network

import com.lunabeestudio.analytics.model.AnalyticsResult
import com.lunabeestudio.analytics.network.model.SendAnalyticsRQ
import com.lunabeestudio.analytics.network.model.SendAppAnalyticsRQ
import com.lunabeestudio.analytics.network.model.SendHealthAnalyticsRQ
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

internal class AnalyticsServerManager(private val okHttpClient: OkHttpClient) {

    var cachedApi: Pair<String, AnalyticsApi>? = null

    private fun getRetrofit(baseUrl: String): AnalyticsApi {
        val cachedApi = cachedApi
        return if (cachedApi?.first == baseUrl) {
            cachedApi.second
        } else {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(MoshiConverterFactory.create())
                .client(okHttpClient)
                .build()
                .create(AnalyticsApi::class.java).also { api ->
                    this.cachedApi = baseUrl to api
                }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun sendAnalytics(
        baseUrl: String,
        apiVersion: String,
        token: String,
        sendAnalyticsRQ: SendAnalyticsRQ
    ): AnalyticsResult {
        return try {
            val result = when (sendAnalyticsRQ) {
                is SendAppAnalyticsRQ -> getRetrofit(baseUrl).sendAppAnalytics(
                    apiVersion,
                    sendAnalyticsRQ,
                    "Bearer $token",
                )
                is SendHealthAnalyticsRQ -> getRetrofit(baseUrl).sendHealthAnalytics(
                    apiVersion,
                    sendAnalyticsRQ,
                    "Bearer $token",
                )
            }
            if (result.isSuccessful) {
                AnalyticsResult.Success()
            } else {
                AnalyticsResult.Failure(HttpException(result))
            }
        } catch (e: Exception) {
            AnalyticsResult.Failure(e)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun deleteAnalytics(
        baseUrl: String,
        apiVersion: String,
        token: String,
        installationUuid: String,
    ): AnalyticsResult {
        return try {
            val result = getRetrofit(baseUrl).deleteAnalytics(
                apiVersion,
                installationUuid,
                "Bearer $token",
            )
            if (result.isSuccessful) {
                AnalyticsResult.Success()
            } else {
                AnalyticsResult.Failure(HttpException(result))
            }
        } catch (e: Exception) {
            AnalyticsResult.Failure(e)
        }
    }
}
