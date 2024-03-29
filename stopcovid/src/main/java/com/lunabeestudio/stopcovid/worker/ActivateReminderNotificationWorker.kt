/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/28/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.worker

import android.app.PendingIntent
import android.content.Context
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.WorkerParameters
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.fragment.HomeFragment

class ActivateReminderNotificationWorker(context: Context, workerParams: WorkerParameters) : NotificationWorker(context, workerParams) {
    override val notificationChannelId: String = UiConstants.Notification.ACTIVATE_REMINDER.channelId
    override val notificationChannelTitleKey: String = "notification.reactivationReminder.title"
    override val notificationChannelDefaultTitle: String = "Reminder"
    override val pendingIntent: PendingIntent = NavDeepLinkBuilder(applicationContext)
        .setComponentName(MainActivity::class.java)
        .setGraph(R.navigation.nav_main)
        .setDestination(R.id.homeFragment)
        .setArguments(bundleOf(Pair(HomeFragment.START_PROXIMITY_ARG_KEY, true)))
        .createPendingIntent()
    override val notificationTitleKey: String = "notification.reactivationReminder.title"
    override val notificationBodyKey: String = "notification.reactivationReminder.body"
    override val notificationId: Int = UiConstants.Notification.ACTIVATE_REMINDER.notificationId
}