/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.domain

object RobertConstant {
    const val EPOCH_DURATION_S: Int = 15 * 60
    const val HELLO_REFRESH_MAX_DELAY_MS: Long = 30 * 1000

    object PREFIX {
        const val C1: Byte = 0b00000001
        const val C2: Byte = 0b00000010
        const val C3: Byte = 0b00000011
        const val C4: Byte = 0b00000100
    }
}