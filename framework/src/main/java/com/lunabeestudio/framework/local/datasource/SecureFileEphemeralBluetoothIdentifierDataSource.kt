/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/06/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.local.datasource

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.core.util.AtomicFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.robert.datasource.LocalEphemeralBluetoothIdentifierDataSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.File

class SecureFileEphemeralBluetoothIdentifierDataSource(
    context: Context,
    private val cryptoManager: LocalCryptoManager,
) : LocalEphemeralBluetoothIdentifierDataSource {

    private val epochFile = AtomicFile(File(context.filesDir, "epochs"))

    private var cache: List<EphemeralBluetoothIdentifier>? = null
        @Synchronized
        get() {
            if (field.isNullOrEmpty()) {
                field = getAllWithoutMutex()
            }
            return field
        }

    private val gsonEphemeralBluetoothIdentifierListType = object : TypeToken<List<EphemeralBluetoothIdentifier>>() {}.type
    private val gson: Gson = Gson()

    override suspend fun getAll(): List<EphemeralBluetoothIdentifier> {
        return mutex.withLock {
            getAllWithoutMutex()
        }
    }

    @WorkerThread
    private fun getAllWithoutMutex(): List<EphemeralBluetoothIdentifier> {
        return try {
            if (epochFile.baseFile.exists()) {
                val json = cryptoManager.decryptToString(epochFile)
                gson.fromJson(json, gsonEphemeralBluetoothIdentifierListType)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e)
            emptyList()
        }
    }

    override suspend fun getForTime(ntpTimeS: Long): EphemeralBluetoothIdentifier? {
        return mutex.withLock { cache?.firstOrNull { it.ntpStartTimeS <= ntpTimeS && ntpTimeS < it.ntpEndTimeS } }
    }

    @WorkerThread
    override suspend fun saveAll(vararg ephemeralBluetoothIdentifier: EphemeralBluetoothIdentifier) {
        mutex.withLock {
            saveAllWithoutMutex(ephemeralBluetoothIdentifier)
        }
    }

    @WorkerThread
    private fun saveAllWithoutMutex(ephemeralBluetoothIdentifier: Array<out EphemeralBluetoothIdentifier>) {
        if (ephemeralBluetoothIdentifier.isNotEmpty()) {
            val json = gson.toJson(ephemeralBluetoothIdentifier)
            cryptoManager.encryptToFile(json, epochFile)
            cache = null
        } else {
            val message = "Trying to save empty ebid array is forbidden"
            Timber.e(message)
        }
    }

    override suspend fun removeUntilTimeKeepLast(ntpTimeS: Long) {
        mutex.withLock {
            cache?.let { cache ->
                val last = cache.maxByOrNull { it.ntpEndTimeS }
                val updatedCache = cache
                    .filter {
                        it.ntpEndTimeS > ntpTimeS || it == last
                    }.toTypedArray()

                saveAllWithoutMutex(updatedCache)
            }
        }
    }

    override suspend fun removeAll() {
        mutex.withLock {
            epochFile.delete()
            cache = null
        }
    }

    companion object {
        private val mutex: Mutex = Mutex()
    }
}
