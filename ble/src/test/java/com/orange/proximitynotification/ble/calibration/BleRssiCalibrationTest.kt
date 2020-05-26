/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble.calibration

import com.google.common.truth.Truth
import com.googlecode.zohhak.api.TestWith
import com.googlecode.zohhak.api.runners.ZohhakRunner
import org.junit.runner.RunWith

@RunWith(ZohhakRunner::class)
class BleRssiCalibrationTest {

    @TestWith(
        value = [
            "0,0,0,0",
            "-10,1,1,-12",
            "10,1,1,8"
        ]
    )
    fun calibrate_show_sum_rssi_grx_gtx(rssi: Int, gRx: Int, gTx: Int, expectedRssi: Int) {

        // Given

        // When
        val result = BleRssiCalibration.calibrate(
            rssi = rssi,
            rxCompensationGain = gRx,
            txCompensationGain = gTx
        )

        // Then
        Truth.assertThat(result).isEqualTo(expectedRssi)
    }
}