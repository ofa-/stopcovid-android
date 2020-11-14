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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager
import com.lunabeestudio.stopcovid.fragment.ProximityFragment

class ReminderNotificationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (StringsManager.strings.isEmpty()) {
            StringsManager.initialize(applicationContext)
        }
        val strings = StringsManager.strings

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.REMINDER.channelId,
                strings["notification.reactivationReminder.title"] ?: "Reminder",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = NavDeepLinkBuilder(applicationContext)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_main)
            .setDestination(R.id.proximityFragment)
            .setArguments(bundleOf(Pair(ProximityFragment.START_PROXIMITY_ARG_KEY, true)))
            .createPendingIntent()

        val notification = NotificationCompat.Builder(
            applicationContext,
            UiConstants.Notification.REMINDER.channelId
        )
            .setContentTitle(strings["notification.reactivationReminder.title"])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_notification_bar)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(strings["notification.reactivationReminder.body"])
            )
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(UiConstants.Notification.REMINDER.notificationId, notification)

        return Result.success()
    }
}