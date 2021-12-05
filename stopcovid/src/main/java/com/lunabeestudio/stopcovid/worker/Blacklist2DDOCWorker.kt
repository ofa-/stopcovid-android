/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2021/10/03 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.stopcovid.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.StopCovid

class Blacklist2DDOCWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val error = (applicationContext as StopCovid).injectionContainer.blacklist2DDOCManager.fetchNewIterations()
        return if (error == null) {
            Result.success()
        } else {
            Result.failure()
        }
    }

    companion object {
        fun start(context: Context): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<Blacklist2DDOCWorker>()
                .addTag(Constants.WorkerTags.BLACKLIST)
                .setConstraints(constraints)
                .build()

            return WorkManager.getInstance(context)
                .enqueueUniqueWork(Constants.WorkerNames.BLACKLIST_2DDOC, ExistingWorkPolicy.KEEP, workRequest)
        }
    }
}