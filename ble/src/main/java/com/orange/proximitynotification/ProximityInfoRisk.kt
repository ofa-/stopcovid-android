/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/11 - for the STOP-COVID project
 */

package com.orange.proximitynotification

data class ProximityInfoRisk(val score: Double) {

    enum class Level {
        LOW,
        MEDIUM,
        HIGH
    }

    val level: Level
        get() = when {
            score < 3 -> Level.LOW
            score < 7 -> Level.MEDIUM
            else -> Level.HIGH
        }
}

