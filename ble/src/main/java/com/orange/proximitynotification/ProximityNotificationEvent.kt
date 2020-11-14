/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/09/14 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification

sealed class ProximityNotificationEvent {

    abstract val id: ProximityNotificationEventId

    data class Verbose(
        override val id: ProximityNotificationEventId,
        val message: String
    ) : ProximityNotificationEvent()

    data class Debug(
        override val id: ProximityNotificationEventId,
        val message: String
    ) : ProximityNotificationEvent()

    data class Info(
        override val id: ProximityNotificationEventId,
        val message: String
    ) : ProximityNotificationEvent()

    data class Error(
        override val id: ProximityNotificationEventId,
        val message: String,
        val cause: Throwable? = null
    ) : ProximityNotificationEvent()
}