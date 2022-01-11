/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/10/29 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.lunabeestudio.stopcovid.coreui.model.Action
import com.lunabeestudio.stopcovid.extension.getRelativeDateTimeString
import kotlin.time.Duration.Companion.milliseconds

abstract class TimeMainFragment : MainFragment() {

    private val timeUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            timeRefresh()
        }
    }

    abstract fun timeRefresh()

    override fun onResume() {
        super.onResume()
        context?.registerReceiver(timeUpdateReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
    }

    override fun onPause() {
        super.onPause()
        context?.unregisterReceiver(timeUpdateReceiver)
    }

    protected fun getStatusLastUpdateToDisplay(context: Context, lastRefresh: Long?, riskLevel: Float): String? = if (riskLevel > 0f) {
        null
    } else {
        stringsFormat(
            "myHealthController.notification.update",
            lastRefresh?.milliseconds?.getRelativeDateTimeString(context, strings) ?: ""
        )
    }

    protected fun refreshStatusActions(isUpdatingStatus: Boolean?): Action? {
        return if (isUpdatingStatus == true) {
            Action(label = strings["home.healthSection.statusState"], loading = true, onClickListener = null)
        } else {
            null
        }
    }
}