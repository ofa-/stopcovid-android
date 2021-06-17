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

internal class RssiClipper(private val rssiThreshold: Int) {
    data class Output(
        val clippedTimestampedRssis: List<TimestampedRssi>,
        val filteredPeaks: List<Int>
    )

    fun clip(timestampedRssis: List<TimestampedRssi>): Output {

        val peaks = mutableListOf<Int>()

        val clippedTimestampRssis = mutableListOf<TimestampedRssi>()
        timestampedRssis.forEachIndexed { index, timestampRssi ->

            val currentRssi = timestampRssi.rssi

            if (currentRssi > rssiThreshold) {
                peaks += currentRssi

                val previousRssi =
                    if (index == 0) currentRssi else clippedTimestampRssis[index - 1].rssi
                val nextRssi =
                    if (index == (timestampedRssis.size - 1)) currentRssi else timestampedRssis[index + 1].rssi

                val updatedRssi = minOf(rssiThreshold, minOf(previousRssi, nextRssi))
                clippedTimestampRssis.add(timestampRssi.copy(rssi = updatedRssi))
            } else {
                clippedTimestampRssis.add(timestampRssi)
            }
        }

        return Output(clippedTimestampRssis, peaks)
    }
}
