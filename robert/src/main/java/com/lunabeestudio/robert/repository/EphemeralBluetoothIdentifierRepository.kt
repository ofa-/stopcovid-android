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
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import com.lunabeestudio.robert.datasource.LocalEphemeralBluetoothIdentifierDataSource

@WorkerThread
internal class EphemeralBluetoothIdentifierRepository(
    private val localEphemeralBluetoothIdentifierDataSource: LocalEphemeralBluetoothIdentifierDataSource
) {
    fun save(vararg ephemeralBluetoothIdentifiers: EphemeralBluetoothIdentifier) {
        localEphemeralBluetoothIdentifierDataSource.saveAll(*ephemeralBluetoothIdentifiers)
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
