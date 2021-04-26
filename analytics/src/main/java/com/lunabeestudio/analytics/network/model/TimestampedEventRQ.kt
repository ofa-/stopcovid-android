/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/05/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.analytics.network.model

import com.lunabeestudio.analytics.model.Infos
import com.lunabeestudio.analytics.model.TimestampedEvent

class TimestampedEventRQ(
    var name: String,
    var timestamp: String,
    var desc: String?,
)