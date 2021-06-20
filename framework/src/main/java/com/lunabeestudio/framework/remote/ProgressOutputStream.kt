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

import okio.IOException
import java.io.OutputStream

internal class ProgressOutputStream(
    private val stream: OutputStream,
    private val listener: UploadProgressRequestBody.ProgressListener,
    private val total: Long
) : OutputStream() {

    private var totalWritten: Long = 0

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        stream.write(b, off, len)
        if (total < 0) {
            listener.update(-1, -1)
            return
        }
        if (len < b.size) {
            totalWritten += len.toLong()
        } else {
            totalWritten += b.size.toLong()
        }
        listener.update(totalWritten, total)
    }

    @Throws(IOException::class)
    override fun write(b: Int) {
        stream.write(b)
        if (total < 0) {
            listener.update(-1, -1)
            return
        }
        totalWritten++
        listener.update(totalWritten, total)
    }

    @Throws(IOException::class)
    override fun close() {
        stream.close()
    }

    @Throws(IOException::class)
    override fun flush() {
        stream.flush()
    }
}