/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.network

import android.content.Context
import com.lunabeestudio.framework.remote.RetrofitClient
import com.lunabeestudio.stopcovid.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Light http client allowing us to do some network works without external libs
 */
object LBMaintenanceHttpClient {

    @Suppress("DeferredResultUnused", "BlockingMethodInNonBlockingContext")
        /**
         * Do a GET http call to read a text file like json for example
         * @param urlString : the url to use to do the call
         * @param onSuccess : success completion, return the file as a string
         * @param onFailure : failure completion, return the exception
         */
    fun get(context: Context,
        urlString: String,
        onSuccess: (result: String) -> Unit,
        onFailure: (e: Exception) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val okHttpClient = RetrofitClient.getDefaultOKHttpClient(context, urlString, BuildConfig.APP_MAINTENANCE_CERTIFICATE_SHA256)
                val request: Request = Request.Builder()
                    .url(urlString)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val string = response.body!!.string()
                withContext(Dispatchers.Main) {
                    onSuccess(string)
                }
            } catch (e: Exception) {
                // FIXME workaround coroutine 1.3.7 https://github.com/Kotlin/kotlinx.coroutines/issues/2049#issuecomment-633270075
                dispatchFailure(onFailure, e)
            }
        }
    }

    private suspend fun dispatchFailure(onFailure: (e: Exception) -> Unit, e: Exception) {
        withContext(Dispatchers.Main) {
            onFailure(e)
        }
    }

}