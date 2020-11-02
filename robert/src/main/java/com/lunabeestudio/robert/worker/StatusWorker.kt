/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertConstant
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.TimeNotAlignedException
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import kotlin.random.Random

internal class StatusWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val robertApplication: RobertApplication = applicationContext as RobertApplication
        val robertManager: RobertManager = robertApplication.robertManager

        robertApplication.refreshInfoCenter()

        Timber.v("Start updating status")
        val result = robertManager.updateStatus(robertApplication)

        Timber.v("Clear old ebids & local proximities")
        robertManager.clearOldData()

        return when (result) {
            is RobertResult.Success -> {
                Timber.v("Update status success")
                Result.success()
            }
            is RobertResult.Failure -> {
                Timber.e(result.error, "Update status failed")
                if (result.error is TimeNotAlignedException) {
                    (applicationContext as RobertApplication).sendClockNotAlignedNotification()
                }
                Result.retry()
            }
        }
    }

    companion object {
        fun scheduleStatusWorker(context: Context, robertManager: RobertManager, policy: ExistingPeriodicWorkPolicy) {
            val minDelaySec: Long = (robertManager.checkStatusFrequencyHour * 3600).roundToLong()
            val maxDelaySec: Long = minDelaySec + (robertManager.randomStatusHour * 3600).roundToLong()
            val randomDelaySec: Long = if (minDelaySec != maxDelaySec) {
                Random.nextLong(minDelaySec, maxDelaySec)
            } else {
                minDelaySec
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            Timber.v("Add random delay of ${randomDelaySec}sec")

            val statusWorkRequest = PeriodicWorkRequestBuilder<StatusWorker>(minDelaySec, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .setInitialDelay(randomDelaySec, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1L, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(RobertConstant.STATUS_WORKER_NAME, policy, statusWorkRequest)
        }
    }
}