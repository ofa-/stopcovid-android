/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/15/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import android.content.Context
import com.google.protobuf.GeneratedMessageLite
import com.lunabeestudio.framework.remote.server.ServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream

abstract class RemoteProtoGzipManager<Proto : GeneratedMessageLite<*, *>, App>(serverManager: ServerManager) : RemoteFileManager(
    serverManager
) {
    final override val mimeType: String = "application/x-gzip"

    protected suspend fun loadLocal(context: Context): App? {
        return withContext(Dispatchers.IO) {
            getLocalFileOrAssetStream(context)?.use {
                getProtoMessage(it).mapProtoToApp()
            }
        }
    }

    override suspend fun fileNotCorrupted(file: File): Boolean {
        return try {
            getProtoMessage(file)
            true
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
    }

    @Throws(IOException::class)
    protected suspend fun getLocalProtoMessage(context: Context): Proto = getProtoMessage(File(getLocalFileName(context)))

    private suspend fun getProtoMessage(file: File): Proto {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            file.inputStream().use { fileInputStream ->
                getProtoMessage(fileInputStream)
            }
        }
    }

    protected suspend fun getMappedProtoMessage(file: File): App {
        return getProtoMessage(file).mapProtoToApp()
    }

    private suspend fun getProtoMessage(inputStream: InputStream): Proto {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            GZIPInputStream(inputStream).use { gzipInputStream ->
                parseProtoGzipStream(gzipInputStream)
            }
        }
    }

    protected abstract fun parseProtoGzipStream(gzipInputStream: GZIPInputStream): Proto
    protected abstract fun Proto.mapProtoToApp(): App
}