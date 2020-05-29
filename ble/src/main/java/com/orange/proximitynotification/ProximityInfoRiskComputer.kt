/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification

import com.orange.proximitynotification.ble.BleProximityMetadata
import java.util.Date
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.min

/**
 * ProximityInfo risk computer
 *
 * @see ProximityInfo
 */
class ProximityInfoRiskComputer {

    /**
     * Compute ProximityInfoRisk for a list of ProximityInfo
     *
     * @param proximityInfos ProximityInfo list
     * @param from The start date to compute the risk from
     * @param durationInSeconds The period over which the risk will be computed
     * @return ProximityInfoRisk computed
     * @see ProximityInfoRisk
     */
    fun computeRisk(proximityInfos: List<ProximityInfo>, from: Date, durationInSeconds: Long): ProximityInfoRisk {
        if (durationInSeconds <= 0) {
            return ProximityInfoRisk(0.0)
        }

        // Initialization

        val durationInMinutes = ceil(durationInSeconds / 60.0).toInt()
        val deltas = listOf(39.0, 27.0, 23.0, 21.0, 20.0, 15.0)
        val po = -66.0
        val groupedRssis = List(durationInMinutes) { mutableListOf<Int>() }

        // Fading compensation

        val timestampedRssis = proximityInfos.mapNotNull { proximityInfo ->
            val metadata = proximityInfo.metadata as? BleProximityMetadata

            return@mapNotNull metadata?.let {
                val timestampDelta = (proximityInfo.timestamp.time - from.time) / 1_000
                val minute = floor(timestampDelta / 60.0).toInt()

                return@let if (minute < durationInMinutes) Pair(minute, metadata.calibratedRssi) else null
            }
        }

        timestampedRssis.forEach { (minute, rssi) ->
            if (minute < groupedRssis.size) {
                groupedRssis[minute].add(rssi)
            }
        }

        // Average RSSI and risk scoring

        val range = 0 until groupedRssis.lastIndex
        val score = range.fold(0.0) { partialScore, minute ->
            val rssis = groupedRssis[minute] + groupedRssis[minute + 1]

            return@fold if (rssis.isEmpty()) {
                0.0
            } else {
                val averageRssi = softmax(rssis)
                val gamma = (averageRssi - po) / deltas[min(rssis.size - 1, deltas.lastIndex)]
                val risk = gamma.coerceIn(0.0, 1.0)

                partialScore + risk
            }
        }

        return ProximityInfoRisk(score)
    }

    private fun softmax(inputs: List<Int>): Double {
        val a = 4.342
        val exponentialSum = inputs.fold(0.0) { accumulator, input -> accumulator + exp(input / a) }

        return if (inputs.isEmpty()) 0.0 else a * ln(exponentialSum / inputs.size)
    }
}
