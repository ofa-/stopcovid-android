/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/13/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.coreui

object UiConstants {

    enum class Permissions {
        CAMERA, LOCATION
    }

    object SharePrefs {
        const val LAST_STRINGS_REFRESH: String = "Last.Strings.Refresh"
    }

    enum class Notification(val channelId: String, val notificationId: Int) {
        AT_RISK("atRisk", 1),
        PROXIMITY("proximity", 2),
        ERROR("error", 3),
        UPGRADE("upgrade", 4),
        TIME("error", 5),
        BLUETOOTH("error", 6),
        NEWS("news", 7),
        ACTIVATE_REMINDER("reminder", 8),
        ISOLATION_REMINDER("reminder", 9),
        CERTIFICATE_REMINDER("reminder", 10),
    }

    val SUPPORTED_LANGUAGE: Array<String> = arrayOf("en", "fr")

    const val DEFAULT_LANGUAGE: String = "en"
}