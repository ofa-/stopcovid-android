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
        const val ARE_INFO_NOTIFICATIONS_ENABLED: String = "Are.Info.Notifications.Enabled"
        const val LAST_VERSION_CODE: String = "Last.Version.Code"
        const val ZIP_GEOLOC_VERSION: String = "Zip.Geoloc.Version"
        const val PRIVATE_EVENT_QR_CODE: String = "Private.Event.QR.Code"
        const val PRIVATE_EVENT_QR_CODE_GENERATION_DATE: String = "Private.Event.QR.Code.Generation.Data"
        const val VENUES_ON_BOARDING_DONE: String = "isVenueOnBoardingDone"
        const val VENUES_FEATURED_ACTIVATED: String = "venuesFeaturedWasActivatedAtLeastOneTime"
        const val CURRENT_VACCINATION_REFERENCE_DEPARTMENT_CODE: String = "currentVaccinationReferenceDepartmentCode"
        const val CURRENT_VACCINATION_REFERENCE_LATITUDE: String = "currentVaccinationReferenceLatitude"
        const val CURRENT_VACCINATION_REFERENCE_LONGITUDE: String = "currentVaccinationReferenceLongitude"
        const val ALERT_RISK_LEVEL_CHANGED: String = "alertRiskLevelChanged"
        const val HIDE_RISK_STATUS: String = "hideRiskStatus"
        const val SHOW_ACTIVATION_REMINDER: String = "showActivationReminder"
    }

    object Notification {
        const val APP_IN_MAINTENANCE: String = "App.In.Maintenance"
        const val SERVICE_ERROR: String = "Service.Error"
        const val SERVICE_ERROR_EXTRA: String = "Service.Error.Extra"
    }

    object WorkerNames {
        const val AT_RISK_NOTIFICATION: String = "StopCovid.AtRisk.Notification.Worker"
        const val ACTIVATE_REMINDER: String = "StopCovid.Activate.Reminder.Worker"
        const val ISOLATION_REMINDER: String = "StopCovid.Isolation.Reminder.Worker"
        const val TIME_CHANGED: String = "StopCovid.TimeChanged.Worker"
    }

    object ServerConstant {
        val MAX_GAP_DEVICE_SERVER: Long = TimeUnit.MINUTES.toMillis(2L)
    }

    object Attestation {
        const val DATA_KEY_REASON: String = "reason"
        const val KEY_DATE_TIME: String = "datetime"
        const val KEY_CREATION_DATE: String = "creationDate"
        const val KEY_CREATION_HOUR: String = "creationHour"
        const val VALUE_REASON_SPORT: String = "sport_animaux"
    }

    object Url {
        const val VENUE_ROOT_URL: String = "https://tac.gouv.fr/"
        const val PLAY_STORE_URL: String = "https://play.google.com/store/apps/details?id="
    }

    object Android {
        const val ANIMATION_DELAY: Long = 500L
        const val FORCE_LOADING_DELAY: Long = 2000L
    }

    object Chart {
        const val LIMIT_LINE_TEXT_SIZE: Float = 15f
        const val SHARE_CHART_FILENAME: String = "chart.jpg"
        const val MIN_CIRCLE_RADIUS_SIZE: Float = 1.75f
        const val RESIZE_START_CIRCLE_COUNT: Float = 25f
        const val DEFAULT_CIRCLE_SIZE: Float = 4f
        const val CIRCLE_LINE_RATIO: Float = 2f
        const val Y_AXIS_LABEL_COUNT: Int = 3
        const val X_AXIS_LABEL_COUNT: Int = 2
        const val AXIS_LABEL_TEXT_SIZE: Float = 15f
        const val EXTRA_BOTTOM_OFFSET: Float = 16f
    }

    object QrCode {
        const val FORMAT_2D_DOC: String = "2D-DOC"
    }
}
