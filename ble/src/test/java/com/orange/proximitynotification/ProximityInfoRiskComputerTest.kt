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

import com.google.common.truth.Truth.assertThat
import com.googlecode.zohhak.api.TestWith
import com.googlecode.zohhak.api.runners.ZohhakRunner
import com.orange.proximitynotification.ble.bleProximityInfo
import com.orange.proximitynotification.ble.bleProximityMetadata
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Date

@RunWith(ZohhakRunner::class)
class ProximityInfoRiskComputerTest {

    private val proximityInfoRiskComputer = ProximityInfoRiskComputer()

    private val durationInSeconds = 16 * 60L

    @Test
    fun computeRisk_without_data_should_return_score_at_0() {

        // Given
        val proximityInfos: List<ProximityInfo> = emptyList()
        val expected = ProximityInfoRisk(0.0)

        // When
        val result =
            proximityInfoRiskComputer.computeRisk(proximityInfos, Date(), durationInSeconds)

        // Then
        assertThat(result).isEqualTo(expected)
    }

    @TestWith(
        value = [
            "0", "1", "2", "3"
        ]
    )
    fun computeRisk_with_dataset_should_return_expected_score(index: Int) {
        // Given
        val (proximityInfos, date, expected) = parseDataset(index)

        // When
        val result = proximityInfoRiskComputer.computeRisk(proximityInfos, date, durationInSeconds)

        // Then
        assertEquals(expected.score, result.score, 0.0001)
    }

    private fun parseDataset(index: Int): Triple<List<ProximityInfo>, Date, ProximityInfoRisk> {
        val minTimestamp = (durationInSeconds - 60) * 1_000 * index
        val maxTimestamp = minTimestamp + durationInSeconds * 1_000
        val proximityInfos = mutableListOf<ProximityInfo>()
        val scores = mutableListOf<Double>()

        val decimalFormat = DecimalFormat()
        val symbols = DecimalFormatSymbols()
        symbols.decimalSeparator = ','
        decimalFormat.decimalFormatSymbols = symbols

        ProximityInfoRiskComputerTest::class.java.classLoader!!
            .getResourceAsStream("risk_computer_dataset.csv")
            .bufferedReader(charset = Charsets.UTF_8)
            .useLines { lines ->

                lines.filterIndexed { index, _ -> index != 0 }
                    .map { line -> line.split(";") }
                    .forEach { values ->
                        values.getOrNull(8)?.let { score ->
                            try {
                                decimalFormat.parse(score)?.let { scores.add(it.toDouble()) }
                            } catch (exception: Exception) {
                            }
                        }
                        val calibratedRssi = values.getOrNull(1)?.toIntOrNull()
                        val timestamp = values.getOrNull(2)?.toLongOrNull()
                        if (calibratedRssi != null && timestamp != null && timestamp >= minTimestamp && timestamp < maxTimestamp) {
                            val proximityInfo = bleProximityInfo(
                                timestamp = Date(timestamp),
                                metadata = bleProximityMetadata(calibratedRssi = calibratedRssi)
                            )
                            proximityInfos.add(proximityInfo)
                        }
                    }
            }

        val score = scores.getOrNull(index)
        if (score == null) {
            throw Exception("Could not parse score at index $index in risk_computer_dataset.csv")
        } else {
            return Triple(proximityInfos, Date(minTimestamp), ProximityInfoRisk(score))
        }
    }
}
