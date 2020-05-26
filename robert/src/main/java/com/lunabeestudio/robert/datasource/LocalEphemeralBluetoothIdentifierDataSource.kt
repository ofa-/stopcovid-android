/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.datasource

import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier

interface LocalEphemeralBluetoothIdentifierDataSource {
    fun getAll(): List<EphemeralBluetoothIdentifier>
    fun getForTime(ntpTimeS: Long): EphemeralBluetoothIdentifier?
    fun saveAll(vararg ephemeralBluetoothIdentifier: EphemeralBluetoothIdentifier)
    fun removeUntilTimeKeepLast(ntpTimeS: Long)
    fun removeAll()
}