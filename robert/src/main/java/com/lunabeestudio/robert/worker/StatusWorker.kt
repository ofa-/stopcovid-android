/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert.worker

import android.content.Context
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
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.random.Random

internal class StatusWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val robertApplication: RobertApplication = applicationContext as RobertApplication
        val robertManager: RobertManager = robertApplication.robertManager

        Timber.d("Start updating status")
        val wasAtRisk = robertManager.isAtRisk
        val result = robertManager.updateStatus(robertApplication)

        if (!wasAtRisk && robertManager.isAtRisk) {
            (applicationContext as RobertApplication).atRiskDetected()
        }

        Timber.d("Clear old ebids & local proximities")
        robertManager.clearOldData()

        updateStatusWorker(robertManager)

        return when (result) {
            is RobertResult.Success -> {
                Timber.d("Update status success")
                Result.success()
            }
            is RobertResult.Failure -> Result.retry()
        }
    }

    private fun updateStatusWorker(robertManager: RobertManager) {
        Timber.d("Update worker status to new random date")
        val minDelay: Long = robertManager.checkStatusFrequencyHour.toLong()
        val maxDelay = minDelay + robertManager.randomStatusHour * 60 * 60
        val randomDelaySec: Long = Random.nextLong(minDelay, maxDelay)
        scheduleStatusWorker(applicationContext, minDelay, randomDelaySec)
    }

    companion object {
        fun scheduleStatusWorker(context: Context, minDelaySec: Long, randomDelaySec: Long) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            Timber.d("Add random delay of ${randomDelaySec}sec")

            val statusWorkRequest = PeriodicWorkRequestBuilder<StatusWorker>(minDelaySec, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .setInitialDelay(randomDelaySec, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(RobertConstant.STATUS_WORKER_NAME, ExistingPeriodicWorkPolicy.REPLACE, statusWorkRequest)
        }
    }
}