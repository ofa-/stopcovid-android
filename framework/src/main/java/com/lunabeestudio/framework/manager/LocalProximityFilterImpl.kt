/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/01/06 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.manager

import com.google.gson.Gson
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeMs
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.robert.RobertConstant
import com.lunabeestudio.robert.manager.LocalProximityFilter
import com.orange.proximitynotification.filter.ProximityFilter
import com.orange.proximitynotification.filter.TimestampedRssi
import timber.log.Timber
import java.util.Date
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

class LocalProximityFilterImpl : LocalProximityFilter {

    @OptIn(ExperimentalTime::class)
    override fun filter(localProximityList: List<LocalProximity>,
        mode: LocalProximityFilter.Mode,
        configJson: String): List<LocalProximity> {
        val config = Gson().fromJson(configJson, ProximityFilter.Config::class.java)
        val proximityFilter = ProximityFilter(config)

        val mutableLocalProximityList = localProximityList.toMutableList()
        val groupedLocalProximity = localProximityList.groupBy { it.ebidBase64 }

        groupedLocalProximity.forEach { (_, list) ->
            val epochStartTimeS = (list.first().collectedTime / RobertConstant.EPOCH_DURATION_S) * RobertConstant.EPOCH_DURATION_S
            val epochStartDate = Date(epochStartTimeS.ntpTimeSToUnixTimeMs())

            val timestampedRssiList = list.map {
                TimestampedRssi(
                    id = it.hashCode(),
                    timestamp = Date(it.collectedTime.ntpTimeSToUnixTimeMs()),
                    rssi = it.calibratedRssi
                )
            }

            val filterOutput = proximityFilter.filter(
                timestampedRssis = timestampedRssiList,
                epochStart = epochStartDate,
                epochDuration = RobertConstant.EPOCH_DURATION_S.toLong(),
                mode = mode.toBleMode()
            )
            if (filterOutput is ProximityFilter.Output.Rejected) {
                mutableLocalProximityList.removeAll(list)
            } else if (filterOutput is ProximityFilter.Output.Accepted && filterOutput.areTimestampedRssisUpdated) {
                val localProximityByHashValue: Map<Int, List<LocalProximity>> = list.groupBy { it.hashCode() }
                filterOutput.timestampedRssis.forEach { (id, _, rssi) ->
                    localProximityByHashValue[id]?.firstOrNull()?.calibratedRssi = rssi
                }
            }
        }

        return mutableLocalProximityList
    }
}

private fun LocalProximityFilter.Mode.toBleMode(): ProximityFilter.Mode {
    return when (this) {
        LocalProximityFilter.Mode.FULL -> ProximityFilter.Mode.FULL
        LocalProximityFilter.Mode.MEDIUM -> ProximityFilter.Mode.MEDIUM
        LocalProximityFilter.Mode.RISKS -> ProximityFilter.Mode.RISKS
    }
}