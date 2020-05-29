/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.repository

import androidx.annotation.WorkerThread
import com.lunabeestudio.robert.RobertConstant
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import com.lunabeestudio.robert.datasource.LocalEphemeralBluetoothIdentifierDataSource
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
import com.lunabeestudio.robert.model.NoKeyException
import com.lunabeestudio.robert.model.ServerDecryptException
import javax.crypto.AEADBadTagException

@WorkerThread
internal class EphemeralBluetoothIdentifierRepository(
    private val localEphemeralBluetoothIdentifierDataSource: LocalEphemeralBluetoothIdentifierDataSource,
    private val sharedCryptoDataSource: SharedCryptoDataSource,
    private val localKeystoreDataSource: LocalKeystoreDataSource
) {
    @OptIn(ExperimentalStdlibApi::class)
    @Throws(NullPointerException::class, NoKeyException::class)
    fun save(tuples: ByteArray) {
        val rawEbid = localKeystoreDataSource.kEA?.let {
            try {
                sharedCryptoDataSource.decrypt(it, tuples).decodeToString()
            } catch (e: AEADBadTagException) {
                throw ServerDecryptException()
            }
        } ?: throw NoKeyException("Failed to retrieve kEA")

        val ebids = EphemeralBluetoothIdentifier.createFromTuples(localKeystoreDataSource.timeStart!!,
            RobertConstant.EPOCH_DURATION_S,
            rawEbid)

        localEphemeralBluetoothIdentifierDataSource.saveAll(*ebids.toTypedArray())
    }

    fun getAll(): List<EphemeralBluetoothIdentifier> {
        return localEphemeralBluetoothIdentifierDataSource.getAll()
    }

    fun getForTime(ntpTimeS: Long = System.currentTimeMillis().unixTimeMsToNtpTimeS()): EphemeralBluetoothIdentifier? {
        return localEphemeralBluetoothIdentifierDataSource.getForTime(ntpTimeS)
    }

    fun removeUntilTimeKeepLast(ntpTimeS: Long) {
        localEphemeralBluetoothIdentifierDataSource.removeUntilTimeKeepLast(ntpTimeS)
    }

    fun removeAll() {
        localEphemeralBluetoothIdentifierDataSource.removeAll()
    }
}
