/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/03/06 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lunabeestudio.stopcovid.StopCovid

class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED) {
            (context.applicationContext as StopCovid).startAppMaintenanceWorker(true)
        }
    }
}