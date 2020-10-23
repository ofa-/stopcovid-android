/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/09/14 - for the STOP-COVID project
 */

package com.orange.proximitynotification

object ProximityNotificationLogger {

    interface Listener {
        fun onEvent(event: ProximityNotificationEvent)
    }

    private var listener: Listener? = null

    @Synchronized
    fun registerListener(listener: Listener) {
        this.listener = listener
    }

    @Synchronized
    fun unregisterListener() {
        this.listener = null
    }

    @Synchronized
    fun log(event: ProximityNotificationEvent) = listener?.onEvent(event)

    fun debug(eventId: ProximityNotificationEventId, message: String) = log(
        ProximityNotificationEvent.Debug(eventId, message)
    )

    fun info(eventId: ProximityNotificationEventId, message: String) = log(
        ProximityNotificationEvent.Info(eventId, message)
    )

    fun error(eventId: ProximityNotificationEventId, message: String, cause: Throwable? = null) =
        log(ProximityNotificationEvent.Error(eventId, message, cause))
}