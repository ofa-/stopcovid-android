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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.StopCovid

class DccLightRenewCleanWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        (applicationContext as StopCovid).injectionContainer.cleanAndRenewActivityPassUseCase()
        return Result.success()
    }

    companion object {
        fun startDccLightCleanAndRenewWorker(context: Context): Operation {
            val workRequest = OneTimeWorkRequestBuilder<DccLightRenewCleanWorker>()
                .addTag(Constants.WorkerTags.DCC_LIGHT)
                .build()

            return WorkManager.getInstance(context)
                .enqueueUniqueWork(Constants.WorkerNames.DCC_LIGHT_RENEW_CLEAN, ExistingWorkPolicy.REPLACE, workRequest)
        }
    }
}