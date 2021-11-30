/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/2/11 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import blacklist.Blacklist
import com.lunabeestudio.framework.local.dao.CertificateBlacklistRoomDao
import com.lunabeestudio.framework.remote.server.ServerManager
import com.lunabeestudio.robert.utils.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.zip.GZIPInputStream

abstract class RemoteProtoGzipRoomBlacklistManager<Room>(
    private val serverManager: ServerManager,
    private val dao: CertificateBlacklistRoomDao<Room>,
) {
    private var isRunning: Boolean = false
        @Synchronized get
        @Synchronized set

    protected val mimeType: String = "application/x-gzip"

    protected abstract val remoteTemplateUrl: String
    protected abstract val tmpFile: File
    protected abstract var blacklistIteration: Int

    private val _blacklistUpdateEvent: MutableLiveData<Event<Unit>> = MutableLiveData()
    val blacklistUpdateEvent: LiveData<Event<Unit>>
        get() = _blacklistUpdateEvent

    private suspend fun fetchIteration(iteration: Int): Exception? {
        val remoteFileUrl: String = String.format(remoteTemplateUrl, iteration)

        return try {
            val saveSucceeded = serverManager.saveTo(remoteFileUrl, tmpFile, mimeType)

            if (saveSucceeded) {
                val europeanCertificateBlacklistRoomList = getProtoMessage().itemsList.groupBy {
                    it.firstOrNull() == '-'
                }.mapValues {
                    mapToRoom(it.value)
                }

                europeanCertificateBlacklistRoomList[false]?.let { dao.insertAll(*it) }
                europeanCertificateBlacklistRoomList[true]?.let { dao.deleteAll(*it) }
            }

            null
        } catch (e: Exception) {
            e
        } finally {
            tmpFile.delete()
        }
    }

    protected abstract fun mapToRoom(hashList: List<String>): Array<Room>

    protected suspend fun getProtoMessage(): Blacklist.BlackListMessage {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            tmpFile.inputStream().use { fileInputStream ->
                getProtoMessage(fileInputStream)
            }
        }
    }

    private suspend fun getProtoMessage(inputStream: InputStream): Blacklist.BlackListMessage {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            GZIPInputStream(inputStream).use { gzipInputStream ->
                Blacklist.BlackListMessage.parseFrom(gzipInputStream)
            }
        }
    }

    suspend fun fetchNewIterations(): Exception? {
        return if (!isRunning) {
            isRunning = true
            try {
                var currentIteration = blacklistIteration + 1
                var fetchError = fetchIteration(currentIteration)
                while (fetchError == null) {
                    blacklistIteration = currentIteration
                    currentIteration++
                    fetchError = fetchIteration(currentIteration)

                    _blacklistUpdateEvent.postValue(Event(Unit))
                }
                if ((fetchError as? HttpException)?.code() == 404) {
                    // 404 means last iteration has been reached
                    null
                } else {
                    Timber.e(fetchError)
                    fetchError
                }
            } finally {
                isRunning = false
            }
        } else {
            null
        }
    }

    suspend fun isBlacklisted(sha256: String): Boolean = withContext(Dispatchers.IO) {
        dao.getByHash(sha256) != null
    }
}