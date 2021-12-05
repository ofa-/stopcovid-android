/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/4/10 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Operation
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.StopCovid
import java.util.concurrent.TimeUnit

class SmartWalletNotificationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        (applicationContext as StopCovid).injectionContainer.smartWalletNotificationUseCase(applicationContext)
        return Result.success()
    }

    companion object {
        fun start(
            context: Context,
        ): Operation {
            val workRequest = PeriodicWorkRequestBuilder<SmartWalletNotificationWorker>(
                Constants.WorkerPeriodicTime.SMART_WALLET_NOTIFICATION_HOURS,
                TimeUnit.HOURS
            )
                .addTag(Constants.WorkerTags.SMART_WALLET)
                .build()

            return WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(Constants.WorkerNames.SMART_WALLET_NOTIFICATION, ExistingPeriodicWorkPolicy.KEEP, workRequest)
        }
    }
}