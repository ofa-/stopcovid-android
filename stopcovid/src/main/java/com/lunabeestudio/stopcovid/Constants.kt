/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid

import java.util.concurrent.TimeUnit

object Constants {
    object SharedPrefs {
        const val ON_BOARDING_DONE: String = "On.Boarding.Done"
        const val LAST_PRIVACY_REFRESH: String = "Last.Privacy.Refresh"
        const val LAST_LINKS_REFRESH: String = "Last.Links.Refresh"
        const val LAST_MORE_KEY_FIGURES_REFRESH: String = "Last.MoreKeyFigures.Refresh"
        const val LAST_MAINTENANCE_REFRESH: String = "Last.Maintenance.Refresh"
        const val LAST_INFO_CENTER_REFRESH: String = "Last.Info.Center.Refresh"
        const val LAST_INFO_CENTER_FETCH: String = "Last.Info.Center.Fetch"
        const val HAS_NEWS: String = "Has.News"
        const val CHOSEN_POSTAL_CODE: String = "Chosen.Postal.Code"
        const val IS_ADVERTISEMENT_AVAILABLE: String = "Is.Advertisement.Available"
        const val ARE_INFO_NOTIFICATIONS_ENABLED: String = "Are.Info.Notifications.Enabled"
        const val LAST_VERSION_CODE: String = "Last.Version.Code"
        const val PRIVATE_EVENT_QR_CODE: String = "Private.Event.QR.Code"
        const val PRIVATE_EVENT_QR_CODE_GENERATION_DATE: String = "Private.Event.QR.Code.Generation.Data"
        const val VENUES_ON_BOARDING_DONE: String = "isVenueOnBoardingDone"
        const val VENUES_FEATURED_ACTIVATED: String = "venuesFeaturedWasActivatedAtLeastOneTime"
    }

    object Notification {
        const val APP_IN_MAINTENANCE: String = "App.In.Maintenance"
        const val SERVICE_ERROR: String = "Service.Error"
        const val SERVICE_ERROR_EXTRA: String = "Service.Error.Extra"
    }

    object WorkerNames {
        const val AT_RISK_NOTIFICATION: String = "StopCovid.AtRisk.Notification.Worker"
        const val WARNING_NOTIFICATION: String = "StopCovid.Warning.Notification.Worker"
        const val ACTIVATE_REMINDER: String = "StopCovid.Activate.Reminder.Worker"
        const val ISOLATION_REMINDER: String = "StopCovid.Isolation.Reminder.Worker"
        const val TIME_CHANGED: String = "StopCovid.TimeChanged.Worker"
    }

    object ServerConstant {
        val MAX_GAP_DEVICE_SERVER: Long = TimeUnit.MINUTES.toMillis(2L)
    }

    object Attestation {
        const val KEY_REASON: String = "reason"
        const val KEY_DATE_TIME: String = "datetime"
        const val VALUE_REASON_SPORT: String = "sport_animaux"
    }

    object Url {
        const val VENUE_ROOT_URL: String = "https://tac.gouv.fr/"
    }

    object Android {
        const val ANIMATION_DELAY: Long = 500L
    }
}
