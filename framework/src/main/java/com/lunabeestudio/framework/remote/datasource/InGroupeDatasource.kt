/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/22/6 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote.datasource

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AnalyticsServiceName
import com.lunabeestudio.domain.model.WalletCertificateType
import com.lunabeestudio.framework.remote.RetrofitClient
import com.lunabeestudio.framework.remote.model.ApiConvertErrorRS
import com.lunabeestudio.framework.remote.model.ApiConvertRQ
import com.lunabeestudio.robert.datasource.RemoteCertificateDataSource
import com.lunabeestudio.robert.model.BackendException
import com.lunabeestudio.robert.model.RobertResultData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class InGroupeDatasource(
    private val context: Context,
    private val certificateSHA256: String,
) : RemoteCertificateDataSource {

    override suspend fun convertCertificate(
        url: String,
        encodedCertificate: String,
        from: WalletCertificateType.Format,
        to: WalletCertificateType.Format
    ): RobertResultData<String> {
        val okHttpClient = RetrofitClient.getDefaultOKHttpClient(
            context = context,
            url = url,
            certificateSHA256 = certificateSHA256,
            cacheConfig = null
        )

        val gson = Gson()

        val apiConvertRQ = ApiConvertRQ(chainEncoded = encodedCertificate, destination = to.toApiKey(), source = from.toApiKey())
        val mediaType = "application/json".toMediaTypeOrNull()
        val bodyRq = gson.toJson(apiConvertRQ).toRequestBody(mediaType)

        val request: Request = Request.Builder()
            .url(url)
            .post(bodyRq)
            .build()

        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            val response = okHttpClient.newCall(request).execute()
            val bodyRs = response.body?.string()

            if (response.isSuccessful && bodyRs != null) {
                RobertResultData.Success(bodyRs)
            } else {
                var analyticsErrorDesc: String? = null
                try {
                    val error = gson.fromJson(bodyRs, ApiConvertErrorRS::class.java)
                    analyticsErrorDesc = "${error.msgError} (${error.codeError})"
                    RobertResultData.Failure(BackendException("${error.msgError} (${error.codeError})"))
                } catch (e: Exception) {
                    RobertResultData.Failure(BackendException("Unable to parse body result: $bodyRs"))
                } finally {
                    AnalyticsManager.reportWSError(
                        context,
                        context.filesDir,
                        AnalyticsServiceName.CERTIFICATE_CONVERSION,
                        "0",
                        response.code,
                        analyticsErrorDesc,
                    )
                }
            }
        }
    }

    fun WalletCertificateType.Format.toApiKey(): String {
        return when (this) {
            WalletCertificateType.Format.WALLET_2D -> "DEUX_D_DOC"
            WalletCertificateType.Format.WALLET_DCC -> "DGCA"
        }
    }
}