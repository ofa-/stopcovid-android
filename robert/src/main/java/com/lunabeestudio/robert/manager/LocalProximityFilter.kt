/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/01/06 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.manager

import com.lunabeestudio.domain.model.LocalProximity

interface LocalProximityFilter {
    enum class Mode {
        FULL,
        MEDIUM,
        RISKS
    }

    fun filter(localProximityList: List<LocalProximity>, mode: LocalProximityFilter.Mode, configJson: String): List<LocalProximity>
}