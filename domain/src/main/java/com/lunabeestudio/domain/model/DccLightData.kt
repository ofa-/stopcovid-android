/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/27/9 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.domain.model

import java.util.Date

class DccLightData(
    val completed: Boolean,
    val rawDcc: Map<Date, String>,
)