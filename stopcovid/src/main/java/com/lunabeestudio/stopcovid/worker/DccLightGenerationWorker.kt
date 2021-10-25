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
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.lunabeestudio.robert.extension.safeEnumValueOf
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.StopCovid
import com.lunabeestudio.stopcovid.model.TacResult
import com.lunabeestudio.stopcovid.usecase.GenerateActivityPassStateName
import kotlinx.coroutines.flow.collect
import timber.log.Timber

class DccLightGenerationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val certificateId = inputData.getString(CERTIFICATE_ID_WORKER_INPUT_DATA_KEY) ?: return Result.failure()
        val injectionContainer = (applicationContext as StopCovid).injectionContainer

        val validDccLightsCount =
            injectionContainer.walletRepository.countValidActivityPassForCertificate(certificateId, System.currentTimeMillis())

        if (validDccLightsCount > injectionContainer.robertManager.configuration.renewThreshold) {
            Timber.i("Threshold not reached, abort dcc light generation for certificate $certificateId")
            return Result.success()
        }

        val generateActivityPassUseCase = injectionContainer.generateActivityPassUseCase

        var workerResult: Result = Result.failure()

        generateActivityPassUseCase(certificateId).collect { result ->
            when (result) {
                is TacResult.Failure -> workerResult = Result.failure()
                is TacResult.Success -> workerResult = Result.success()
                is TacResult.Loading -> {
                    setProgress(
                        workDataOf(
                            GENERATION_STATE_WORKER_OUTPUT_DATA_KEY to result.partialData?.name?.name
                        )
                    )
                }
            }
        }

        return workerResult
    }

    companion object {
        private const val CERTIFICATE_ID_WORKER_INPUT_DATA_KEY: String = "CERTIFICATE_ID_WORKER_INPUT_DATA_KEY"
        private const val GENERATION_STATE_WORKER_OUTPUT_DATA_KEY: String = "GENERATION_STATE_WORKER_OUTPUT_DATA_KEY"

        fun startDccLightGenerationWorker(context: Context, certificateId: String): LiveData<WorkInfo?> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<DccLightGenerationWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        CERTIFICATE_ID_WORKER_INPUT_DATA_KEY to certificateId,
                    )
                )
                .addTag(Constants.WorkerTags.DCC_LIGHT)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(getWorkerName(certificateId), ExistingWorkPolicy.KEEP, workRequest)

            return getInfo(context, certificateId)
        }

        fun cancel(context: Context, certificateId: String) {
            WorkManager.getInstance(context).cancelUniqueWork(getWorkerName(certificateId))
        }

        fun getInfo(context: Context, certificateId: String): LiveData<WorkInfo?> =
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(getWorkerName(certificateId)).map {
                it.firstOrNull()
            }

        private fun getWorkerName(certificateId: String) = "${Constants.WorkerNames.DCC_LIGHT_GENERATION}-$certificateId"

        fun getProgressState(info: WorkInfo): GenerateActivityPassStateName? {
            return info.progress.getString(GENERATION_STATE_WORKER_OUTPUT_DATA_KEY)?.let {
                safeEnumValueOf<GenerateActivityPassStateName>(it)
            }
        }
    }
}