/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.robert

import android.content.Context
import android.util.Base64
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lunabeestudio.domain.RobertConstant
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.HelloBuilder
import com.lunabeestudio.domain.model.HelloSettings
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.domain.model.SSUBuilder
import com.lunabeestudio.domain.model.SSUSettings
import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.robert.datasource.LocalEphemeralBluetoothIdentifierDataSource
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.datasource.LocalLocalProximityDataSource
import com.lunabeestudio.robert.datasource.RemoteServiceDataSource
import com.lunabeestudio.robert.extension.use
import com.lunabeestudio.robert.model.NoEphemeralBluetoothIdentifierFound
import com.lunabeestudio.robert.model.NoEphemeralBluetoothIdentifierFoundForEpoch
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.model.RobertUnknownException
import com.lunabeestudio.robert.model.UnknownException
import com.lunabeestudio.robert.repository.EphemeralBluetoothIdentifierRepository
import com.lunabeestudio.robert.repository.KeystoreRepository
import com.lunabeestudio.robert.repository.LocalProximityRepository
import com.lunabeestudio.robert.repository.RemoteServiceRepository
import com.lunabeestudio.robert.worker.StatusWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

class RobertManagerImpl(
    application: RobertApplication,
    localEphemeralBluetoothIdentifierDataSource: LocalEphemeralBluetoothIdentifierDataSource,
    localKeystoreDataSource: LocalKeystoreDataSource,
    localLocalProximityDataSource: LocalLocalProximityDataSource,
    serviceDataSource: RemoteServiceDataSource
) : RobertManager {
    private val ephemeralBluetoothIdentifierRepository: EphemeralBluetoothIdentifierRepository =
        EphemeralBluetoothIdentifierRepository(localEphemeralBluetoothIdentifierDataSource)
    private val keystoreRepository: KeystoreRepository =
        KeystoreRepository(localKeystoreDataSource)
    private val localProximityRepository: LocalProximityRepository =
        LocalProximityRepository(localLocalProximityDataSource)
    private val remoteServiceRepository: RemoteServiceRepository =
        RemoteServiceRepository(serviceDataSource)

    init {
        if (isRegistered) {
            startStatusWorker(application.getAppContext())
        }
    }

    override val isRegistered: Boolean
        get() = keystoreRepository.sharedKey != null

    override val isProximityActive: Boolean
        get() = keystoreRepository.proximityActive ?: false

    override val isAtRisk: Boolean
        get() = keystoreRepository.atRisk ?: false

    override val lastExposureTimeframe: Int
        get() = keystoreRepository.lastExposureTimeframe ?: 0

    override val isSick: Boolean
        get() = keystoreRepository.isSick ?: false

    override suspend fun register(application: RobertApplication, captcha: String): RobertResult {
        ephemeralBluetoothIdentifierRepository.removeAll()
        keystoreRepository.sharedKey = null
        keystoreRepository.timeStart = null

        val result = remoteServiceRepository.register(captcha)
        return when (result) {
            is RobertResultData.Success -> {
                ephemeralBluetoothIdentifierRepository.save(*result.data.ephemeralBluetoothIdentifierList.toTypedArray())
                keystoreRepository.sharedKey = Base64.decode(result.data.key, Base64.NO_WRAP)
                keystoreRepository.timeStart = result.data.timeStart
                keystoreRepository.filteringInfo = result.data.filterings
                startStatusWorker(application.getAppContext())
                activateProximity(application)
                RobertResult.Success()
            }
            is RobertResultData.Failure -> RobertResult.Failure(result.error)
        }
    }

    override suspend fun activateProximity(application: RobertApplication, statusTried: Boolean): RobertResult {
        return when (val helloBuilder = getCurrentHelloBuilder()) {
            is RobertResultData.Success -> {
                keystoreRepository.proximityActive = true
                application.refreshProximityService()
                RobertResult.Success()
            }
            is RobertResultData.Failure -> {
                if (statusTried) {
                    RobertResult.Failure(helloBuilder.error)
                } else {
                    when (val status = updateStatus()) {
                        is RobertResult.Success -> {
                            activateProximity(application, true)
                        }
                        is RobertResult.Failure -> {
                            RobertResult.Failure(status.error)
                        }
                    }
                }
            }
        }
    }

    override fun deactivateProximity(application: RobertApplication) {
        keystoreRepository.proximityActive = false
        application.refreshProximityService()
    }

    override suspend fun updateStatus(): RobertResult {
        val ssu = getSSU(RobertConstant.PREFIX.C2)
        val timeStart = keystoreRepository.timeStart
        return if (ssu is RobertResultData.Success && timeStart != null) {
            val result = remoteServiceRepository.status(ssu.data, timeStart)

            when (result) {
                is RobertResultData.Success -> {
                    ephemeralBluetoothIdentifierRepository.save(*result.data.ephemeralBluetoothIdentifierList.toTypedArray())
                    keystoreRepository.atRisk = result.data.atRisk
                    keystoreRepository.lastExposureTimeframe = result.data.lastExposureTimeframe
                    RobertResult.Success()
                }
                is RobertResultData.Failure -> RobertResult.Failure(result.error)
            }
        } else {
            Timber.e("hello or timeStart not found")
            RobertResult.Failure(UnknownException())
        }
    }

    override fun clearOldData() {
        Timber.d("clear old data")
        val ephemeralBluetoothIdentifierExpiredTime = System.currentTimeMillis().unixTimeMsToNtpTimeS()
        ephemeralBluetoothIdentifierRepository.removeUntilTimeKeepLast(ephemeralBluetoothIdentifierExpiredTime)
        val localProximityExpiredTime: Long =
            (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(Constant.DAYS_TO_ERASE_AFTER)).unixTimeMsToNtpTimeS()
        localProximityRepository.removeUntilTime(localProximityExpiredTime)
    }

    override suspend fun report(token: String, application: RobertApplication): RobertResult {
        val result = remoteServiceRepository.report(token, localProximityRepository.getAll())
        return when (result) {
            is RobertResult.Success -> {
                val ssu = getSSU(RobertConstant.PREFIX.C3)
                if (ssu is RobertResultData.Success) {
                    remoteServiceRepository.unregister(ssu.data)
                }
                cleanLocalData(application)
                keystoreRepository.isSick = true
                result
            }
            else -> result
        }
    }

    override suspend fun storeLocalProximity(vararg localProximity: LocalProximity) {
        localProximityRepository.save(*localProximity)
    }

    private fun getSSU(prefix: Byte): RobertResultData<ServerStatusUpdate> {
        val ephemeralBluetoothIdentifier = ephemeralBluetoothIdentifierRepository.getForTime()
            ?: ephemeralBluetoothIdentifierRepository.getAll().lastOrNull()

        return if (ephemeralBluetoothIdentifier != null) {
            keystoreRepository.sharedKey?.use { key ->
                val ssuBuilder = SSUBuilder(SSUSettings(prefix = prefix), ephemeralBluetoothIdentifier, key)
                RobertResultData.Success(ssuBuilder.build())
            } ?: RobertResultData.Failure(RobertUnknownException())
        } else {
            RobertResultData.Failure(NoEphemeralBluetoothIdentifierFound())
        }
    }

    override fun getCurrentHelloBuilder(): RobertResultData<HelloBuilder> {
        val ephemeralBluetoothIdentifier = ephemeralBluetoothIdentifierRepository.getForTime()

        return if (ephemeralBluetoothIdentifier != null) {
            keystoreRepository.sharedKey?.use { key ->
                val helloBuilder = HelloBuilder(HelloSettings(), ephemeralBluetoothIdentifier, key)
                RobertResultData.Success(helloBuilder)
            } ?: RobertResultData.Failure(RobertUnknownException())
        } else {
            RobertResultData.Failure(NoEphemeralBluetoothIdentifierFoundForEpoch())
        }
    }

    override suspend fun eraseLocalHistory(): RobertResult {
        localProximityRepository.removeAll()
        return RobertResult.Success()
    }

    override suspend fun eraseRemoteExposureHistory(): RobertResult {
        val ssu = getSSU(RobertConstant.PREFIX.C4)
        return when (ssu) {
            is RobertResultData.Success -> {
                val result = remoteServiceRepository.deleteExposureHistory(ssu.data)
                when (result) {
                    is RobertResult.Success -> result
                    is RobertResult.Failure -> RobertResult.Failure(result.error)
                }
            }
            is RobertResultData.Failure -> {
                RobertResult.Failure(ssu.error)
            }
        }
    }

    override suspend fun eraseRemoteAlert(): RobertResult {
        return remoteServiceRepository.eraseRemoteAlert()
    }

    override suspend fun quitStopCovid(application: RobertApplication): RobertResult {
        val ssu = getSSU(RobertConstant.PREFIX.C3)
        return when (ssu) {
            is RobertResultData.Success -> {
                val result = remoteServiceRepository.unregister(ssu.data)
                when (result) {
                    is RobertResult.Success -> {
                        cleanLocalData(application)
                        RobertResult.Success()
                    }
                    is RobertResult.Failure -> RobertResult.Failure(result.error)
                }
            }
            is RobertResultData.Failure -> {
                RobertResult.Failure(ssu.error)
            }
        }
    }

    private fun cleanLocalData(application: RobertApplication) {
        stopStatusWorker(application.getAppContext())
        deactivateProximity(application)
        ephemeralBluetoothIdentifierRepository.removeAll()
        localProximityRepository.removeAll()
        keystoreRepository.sharedKey = null
        keystoreRepository.timeStart = null
        keystoreRepository.atRisk = null
        keystoreRepository.lastExposureTimeframe = null
        keystoreRepository.proximityActive = null
        keystoreRepository.isSick = null
    }

    private fun startStatusWorker(context: Context) {
        Timber.d("Create worker status")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val statusWorkRequest = PeriodicWorkRequestBuilder<StatusWorker>(1L, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(Constant.STATUS_WORKER_NAME, ExistingPeriodicWorkPolicy.KEEP, statusWorkRequest)
    }

    private fun stopStatusWorker(context: Context) {
        Timber.d("Stop worker status")
        WorkManager.getInstance(context).cancelUniqueWork(Constant.STATUS_WORKER_NAME)
    }
}
