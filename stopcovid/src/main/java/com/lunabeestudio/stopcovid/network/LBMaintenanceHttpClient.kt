/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.network

import android.content.Context
import com.lunabeestudio.framework.remote.RetrofitClient
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.coreui.ConfigConstant
import com.lunabeestudio.stopcovid.manager.TimeCheckManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/**
 * Light http client allowing us to do some network works without external libs
 */
object LBMaintenanceHttpClient {

    /**
     * Do a GET http call to read a text file like json for example
     * @param urlString : the url to use to do the call
     * @param onSuccess : success completion, return the file as a string
     * @param onFailure : failure completion, return the exception
     */
    suspend fun get(context: Context,
        urlString: String,
        onSuccess: suspend (result: String) -> Unit,
        onFailure: suspend (e: Exception) -> Unit) {
        try {
            val okHttpClient = RetrofitClient.getDefaultOKHttpClient(context, urlString, ConfigConstant.SERVER_CERTIFICATE_SHA256)
            val request: Request = Request.Builder()
                .url(urlString)
                .build()

            @Suppress("BlockingMethodInNonBlockingContext")
            val string = withContext(Dispatchers.IO) {
                val response = okHttpClient.newCall(request).execute()
                val isClockAligned = TimeCheckManager.isTimeAlignedWithServer(response)
                if (isClockAligned == false) {
                    (context.applicationContext as? StopCovid)?.sendClockNotAlignedNotification()
                } else if (isClockAligned == true) {
                    (context.applicationContext as? StopCovid)?.cancelClockNotAlignedNotification()
                }
                response.body!!.string()
            }

            onSuccess(string)
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
