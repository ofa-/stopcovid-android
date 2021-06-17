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

import com.lunabeestudio.analytics.model.AppInfos
import com.lunabeestudio.analytics.model.HealthInfos
import com.lunabeestudio.analytics.model.Infos

sealed class SendAnalyticsRQ(
    val installationUuid: String,
    val events: List<TimestampedEventRQ>,
    val errors: List<TimestampedEventRQ>,
) {
    abstract val infos: Infos
}

class SendAppAnalyticsRQ(
    installationUuid: String,
    override val infos: AppInfos,
    events: List<TimestampedEventRQ>,
    errors: List<TimestampedEventRQ>
) : SendAnalyticsRQ(installationUuid, events, errors)

class SendHealthAnalyticsRQ(
    installationUuid: String,
    override val infos: HealthInfos,
    events: List<TimestampedEventRQ>,
    errors: List<TimestampedEventRQ>
) : SendAnalyticsRQ(installationUuid, events, errors)