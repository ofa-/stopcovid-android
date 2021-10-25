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
        const val LAST_VERSION_NAME: String = "Last.Version.Name"
        const val ZIP_GEOLOC_VERSION: String = "Zip.Geoloc.Version"
        const val PRIVATE_EVENT_QR_CODE: String = "Private.Event.QR.Code"
        const val PRIVATE_EVENT_QR_CODE_GENERATION_DATE: String = "Private.Event.QR.Code.Generation.Date"
        const val VENUES_ON_BOARDING_DONE: String = "isVenueOnBoardingDone"
        const val VENUES_FEATURED_ACTIVATED: String = "venuesFeaturedWasActivatedAtLeastOneTime"
        const val CURRENT_VACCINATION_REFERENCE_DEPARTMENT_CODE: String =
            "currentVaccinationReferenceDepartmentCode"
        const val CURRENT_VACCINATION_REFERENCE_LATITUDE: String =
            "currentVaccinationReferenceLatitude"
        const val CURRENT_VACCINATION_REFERENCE_LONGITUDE: String =
            "currentVaccinationReferenceLongitude"
        const val ALERT_RISK_LEVEL_CHANGED: String = "alertRiskLevelChanged"
        const val HIDE_RISK_STATUS: String = "hideRiskStatus"
        const val SHOW_CERTIFICATE_DETAILS: String = "showCertificateDetails"
        const val SHOW_ERROR_PANEL: String = "showErrorPanel"
        const val SHOW_ACTIVATION_REMINDER: String = "showActivationReminder"
        const val HAS_USED_UNIVERSAL_QR_SCAN: String = "hasUsedUniversalQrScan"
        const val RATINGS_KEY_FIGURES_OPENING: String = "ratingsKeyFiguresOpening"
        const val RATINGS_SHOWN: String = "ratingsShown"
        const val GOOGLE_REVIEW_SHOWN: String = "Google.Review.Shown"
        const val NOTIFICATION_VERSION_CLOSED: String = "Notification.Version.Closed"
        const val ACTIVITY_PASS_AUTO_RENEW_ENABLED: String = "Activity.Pass.Auto.Renew.Enabled"
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
        const val VACCINATION_COMPLETED_REMINDER: String = "StopCovid.VaccinationCompleted.Reminder.Worker"
        const val DCC_LIGHT_GENERATION: String = "StopCovid.DccLightGeneration.Worker"
        const val DCC_LIGHT_RENEW_CLEAN: String = "StopCovid.DccLightRenewClean.Worker"
        const val DCC_LIGHT_AVAILABLE: String = "StopCovid.DccLightAvailable.Worker"
    }

    object WorkerTags {
        const val DCC_LIGHT: String = "DCC_LIGHT_WORKERS"
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
        const val FIGURES_FRAGMENT_URI: String = "tousanticovid://allFigures/"
        const val CERTIFICATE_SHORTCUT_URI: String = "tousanticovid://attestations/"
        const val NEW_CERTIFICATE_SHORTCUT_URI: String = "tousanticovid://attestations//new_attestation"
        const val PROXIMITY_FRAGMENT_URI: String = "tousanticovid://proximity/"
        const val UNIVERSAL_QRCODE_SHORTCUT_URI: String = "tousanticovid://universalQRCode/"
        const val WALLET_CERTIFICATE_SHORTCUT_URI: String = "tousanticovid://walletCertificate/list"
        const val DCC_FULLSCREEN_SHORTCUT_URI: String = "tousanticovid://dccFullScreen/"
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
        const val X_ANIMATION_DURATION_MILLIS: Int = 1000
        const val AXIS_LABEL_TEXT_SIZE: Float = 15f
        const val EXTRA_BOTTOM_OFFSET: Float = 16f
        const val WIDGET_CIRCLE_SIZE: Float = 2f
        const val WIDGET_LINE_WIDTH: Float = 1f
        const val WIDGET_MARGIN_SIZE: Float = 6f
        const val ZOOM_MIN_THRESHOLD: Float = 1.25f
        const val SIGNIFICANT_DIGIT_MAX: Int = 3
    }

    object QrCode {
        const val FORMAT_2D_DOC: String = "2D-DOC"
    }

    object Certificate {
        const val MANUFACTURER_AUTOTEST: String = "AUTOTEST"
        const val DCC_LIGHT_PREFIX: String = "HCFR1:"
        const val DCC_EXEMPTION_PREFIX: String = "EX1:"
    }

    object HomeScreenWidget {
        const val NUMBER_VALUES_GRAPH_FIGURE: Int = 8
        const val WORKER_UPDATE_FIGURES_NAME: String = "updateFiguresWorker"
        const val WORKER_UPDATE_FIGURES_PERIODIC_REFRESH_HOURS: Long = 5
    }

    object Logs {
        const val DIR_NAME: String = "logs"
    }

    object Build {
        const val NB_DIGIT_MAJOR_RELEASE: Int = 2
    }
}
