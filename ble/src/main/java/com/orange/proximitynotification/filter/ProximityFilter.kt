/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/28 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.filter

import java.util.Date
import kotlin.math.ln

/**
 * Proximity information filter
 */
class ProximityFilter(private val config: Config) {

    /**
     * ProximityFilter configuration
     *
     * @param durationThreshold Duration threshold in seconds. RSSIs are kept if the period over which they were received exceeds this threshold. Default value is 5 minutes.
     * @param rssiThreshold RSSI threshold in dbM. The threshold above which an RSSI value is clipped. Default value is 0 dbM. Has no effect if filtering mode is [Mode.FULL].
     * @param deltas An array used to weight the risk according to the number of RSSI values contained in a time slot. Default values are [39.0, 27.0, 23.0, 21.0, 20.0, 19.0, 18.0, 17.0, 16.0, 15.0]. Has no effect if filtering mode is [Mode.FULL].
     * @param p0 The power below which a received RSSI value is considered to be zero risk, in dbM. Default value is -66 dbM. Has no effect if filtering mode is [Mode.FULL].
     * @param a A constant for the softmax function which is applied to RSSIs when computing the risk. Default value is 10ln(10). Has no effect if filtering mode is [Mode.FULL].
     * @param b A constant for the softmax function used to compute the risk if filtering mode is [Mode.RISKS]. Default value is 0.1.
     * @param timeWindow The period (in seconds) over which a partial risk is computed. Default value is 120 seconds.
     * @param timeOverlap The period (in seconds) over which two successive time windows overlap. Default value is 60 seconds.
     * @param riskThreshold Risk threshold. The threshold above which RSSIs are accepted if filtering mode is [Mode.RISKS]. Default value is 0.2.
     */
    data class Config(
        val durationThreshold: Long = 5 * 60,
        val rssiThreshold: Int = 0,
        val deltas: List<Double> = listOf(
            39.0,
            27.0,
            23.0,
            21.0,
            20.0,
            19.0,
            18.0,
            17.0,
            16.0,
            15.0
        ),
        val p0: Double = -66.0,
        val a: Double = 10 / ln(10.0),
        val b: Double = 0.1,
        val timeWindow: Int = 2 * 60,
        val timeOverlap: Int = 60,
        val riskThreshold: Double = 0.2
    )


    /**
     * Processing output
     */
    sealed class Output {

        /**
         * RSSIs were rejected
         */
        object Rejected : Output()

        /**
         * RSSIs were accepted
         *
         * @param timestampedRssis output list of [TimestampedRssi]. If [areTimestampedRssisUpdated] is false it contains timestampedRssis given in [ProximityFilter.filter]
         * @param areTimestampedRssisUpdated true if [timestampedRssis] were updated. False otherwise
         * @param windowRisks computed risks for each [Config.timeWindow] slot or null if filtering mode is [Mode.FULL]
         * @param meanPeak Mean value of RSSI peak or null if filtering mode is [Mode.FULL]
         * @param peakCount RSSI peak count or null if filtering mode is [Mode.FULL]
         * @param durationInMinutes The period (in minutes) over which the RSSIs where received. null if filtering mode is [Mode.FULL], [Mode.MEDIUM]
         * @param riskDensity Risk density. null if filtering mode is [Mode.FULL], [Mode.MEDIUM]
         * @param intermediateRisk computed intermediate risk based on [windowRisks] or null if filtering mode is [Mode.FULL], [Mode.MEDIUM]
         * @param risk computed risk or null if filtering mode is [Mode.FULL], [Mode.MEDIUM]
         */
        data class Accepted(
            val timestampedRssis: List<TimestampedRssi>,
            val areTimestampedRssisUpdated: Boolean,
            val windowRisks: List<Double>? = null,
            val meanPeak: Double? = null,
            val peakCount: Int? = null,
            val durationInMinutes: Double? = null,
            val riskDensity: Int? = null,
            val intermediateRisk: Double? = null,
            val risk: Double? = null
        ) : Output()
    }

    enum class Mode {
        /**
         * An all-or-nothing mode where RSSIs are kept if the period over which they were received exceeds a given threshold.
         */
        FULL,

        /**
         * A mode that first performs steps from the full mode, then clips the RSSIs that exceed a given threshold and finally computes a risk.
         */
        MEDIUM,

        /**
         * A mode that first performs steps from the medium mode and then computes additional relevant output values. With this mode, the risk is computed from a softmax function.
         */
        RISKS
    }

    fun filter(
        timestampedRssis: List<TimestampedRssi>,
        epochStart: Date,
        epochDuration: Long,
        mode: Mode
    ): Output {
        if (timestampedRssis.isEmpty()) {
            return Output.Rejected
        }

        val sortedTimestampedRssis = timestampedRssis.sortedBy { it.timestamp }
        val durationBetweenFirstAndLastRssiInSeconds =
            (sortedTimestampedRssis.last().timestamp.time - sortedTimestampedRssis.first().timestamp.time) / 1000

        if (durationBetweenFirstAndLastRssiInSeconds < config.durationThreshold) {
            return Output.Rejected
        }

        if (mode == Mode.FULL) {
            return Output.Accepted(sortedTimestampedRssis, false)
        }

        val rssiClipper = RssiClipper(config.rssiThreshold)
        val riskComputer =
            RiskComputer(config.deltas, config.p0, config.a, config.timeWindow, config.timeOverlap)

        val clipOutput = rssiClipper.clip(sortedTimestampedRssis)
        val windowRisks =
            riskComputer.compute(clipOutput.clippedTimestampedRssis, epochStart, epochDuration)

        val timestampedRssisUpdated = clipOutput.filteredPeaks.isNotEmpty()

        if (mode == Mode.MEDIUM) {
            return Output.Accepted(
                timestampedRssis = clipOutput.clippedTimestampedRssis,
                areTimestampedRssisUpdated = timestampedRssisUpdated,
                windowRisks = windowRisks,
                meanPeak = clipOutput.filteredPeaks.average(),
                peakCount = clipOutput.filteredPeaks.size
            )
        }

        val durationInMinutes = durationBetweenFirstAndLastRssiInSeconds.toDouble() / 60
        val riskDensity = windowRisks.count { it > 0 }
        val intermediateRisk = windowRisks.softmax(config.b)
        val risk = intermediateRisk * (durationInMinutes + riskDensity) / (windowRisks.size * 2)

        // Risks mode
        if (risk < config.riskThreshold) {
            return Output.Rejected
        }

        return Output.Accepted(
            timestampedRssis = clipOutput.clippedTimestampedRssis,
            areTimestampedRssisUpdated = timestampedRssisUpdated,
            windowRisks = windowRisks,
            meanPeak = clipOutput.filteredPeaks.average(),
            peakCount = clipOutput.filteredPeaks.size,
            durationInMinutes = durationInMinutes,
            riskDensity = riskDensity,
            intermediateRisk = intermediateRisk,
            risk = risk
        )
    }


}

