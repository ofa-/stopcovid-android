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

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.manager.AppMaintenanceManager

class MaintenanceWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        AppMaintenanceManager.checkForMaintenanceUpgrade(
            applicationContext,
            (applicationContext as StopCovid).injectionContainer.serverManager.okHttpClient
        )
        return Result.success()
    }
}