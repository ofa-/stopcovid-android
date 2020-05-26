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

import com.lunabeestudio.domain.model.LocalProximity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow

interface LocalLocalProximityDataSource {
    fun getAll(): List<LocalProximity>
    suspend fun saveAll(vararg localProximity: LocalProximity)
    fun removeUntilTime(ntpTimeS: Long)
    fun removeAll()
}