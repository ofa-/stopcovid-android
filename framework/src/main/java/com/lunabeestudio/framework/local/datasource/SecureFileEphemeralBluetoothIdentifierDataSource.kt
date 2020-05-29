/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/06/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.local.datasource

import android.content.Context
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import com.lunabeestudio.framework.local.LocalCryptoManager
import com.lunabeestudio.robert.datasource.LocalEphemeralBluetoothIdentifierDataSource
import java.io.File

class SecureFileEphemeralBluetoothIdentifierDataSource(
    context: Context,
    private val cryptoManager: LocalCryptoManager
) : LocalEphemeralBluetoothIdentifierDataSource {

    private val epochFile = File(context.filesDir, "epochs")

    private var cache: List<EphemeralBluetoothIdentifier>? = null
        @Synchronized
        get() {
            if (field == null) {
                field = getAll()
            }
            return field
        }

    private val gsonEphemeralBluetoothIdentifierListType = object : TypeToken<List<EphemeralBluetoothIdentifier>>() {}.type
    private val gson: Gson = Gson()

    @WorkerThread
    override fun getAll(): List<EphemeralBluetoothIdentifier> {
        return if (epochFile.exists()) {
            val json = cryptoManager.decryptToString(epochFile)
            gson.fromJson(json, gsonEphemeralBluetoothIdentifierListType)
        } else {
            emptyList()
        }
    }

    override fun getForTime(ntpTimeS: Long): EphemeralBluetoothIdentifier? {
        return cache?.firstOrNull { it.ntpStartTimeS <= ntpTimeS && ntpTimeS < it.ntpEndTimeS }
    }

    @WorkerThread
    override fun saveAll(vararg ephemeralBluetoothIdentifier: EphemeralBluetoothIdentifier) {
        val json = gson.toJson(ephemeralBluetoothIdentifier)
        cryptoManager.encryptToFile(json, epochFile)
        cache = null
    }

    override fun removeUntilTimeKeepLast(ntpTimeS: Long) {
        cache?.let { cache ->
            val last = cache.maxBy { it.ntpEndTimeS }
            val updatedCache = cache
                .filter {
                    it.ntpEndTimeS > ntpTimeS || it == last
                }.toTypedArray()

            saveAll(*updatedCache)
        }
    }

    override fun removeAll() {
        epochFile.delete()
        cache = null
    }
}