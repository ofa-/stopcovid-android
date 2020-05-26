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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lunabeestudio.robert.RobertApplication
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.model.RobertResult
import timber.log.Timber

class StatusWorker(context: Context, workerParams: WorkerParameters)
    : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val robertManager: RobertManager = (applicationContext as RobertApplication).robertManager

        Timber.d("Start updating status")
        val wasAtRisk = robertManager.isAtRisk
        val result = robertManager.updateStatus()

        if (!wasAtRisk && robertManager.isAtRisk) {
            (applicationContext as RobertApplication).atRiskDetected()
        }

        return when (result) {
            is RobertResult.Success -> {
                Timber.d("Update status success")
                robertManager.clearOldData()
                Result.success()
            }
            is RobertResult.Failure -> Result.retry()
        }
    }
}