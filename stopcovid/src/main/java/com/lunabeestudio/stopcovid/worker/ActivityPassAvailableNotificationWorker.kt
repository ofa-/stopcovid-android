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
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.R
import com.lunabeestudio.stopcovid.activity.MainActivity
import com.lunabeestudio.stopcovid.coreui.UiConstants
import com.lunabeestudio.stopcovid.fragment.WalletContainerFragmentArgs
import java.util.concurrent.TimeUnit

class ActivityPassAvailableNotificationWorker(
    context: Context,
    workerParams: WorkerParameters,
) : NotificationWorker(
    context,
    workerParams
) {
    override val notificationChannelId: String = UiConstants.Notification.ACTIVITY_PASS_REMINDER.channelId
    override val notificationChannelTitleKey: String = "notification.channel.activityPass.title"
    override val notificationChannelDefaultTitle: String = "Ephemeral pass"

    override val pendingIntent: PendingIntent = NavDeepLinkBuilder(applicationContext)
        .setComponentName(MainActivity::class.java)
        .setGraph(R.navigation.nav_main)
        .setDestination(R.id.nav_wallet)
        .setArguments(WalletContainerFragmentArgs(navCertificateId = inputData.getString(INPUT_DATA_CERTIFICATE_ID_KEY)).toBundle())
        .createPendingIntent()
    override val notificationTitleKey: String = "activityPass.notification.title"
    override val notificationBodyKey: String = "activityPass.notification.message"
    override val notificationId: Int = UiConstants.Notification.ACTIVITY_PASS_REMINDER.notificationId

    companion object {
        private const val INPUT_DATA_CERTIFICATE_ID_KEY: String = "Input.Data.Certificate.Id.Key"

        fun triggerActivityPassAvailableNotificationWorker(context: Context, navCertificateId: String, startTimeMs: Long) {
            val inputData = Data.Builder()
                .putString(INPUT_DATA_CERTIFICATE_ID_KEY, navCertificateId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ActivityPassAvailableNotificationWorker>().setInputData(inputData)
                .setInitialDelay(startTimeMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(Constants.WorkerNames.DCC_LIGHT_AVAILABLE, ExistingWorkPolicy.REPLACE, workRequest)
        }
    }
}