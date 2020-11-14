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

import com.google.common.truth.Truth.assertThat
import com.googlecode.zohhak.api.Coercion
import com.googlecode.zohhak.api.TestWith
import com.googlecode.zohhak.api.runners.ZohhakRunner
import com.orange.proximitynotification.tools.CSVReader
import org.junit.runner.RunWith
import java.util.Date

@RunWith(ZohhakRunner::class)
class ProximityFilterIntTest {

    companion object {
        private val EPOCH_START = Date(0)
        private const val EPOCH_DURATION: Long = 15 * 60
    }

    private var timestampedRssis = mutableListOf<TimestampedRssi>()
    private var clippedRssisByRssiThreshold = mutableMapOf<Int, MutableList<Int>>()

    init {
        val csvReader = CSVReader("timestamp_rssi_dataset.csv")
        csvReader.readLines { values ->
            timestampedRssis.add(
                timestampedRssi(
                    timestamp = Date(values[0].toLong() * 1000),
                    rssi = values[1].toInt()
                )
            )
            clippedRssisByRssiThreshold.getOrPut(-35) { mutableListOf() }.add(values[2].toInt())
            clippedRssisByRssiThreshold.getOrPut(-60) { mutableListOf() }.add(values[3].toInt())
        }
    }

    @TestWith(
        value = [
            "2, true",
            "5, true",
            "14, true",
            "15, false"
        ]
    )
    fun filter_given_full_mode(durationThresholdInMinutes: Int, accepted: Boolean) {

        // Given
        val config = ProximityFilter.Config(durationThreshold = durationThresholdInMinutes * 60L)
        val proximityFilter = ProximityFilter(config)

        // When
        val output = proximityFilter.filter(
            timestampedRssis,
            EPOCH_START,
            EPOCH_DURATION,
            ProximityFilter.Mode.FULL
        )

        // Then
        assertThat(output is ProximityFilter.Output.Accepted).isEqualTo(accepted)

        if (accepted) {
            val okOutput = output as ProximityFilter.Output.Accepted
            assertThat(okOutput.areTimestampedRssisUpdated).isFalse()
            assertThat(okOutput.timestampedRssis).isEqualTo(timestampedRssis)
            assertThat(okOutput.peakCount).isNull()
            assertThat(okOutput.meanPeak).isNull()
            assertThat(okOutput.intermediateRisk).isNull()
            assertThat(okOutput.risk).isNull()
            assertThat(okOutput.riskDensity).isNull()
            assertThat(okOutput.durationInMinutes).isNull()
        }
    }

    @TestWith(
        separator = ";",
        value = [
            "-35;1;-15.0;4.34;-66.0;[0.22223279,0,0,0,0,0,0.01226278,0.096550071,0.118937533,0.121459094,0.060818523,0.0287871,0,0,0]",
            "-35;1;-15.0;10.0;-66.0;[0,0,0,0,0,0,0,0.076291313,0.095690344,0.090033589,0.044448579,0.013257903,0,0,0]",
            "-35;1;-15.0;4.34;-55.0;[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]",
            "-60;3;-41.0;4.34;-66.0;[0,0,0,0,0,0,0.01226278,0.096550071,0.118937533,0.121459094,0.060818523,0.0287871,0,0,0]"
        ]
    )
    fun filter_given_medium_mode(
        rssiThreshold: Int,
        peakCount: Int,
        meanPeak: Double,
        a: Double,
        p0: Double,
        windowRisks: List<Double>
    ) {

        // Given
        val config = ProximityFilter.Config(
            durationThreshold = 2 * 60L,
            rssiThreshold = rssiThreshold,
            timeWindow = 120,
            timeOverlap = 60,
            a = a,
            p0 = p0
        )
        val proximityFilter = ProximityFilter(config)

        // When
        val output = proximityFilter.filter(
            timestampedRssis,
            EPOCH_START,
            EPOCH_DURATION,
            ProximityFilter.Mode.MEDIUM
        )

        // Then
        assertThat(output is ProximityFilter.Output.Accepted).isTrue()
        val okOutput = output as ProximityFilter.Output.Accepted
        assertThat(okOutput.areTimestampedRssisUpdated).isTrue()
        assertThat(okOutput.timestampedRssis.map { it.rssi }).isEqualTo(clippedRssisByRssiThreshold[rssiThreshold])
        assertThat(okOutput.peakCount).isEqualTo(peakCount)
        assertThat(okOutput.meanPeak).isEqualTo(meanPeak)


        assertThat(okOutput.windowRisks).hasSize(windowRisks.size)
        windowRisks.forEachIndexed { index, d ->
            assertThat(okOutput.windowRisks?.get(index)).isWithin(0.0001).of(d)
        }

        assertThat(okOutput.intermediateRisk).isNull()
        assertThat(okOutput.risk).isNull()
        assertThat(okOutput.riskDensity).isNull()
        assertThat(okOutput.durationInMinutes).isNull()
    }

    @TestWith(
        separator = ";",
        value = [
            "-35;0.0;4.34;0.2;-66.0;1;-15.0;[0.22223279,0,0,0,0,0,0.01226278,0.096550071,0.118937533,0.121459094,0.060818523,0.0287871,0,0,0];0.056330584;0.041121327;7; true",
            "-35;0.0;10.0;0.2;-66.0;1;-15.0;[0,0,0,0,0,0,0,0.076291313,0.095690344,0.090033589,0.044448579,0.013257903,0,0,0];0.024608374;0.016323555;5; true",
            "-35;0.0;4.34;0.1;-66.0;1;-15.0;[0.22223279,0,0,0,0,0,0.01226278,0.096550071,0.118937533,0.121459094,0.060818523,0.0287871,0,0,0];0.071978844;0.052544556;7; true",
            "-35;0.0;4.34;0.2;-55.0;1;-15.0;[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0];0;0;0; true",
            "-60;0.0;4.34;0.2;-66.0;3;-41.0;[0,0,0,0,0,0,0.01226278,0.096550071,0.118937533,0.121459094,0.060818523,0.0287871,0,0,0];0.034699744;0.024174155;6; true",
            "-35;0.2;4.34;0.2;-66.0;null;null;null;null;null;null; false",
            "-35;0.2;4.34;0.2;-75.0;1;-15.0;[0.82223279,0.00044885,0,0.165984332,0.279341285,0.365483185,0.61226278,0.696550071,0.718937533,0.721459094,0.660818523,0.6287871,0.555474001,0.321603449,0.041594884];0.582083943;0.560740866;14; true"
        ]
    )
    fun filter_given_risks_mode(
        rssiThreshold: Int,
        riskThreshold: Double,
        a: Double,
        b: Double,
        p0: Double,
        peakCount: Int?,
        meanPeak: Double?,
        windowRisks: List<Double>?,
        intermediateRisk: Double?,
        risk: Double?,
        riskDensity: Int?,
        accepted: Boolean
    ) {

        // Given
        val config = ProximityFilter.Config(
            durationThreshold = 2 * 60L,
            rssiThreshold = rssiThreshold,
            timeWindow = 120,
            timeOverlap = 60,
            a = a,
            b = b,
            p0 = p0,
            riskThreshold = riskThreshold
        )
        val proximityFilter = ProximityFilter(config)

        // When
        val output = proximityFilter.filter(
            timestampedRssis,
            EPOCH_START,
            EPOCH_DURATION,
            ProximityFilter.Mode.RISKS
        )

        // Then
        assertThat(output is ProximityFilter.Output.Accepted).isEqualTo(accepted)

        if (accepted) {
            val okOutput = output as ProximityFilter.Output.Accepted
            assertThat(okOutput.areTimestampedRssisUpdated).isTrue()
            assertThat(okOutput.timestampedRssis.map { it.rssi }).isEqualTo(
                clippedRssisByRssiThreshold[rssiThreshold]
            )
            assertThat(okOutput.peakCount).isEqualTo(peakCount)
            assertThat(okOutput.meanPeak).isEqualTo(meanPeak)
            assertThat(okOutput.windowRisks).hasSize(windowRisks!!.size)
            windowRisks.forEachIndexed { index, d ->
                assertThat(okOutput.windowRisks?.get(index)).isWithin(0.0001).of(d)
            }
            assertThat(okOutput.intermediateRisk).isWithin(0.0001).of(intermediateRisk!!)
            assertThat(okOutput.risk).isWithin(0.0001).of(risk!!)
            assertThat(okOutput.riskDensity).isEqualTo(riskDensity)
            assertThat(okOutput.durationInMinutes).isEqualTo(14.9)
        }
    }

    @Coercion
    fun toDoubleArray(input: String): List<Double> {
        return runCatching {
            input
                .replace("[", "")
                .replace("]", "")
                .split(",")
                .map { it.toDouble() }
        }.getOrElse { emptyList() }
    }

}