/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/28/05 - for the STOP-COVID project
 */

package com.lunabeestudio.stopcovid.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.navigation.NavDeepLinkBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.coreui.manager.StringsManager

class AtRiskNotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private var strings: HashMap<String, String> = StringsManager.getStrings()

    override fun doWork(): Result {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UiConstants.Notification.AT_RISK.channelId,
                strings["notification.channel.atRisk.title"] ?: "At risk",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = NavDeepLinkBuilder(applicationContext)
            .setComponentName(MainActivity::class.java)
            .setGraph(R.navigation.nav_main)
            .setDestination(R.id.informationFragment)
            .createPendingIntent()

        val notification = NotificationCompat.Builder(applicationContext,
            UiConstants.Notification.AT_RISK.channelId
        )
            .setContentTitle(strings["notification.atRisk.title"])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_notification_bar)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(strings["notification.atRisk.message"]))
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(UiConstants.Notification.AT_RISK.notificationId, notification)

        return Result.success()
    }
}