/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert

import com.lunabeestudio.robert.manager.LocalProximityFilter

object RobertConstant {
    const val STATUS_WORKER_NAME: String = "RobertManager.Status.Worker"
    const val EPOCH_DURATION_S: Int = 15 * 60
    const val KA_STRING_INPUT: String = "mac"
    const val KEA_STRING_INPUT: String = "tuples"
    const val DEFAULT_CALIBRATION_KEY: String = "DEFAULT"
    const val REGISTER_DELAY_MONTH: Int = 2
    val BLE_FILTER_MODE: LocalProximityFilter.Mode = LocalProximityFilter.Mode.RISKS
    const val LAST_CONTACT_DELTA_S: Long = 24 * 60 * 60

    object PREFIX {
        const val C1: Byte = 0b00000001
        const val C2: Byte = 0b00000010
        const val C3: Byte = 0b00000011
        const val C4: Byte = 0b00000100
    }
}
