/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.datasource

import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier

interface LocalEphemeralBluetoothIdentifierDataSource {
    suspend fun getAll(): List<EphemeralBluetoothIdentifier>
    suspend fun getForTime(ntpTimeS: Long): EphemeralBluetoothIdentifier?
    suspend fun saveAll(vararg ephemeralBluetoothIdentifier: EphemeralBluetoothIdentifier)
    suspend fun removeUntilTimeKeepLast(ntpTimeS: Long)
    suspend fun removeAll()
}