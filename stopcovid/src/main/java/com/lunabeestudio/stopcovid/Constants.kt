/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid

object Constants {
    object SharedPrefs {
        const val ON_BOARDING_DONE: String = "On.Boarding.Done"
        const val LAST_PRIVACY_REFRESH: String = "Last.Privacy.Refresh"
        const val LAST_MAINTENANCE_REFRESH: String = "Last.Maintenance.Refresh"
    }

    object WorkerNames {
        const val NOTIFICATION: String = "StopCovid.Notification.Worker"
    }

    object ServerConstant {
        val ACCEPTED_REPORT_CODE_LENGTH: List<Int> = listOf(6, 36)
    }
}