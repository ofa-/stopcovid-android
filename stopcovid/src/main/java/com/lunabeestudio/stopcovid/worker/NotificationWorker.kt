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
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.extension.stringsManager

abstract class NotificationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    abstract val notificationChannelId: String
    abstract val notificationChannelTitleKey: String
    abstract val notificationChannelDefaultTitle: String
    abstract val pendingIntent: PendingIntent
    abstract val notificationTitleKey: String
    abstract val notificationBodyKey: String
    abstract val notificationId: Int

    override suspend fun doWork(): Result {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (applicationContext.stringsManager().strings.isEmpty()) {
            applicationContext.stringsManager().initialize(applicationContext)
        }
        val strings = applicationContext.stringsManager().strings

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                strings[notificationChannelTitleKey] ?: notificationChannelDefaultTitle,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(
            applicationContext,
            notificationChannelId
        )
            .setContentTitle(strings[notificationTitleKey])
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_notification_bar)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(strings[notificationBodyKey])
            )
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(notificationId, notification)

        return Result.success()
    }
}