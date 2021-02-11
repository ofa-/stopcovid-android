/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/26 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.filter

import java.util.Date
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

internal class RiskComputer(
    private val deltas: List<Double>,
    private val p0: Double,
    private val a: Double,
    private val timeWindow: Int,
    private val timeOverlap: Int
) {

    fun compute(
        timestampedRssis: List<TimestampedRssi>,
        from: Date,
        durationInSeconds: Long
    ): List<Double> {

        val timeSlotInterval = timeWindow - timeOverlap

        if (durationInSeconds <= 0 || timeSlotInterval <= 0) {
            return emptyList()
        }

        // Initialization
        val timeSlotCount =
            ceil((durationInSeconds.toDouble() / timeSlotInterval.toDouble())).toInt()
        val groupedRssis =
            timestampedRssis.sampleByTimeInterval(timeSlotInterval, timeSlotCount, from)

        val windowTimeSlotCount = timeWindow / timeSlotInterval

        // Average RSSI and risk scoring
        return groupedRssis.indices.map { timeSlot ->
            val rssis = mutableListOf<Int>()
            for (i in timeSlot until timeSlot + windowTimeSlotCount) {
                if (i in groupedRssis.indices) {
                    rssis += groupedRssis[i]
                }
            }

            return@map if (rssis.isEmpty()) {
                0.0
            } else {
                val averageRssi = rssis.softmax(a)
                val gamma = (averageRssi - p0) / deltas[min(
                    rssis.size - 1,
                    deltas.lastIndex
                )]
                gamma.coerceIn(0.0, 1.0)
            }
        }
    }
}


private fun List<TimestampedRssi>.sampleByTimeInterval(
    timeInterval: Int,
    timeSlotCount: Int,
    from: Date
): List<List<Int>> {
    val groupedRssis = List(timeSlotCount) { mutableListOf<Int>() }

    // Fading compensation

    forEach { timestampRssi ->
        val timestampDelta = (timestampRssi.timestamp.time - from.time) / 1_000
        val timeSlot = floor((timestampDelta.toDouble() / timeInterval.toDouble())).toInt()
        if (timeSlot in 0 until timeSlotCount) {
            groupedRssis[timeSlot].add(timestampRssi.rssi)
        }
    }
    return groupedRssis
}
