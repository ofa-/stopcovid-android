/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.repository

import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.robert.datasource.LocalLocalProximityDataSource

internal class LocalProximityRepository(private val localLocalProximityDataSource: LocalLocalProximityDataSource) {

    suspend fun save(vararg localProximity: LocalProximity) {
        localLocalProximityDataSource.saveAll(*localProximity)
    }

    fun getBetweenTime(startNtpTimeS: Long, endNtpTimeS: Long, onProgressUpdate: ((Float) -> Unit)? = null): List<LocalProximity> {
        return localLocalProximityDataSource.getBetweenTime(startNtpTimeS, endNtpTimeS, onProgressUpdate)
    }

    fun removeUntilTime(ntpTimeS: Long) {
        localLocalProximityDataSource.removeUntilTime(ntpTimeS)
    }

    fun removeAll() {
        localLocalProximityDataSource.removeAll()
    }
}
