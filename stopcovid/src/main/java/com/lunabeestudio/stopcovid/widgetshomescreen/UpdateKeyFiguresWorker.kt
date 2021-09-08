package com.lunabeestudio.stopcovid.widgetshomescreen

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lunabeestudio.stopcovid.Constants
import com.lunabeestudio.stopcovid.extension.keyFiguresManager
import java.util.concurrent.TimeUnit

class UpdateKeyFiguresWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        applicationContext.keyFiguresManager().onAppForeground(applicationContext)
        return Result.success()
    }

    companion object {

        fun scheduleWorker(context: Context) {
            val constraints: Constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWorkRequest: PeriodicWorkRequest =
                PeriodicWorkRequestBuilder<UpdateKeyFiguresWorker>(
                    Constants.HomeScreenWidget.WORKER_UPDATE_FIGURES_PERIODIC_REFRESH_HOURS,
                    TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .setInitialDelay(Constants.HomeScreenWidget.WORKER_UPDATE_FIGURES_PERIODIC_REFRESH_HOURS, TimeUnit.HOURS)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    Constants.HomeScreenWidget.WORKER_UPDATE_FIGURES_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWorkRequest
                )
        }
    }
}
