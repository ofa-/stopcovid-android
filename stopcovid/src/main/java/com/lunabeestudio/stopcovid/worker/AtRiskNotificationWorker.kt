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

class AtRiskNotificationWorker(context: Context, workerParams: WorkerParameters) : NotificationWorker(context, workerParams) {
    override val notificationChannelId: String = UiConstants.Notification.AT_RISK.channelId
    override val notificationChannelTitleKey: String = "notification.channel.atRisk.title"
    override val notificationChannelDefaultTitle: String = "At risk"
    override val pendingIntent: PendingIntent = NavDeepLinkBuilder(applicationContext)
        .setComponentName(MainActivity::class.java)
        .setGraph(R.navigation.nav_main)
        .setDestination(R.id.proximityFragment)
        .createPendingIntent()
    override val notificationTitleKey: String = inputData.getString(INPUT_DATA_TITLE_KEY) ?: ""
    override val notificationBodyKey: String = inputData.getString(INPUT_DATA_MESSAGE_KEY) ?: ""
    override val notificationId: Int = UiConstants.Notification.AT_RISK.notificationId

    companion object {
        const val INPUT_DATA_TITLE_KEY: String = "Input.Data.Title.Key"
        const val INPUT_DATA_MESSAGE_KEY: String = "Input.Data.Message.Key"
    }
}
