/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/09/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.local.datasource

import androidx.core.util.AtomicFile
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.framework.extension.toDomain
import com.lunabeestudio.framework.extension.toProto
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.framework.proto.ProtoStorage
import com.lunabeestudio.robert.datasource.LocalLocalProximityDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

open class SecureFileLocalProximityDataSource(
    private val storageDir: File,
    private val cryptoManager: LocalCryptoManager,
) : LocalLocalProximityDataSource {

    protected val daySinceNtp: Long
        get() = System.currentTimeMillis().unixTimeMsToNtpTimeS() / (60 * 60 * 24)

    private var _encryptedFile: File? = null
    protected val encryptedFile: File
        get() {
            if (_encryptedFile == null) {
                _encryptedFile = getDailySessionFile()
            }
            return _encryptedFile!!
        }

    protected val cacheMtx: Mutex = Mutex()
    protected val localProximityList: MutableList<LocalProximity> = mutableListOf()

    private var dumpRequested: AtomicBoolean = AtomicBoolean(false)
    private var dumpDelayRunning: AtomicBoolean = AtomicBoolean(false)

    private var dumpJob: Job? = null

    override fun getBetweenTime(
        ntpStartTimeS: Long,
        ntpEndTimeS: Long,
        onProgressUpdate: ((Float) -> Unit)?
    ): List<LocalProximity> {
        val filesList = storageDir.listFiles { file ->
            file.isDirectory && file.name.toIntOrNull() != null
        }?.mapNotNull { file ->
            val dirDay = file.name.toInt()
            if (dirDay >= ntpStartTimeS / (60 * 60 * 24) && dirDay <= ntpEndTimeS / (60 * 60 * 24)) {
                file
            } else {
                null
            }
        }?.flatMap {
            it.listFiles()?.asList() ?: emptyList()
        }
        onProgressUpdate?.invoke(0f)
        return filesList?.flatMapIndexed { index, file ->
            val result = try {
                cryptoManager.createCipherInputStream(file.inputStream()).use { cis ->
                    ProtoStorage.LocalProximityProtoList.parseFrom(cis).toDomain()
                }
            } catch (e: Exception) {
                Timber.e(e)
                emptyList()
            }
            onProgressUpdate?.invoke((index + 1).toFloat() / filesList.size.toFloat())
            result
        } ?: emptyList()
    }

    override suspend fun saveAll(vararg localProximity: LocalProximity) {
        cacheMtx.withLock {
            localProximityList += localProximity
        }
        if (dumpDelayRunning.get()) {
            dumpRequested.set(true)
            Timber.v("Dump delay running.")
        } else {
            dumpJob?.cancel()
            dumpJob = CoroutineScope(Dispatchers.IO).launch {
                dumpCache()
            }
        }
    }

    override fun removeUntilTime(ntpTimeS: Long) {
        storageDir.listFiles { file ->
            file.isDirectory && file.name.toIntOrNull() != null
        }?.forEach { file ->
            val dirDay = file.name.toInt()
            if (dirDay < ntpTimeS / (60 * 60 * 24)) {
                file.deleteRecursively()
            }
        }
    }

    override fun removeAll() {
        dumpJob?.cancel(CancellationException("Remove all local data called"))
        dumpDelayRunning.set(false)
        localProximityList.clear()
        storageDir.listFiles()?.forEach { file ->
            file.deleteRecursively()
        }
        _encryptedFile = null
    }

    // Get file per day and app life session
    protected fun getDailySessionFile(): File {
        val dailySessionDir = File(storageDir, "$daySinceNtp")

        dailySessionDir.mkdirs()

        val sessionNum = dailySessionDir.listFiles { _, name ->
            name.contains(Regex("^$daySinceNtp-\\d+$"))
        }?.map {
            it.name.substringAfterLast("-").toInt()
        }?.maxOrNull()?.plus(1) ?: 0

        return File(dailySessionDir, "$daySinceNtp-$sessionNum")
    }

    private suspend fun dumpCache() {
        dumpRequested.set(true)

        @Suppress("BlockingMethodInNonBlockingContext")
        val doDump = suspend {
            var lastDumpedIndex: Int
            var dumpTime = System.currentTimeMillis()

            val atomicFile = AtomicFile(encryptedFile)
            val fileOutputStream = atomicFile.startWrite()
            cryptoManager.createCipherOutputStream(fileOutputStream).use { cos ->
                val proto = cacheMtx.withLock {
                    Timber.v("Start dumping ${localProximityList.size} items to ${encryptedFile.absolutePath}")
                    lastDumpedIndex = (localProximityList.size - 1).coerceAtLeast(0)
                    localProximityList.toProto()
                }
                proto.writeTo(cos)
            }
            atomicFile.finishWrite(fileOutputStream)

            dumpTime = System.currentTimeMillis() - dumpTime
            Timber.v("Dumping cache to ${encryptedFile.absolutePath} done in ${dumpTime}ms")

            Pair(lastDumpedIndex, dumpTime)
        }

        dumpDelayRunning.set(true)
        while (dumpRequested.getAndSet(false)) {
            val dumpResult = doDump()
            updateEncryptedFolderIfNeeded(dumpResult.first)
            val delayMillis = max(dumpResult.second * DUMP_DELAY_FACTOR, DUMP_MIN_DELAY_MS)
            Timber.v("Delaying dumps for ${delayMillis}ms")
            delay(delayMillis)
        }
        dumpDelayRunning.set(false)
    }

    protected open suspend fun updateEncryptedFolderIfNeeded(lastDumpedIndex: Int) {
        cacheMtx.withLock {
            val fileDaySinceNtp = encryptedFile.parentFile!!.name.toLong()
            if (daySinceNtp != fileDaySinceNtp || localProximityList.size > DUMP_FILE_MAX_ENTRIES) {
                // Change folder
                _encryptedFile = getDailySessionFile()
                // Flush cache
                Timber.v("Change dump session file & flush cache")
                localProximityList.subList(0, lastDumpedIndex + 1).clear()
            }
        }
    }

    companion object {
        private const val DUMP_FILE_MAX_ENTRIES: Long = 5000
        private const val DUMP_DELAY_FACTOR: Int = 3
        private const val DUMP_MIN_DELAY_MS: Long = 15 * 1000
    }
}