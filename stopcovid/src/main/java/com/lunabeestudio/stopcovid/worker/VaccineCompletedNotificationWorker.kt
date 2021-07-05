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
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.WorkerParameters
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants

class VaccineCompletedNotificationWorker(context: Context, workerParams: WorkerParameters) : NotificationWorker(context, workerParams) {
    override val notificationChannelId: String = UiConstants.Notification.CERTIFICATE_REMINDER.channelId
    override val notificationChannelTitleKey: String = "notification.channel.vaccineCompleted.title"
    override val notificationChannelDefaultTitle: String = "Vaccination completed"
    override val pendingIntent: PendingIntent = NavDeepLinkBuilder(applicationContext)
        .setComponentName(MainActivity::class.java)
        .setGraph(R.navigation.nav_main)
        .setDestination(R.id.walletContainerFragment)
        .createPendingIntent()
    override val notificationTitleKey: String = "vaccineCompletionNotification.title"
    override val notificationBodyKey: String = "vaccineCompletionNotification.message"
    override val notificationId: Int = UiConstants.Notification.CERTIFICATE_REMINDER.notificationId
}