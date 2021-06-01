/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/11/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.remote

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.IOException
import okio.buffer
import okio.sink

class UploadProgressRequestBody(private val requestBody: RequestBody, private val progressListener: ProgressListener) : RequestBody() {
    override fun contentType(): MediaType? {
        return requestBody.contentType()
    }

    override fun contentLength(): Long {
        try {
            return requestBody.contentLength()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return -1
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val progressOutputStream = ProgressOutputStream(sink.outputStream(), progressListener, contentLength())
        val progressSink: BufferedSink = progressOutputStream.sink().buffer()
        requestBody.writeTo(progressSink)
        progressSink.flush()
    }

    interface ProgressListener {
        fun update(bytesWritten: Long, contentLength: Long)
    }
}