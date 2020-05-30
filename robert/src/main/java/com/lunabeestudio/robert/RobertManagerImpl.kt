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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.DeviceParameterCorrection
import com.lunabeestudio.domain.model.HelloBuilder
import com.lunabeestudio.domain.model.HelloSettings
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.domain.model.RegisterReport
import com.lunabeestudio.domain.model.SSUBuilder
import com.lunabeestudio.domain.model.SSUSettings
import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.robert.datasource.ConfigurationDataSource
import com.lunabeestudio.robert.datasource.LocalEphemeralBluetoothIdentifierDataSource
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.datasource.LocalLocalProximityDataSource
import com.lunabeestudio.robert.datasource.RemoteServiceDataSource
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
import com.lunabeestudio.robert.extension.use
import com.lunabeestudio.robert.model.NoEphemeralBluetoothIdentifierFound
import com.lunabeestudio.robert.model.NoEphemeralBluetoothIdentifierFoundForEpoch
import com.lunabeestudio.robert.model.NoKeyException
import com.lunabeestudio.robert.model.RobertException
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
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class RobertManagerImpl(
    application: RobertApplication,
    localEphemeralBluetoothIdentifierDataSource: LocalEphemeralBluetoothIdentifierDataSource,
    localKeystoreDataSource: LocalKeystoreDataSource,
    localLocalProximityDataSource: LocalLocalProximityDataSource,
    serviceDataSource: RemoteServiceDataSource,
    sharedCryptoDataSource: SharedCryptoDataSource,
    configurationDataSource: ConfigurationDataSource
) : RobertManager {
    private val ephemeralBluetoothIdentifierRepository: EphemeralBluetoothIdentifierRepository =
        EphemeralBluetoothIdentifierRepository(localEphemeralBluetoothIdentifierDataSource, sharedCryptoDataSource, localKeystoreDataSource)
    private val keystoreRepository: KeystoreRepository =
        KeystoreRepository(localKeystoreDataSource)
    private val localProximityRepository: LocalProximityRepository =
        LocalProximityRepository(localLocalProximityDataSource)
    private val remoteServiceRepository: RemoteServiceRepository =
        RemoteServiceRepository(serviceDataSource, sharedCryptoDataSource, localKeystoreDataSource, configurationDataSource)

    init {
        if (isRegistered) {
            startStatusWorker(application.getAppContext())
        }
    }

    override val isRegistered: Boolean
        get() = keystoreRepository.kA != null && keystoreRepository.kEA != null

    override val isProximityActive: Boolean
        get() = keystoreRepository.proximityActive ?: false

    override val isAtRisk: Boolean
        get() = keystoreRepository.atRisk ?: false

    override val atRiskLastRefresh: Long?
        get() = keystoreRepository.atRiskLastRefresh

    override val atRiskMinHourContactNotif: Int
        get() = keystoreRepository.atRiskMinHourContactNotif ?: RobertConstant.MIN_HOUR_CONTACT_NOTIF

    override val atRiskMaxHourContactNotif: Int
        get() = keystoreRepository.atRiskMaxHourContactNotif ?: RobertConstant.MAX_HOUR_CONTACT_NOTIF

    override val lastExposureTimeframe: Int
        get() = keystoreRepository.lastExposureTimeframe ?: 0

    override val quarantinePeriod: Int
        get() = keystoreRepository.quarantinePeriod ?: RobertConstant.QUARANTINE_PERIOD

    override val isSick: Boolean
        get() = keystoreRepository.isSick ?: false

    override val calibration: List<DeviceParameterCorrection>
        get() = keystoreRepository.calibration ?: emptyList()

    override val serviceUUID: String
        get() = keystoreRepository.serviceUUID ?: RobertConstant.BLE_SERVICE_UUID

    override val characteristicUUID: String
        get() = keystoreRepository.characteristicUUID ?: RobertConstant.BLE_CHARACTERISTIC_UUID

    override val backgroundServiceManufacturerData: String
        get() = keystoreRepository.backgroundServiceManufacturerData ?: RobertConstant.BLE_BACKGROUND_SERVICE_MANUFACTURER_DATA_IOS

    override suspend fun register(application: RobertApplication, captcha: String): RobertResult {
        val result = remoteServiceRepository.register(captcha)
        return when (result) {
            is RobertResultData.Success -> {
                if (result.data.configuration.isNullOrEmpty()) {
                    val configResult = remoteServiceRepository.fetchConfig(application.getAppContext())
                    when (configResult) {
                        is RobertResultData.Success -> {
                            if (configResult.data.isNullOrEmpty()) {
                                RobertResult.Failure(RobertUnknownException())
                            } else {
                                finishRegister(application, result.data, configResult.data)
                            }
                        }
                        is RobertResultData.Failure -> {
                            clearLocalData(application)
                            RobertResult.Failure(configResult.error)
                        }
                    }
                } else {
                    finishRegister(application, result.data, result.data.configuration)
                }
            }
            is RobertResultData.Failure -> RobertResult.Failure(result.error)
        }
    }

    private suspend fun finishRegister(application: RobertApplication,
        registerReport: RegisterReport,
        config: List<Configuration>?): RobertResult {
        return try {
            handleConfigChange(config)
            keystoreRepository.timeStart = registerReport.timeStart
            ephemeralBluetoothIdentifierRepository.save(Base64.decode(registerReport.tuples, Base64.NO_WRAP))
            startStatusWorker(application.getAppContext())
            activateProximity(application)
            RobertResult.Success()
        } catch (e: Exception) {
            clearLocalData(application)
            if (e is RobertException) {
                RobertResult.Failure(e)
            } else {
                RobertResult.Failure(RobertUnknownException())
            }
        }
    }

    private fun handleConfigChange(configuration: List<Configuration>?) {
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.CALIBRATION
        }?.value as? List<*>)?.let { calibrations ->
            val gson = Gson()
            val typeToken = object : TypeToken<List<DeviceParameterCorrection>>() {}.type
            keystoreRepository.calibration = gson.fromJson(gson.toJson(calibrations), typeToken)
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.SERVICE_UUID
        }?.value as? String)?.let { serviceUUID ->
            keystoreRepository.serviceUUID = serviceUUID
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.CHARACTERISTIC_UUID
        }?.value as? String)?.let { characteristicUUID ->
            keystoreRepository.characteristicUUID = characteristicUUID
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.BACKGROUND_SERVICE_MANUFACTURER_DATA
        }?.value as? String)?.let { backgroundServiceManufacturerData ->
            keystoreRepository.backgroundServiceManufacturerData = backgroundServiceManufacturerData
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.DATA_RETENTION_PERIOD
        }?.value as? Number)?.let { dataRetentionPeriod ->
            keystoreRepository.dataRetentionPeriod = dataRetentionPeriod.toInt()
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.QUARANTINE_PERIOD
        }?.value as? Number)?.let { quarantinePeriod ->
            keystoreRepository.quarantinePeriod = quarantinePeriod.toInt()
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.CHECK_STATUS_FREQUENCY
        }?.value as? Number)?.let { checkStatusFrequency ->
            keystoreRepository.checkStatusFrequency = checkStatusFrequency.toInt()
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.RANDOM_STATUS_HOUR
        }?.value as? Number)?.let { randomStatusHour ->
            keystoreRepository.randomStatusHour = randomStatusHour.toInt()
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.PRE_SYMPTOMS_SPAN
        }?.value as? Number)?.let { preSymptomsSpan ->
            keystoreRepository.preSymptomsSpan = preSymptomsSpan.toInt()
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.APP_AVAILABILITY
        }?.value as? Boolean)?.let { appAvailability ->
            keystoreRepository.appAvailability = appAvailability
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.MIN_HOUR_CONTACT_NOTIF
        }?.value as? Number)?.let { minHourContactNotif ->
            keystoreRepository.atRiskMinHourContactNotif = minHourContactNotif.toInt()
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.MAX_HOUR_CONTACT_NOTIF
        }?.value as? Number)?.let { maxHourContactNotif ->
            keystoreRepository.atRiskMaxHourContactNotif = maxHourContactNotif.toInt()
        }
    }

    override suspend fun activateProximity(application: RobertApplication, statusTried: Boolean): RobertResult {
        val isHelloAvailable = ephemeralBluetoothIdentifierRepository.getForTime() != null
        return when {
            isHelloAvailable -> {
                keystoreRepository.proximityActive = true
                application.refreshProximityService()
                RobertResult.Success()
            }
            statusTried -> {
                RobertResult.Failure(NoEphemeralBluetoothIdentifierFoundForEpoch())
            }
            else -> {
                when (val status = updateStatus(application)) {
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

    override fun deactivateProximity(application: RobertApplication) {
        keystoreRepository.proximityActive = false
        application.refreshProximityService()
    }

    override suspend fun updateStatus(robertApplication: RobertApplication): RobertResult {
        val ssu = getSSU(RobertConstant.PREFIX.C2)
        return if (ssu is RobertResultData.Success) {
            val result = remoteServiceRepository.status(ssu.data)

            when (result) {
                is RobertResultData.Success -> {
                    try {
                        if (result.data.config.isNullOrEmpty()) {
                            val configResult = remoteServiceRepository.fetchConfig(robertApplication.getAppContext())
                            handleConfigChange((configResult as? RobertResultData.Success)?.data)
                        } else {
                            handleConfigChange(result.data.config)
                        }
                        ephemeralBluetoothIdentifierRepository.save(Base64.decode(result.data.tuples, Base64.NO_WRAP))
                        keystoreRepository.atRisk = result.data.atRisk
                        keystoreRepository.atRiskLastRefresh = Date().time
                        keystoreRepository.lastExposureTimeframe = result.data.lastExposureTimeframe
                        RobertResult.Success()
                    } catch (e: Exception) {
                        when (e) {
                            is RobertException -> RobertResult.Failure(e)
                            else -> RobertResult.Failure(RobertUnknownException())
                        }
                    }
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
            (System.currentTimeMillis() - TimeUnit.DAYS.toMillis((keystoreRepository.dataRetentionPeriod
                ?: RobertConstant.DATA_RETENTION_PERIOD).toLong())).unixTimeMsToNtpTimeS()
        localProximityRepository.removeUntilTime(localProximityExpiredTime)
    }

    override suspend fun report(token: String, firstSymptoms: Int, application: RobertApplication): RobertResult {
        val firstProximityToSendTime = (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(firstSymptoms.toLong()) - TimeUnit.DAYS.toMillis(
            (keystoreRepository.preSymptomsSpan ?: RobertConstant.PRE_SYMPTOMS_SPAN).toLong())).unixTimeMsToNtpTimeS()
        val result = remoteServiceRepository.report(token, localProximityRepository.getUntilTime(firstProximityToSendTime))
        return when (result) {
            is RobertResult.Success -> {
                val ssu = getSSU(RobertConstant.PREFIX.C3)
                if (ssu is RobertResultData.Success) {
                    remoteServiceRepository.unregister(ssu.data)
                }
                clearLocalData(application)
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
            keystoreRepository.kA?.use { key ->
                val ssuBuilder = SSUBuilder(SSUSettings(prefix = prefix), ephemeralBluetoothIdentifier, key)
                RobertResultData.Success(ssuBuilder.build())
            } ?: RobertResultData.Failure(NoKeyException("Failed to retrieve kA"))
        } else {
            RobertResultData.Failure(NoEphemeralBluetoothIdentifierFound())
        }
    }

    override fun getCurrentHelloBuilder(): RobertResultData<HelloBuilder> {
        val ephemeralBluetoothIdentifier = ephemeralBluetoothIdentifierRepository.getForTime()

        return if (ephemeralBluetoothIdentifier != null) {
            keystoreRepository.kA?.use { key ->
                val helloBuilder = HelloBuilder(HelloSettings(prefix = RobertConstant.PREFIX.C1), ephemeralBluetoothIdentifier, key)
                RobertResultData.Success(helloBuilder)
            } ?: RobertResultData.Failure(NoKeyException("Failed to retrieve kA"))
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
        keystoreRepository.atRisk = null
        keystoreRepository.lastExposureTimeframe = null
        return RobertResult.Success()
    }

    override suspend fun quitStopCovid(application: RobertApplication): RobertResult {
        val ssu = getSSU(RobertConstant.PREFIX.C3)
        return when (ssu) {
            is RobertResultData.Success -> {
                val result = remoteServiceRepository.unregister(ssu.data)
                when (result) {
                    is RobertResult.Success -> {
                        clearLocalData(application)
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

    override fun clearLocalData(application: RobertApplication) {
        stopStatusWorker(application.getAppContext())
        deactivateProximity(application)
        ephemeralBluetoothIdentifierRepository.removeAll()
        localProximityRepository.removeAll()
        keystoreRepository.kA = null
        keystoreRepository.kEA = null
        keystoreRepository.timeStart = null
        keystoreRepository.atRisk = null
        keystoreRepository.atRiskLastRefresh = null
        keystoreRepository.lastExposureTimeframe = null
        keystoreRepository.proximityActive = null
        keystoreRepository.isSick = null
    }

    private fun startStatusWorker(context: Context) {
        Timber.d("Create worker status")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val minDelay: Long = (keystoreRepository.checkStatusFrequency ?: RobertConstant.CHECK_STATUS_FREQUENCY_HOURS).toLong()
        val maxDelay = minDelay + (keystoreRepository.randomStatusHour?.toLong() ?: RobertConstant.RANDOM_STATUS_SEC) * 60 * 60
        val randomDelaySec: Long = Random.nextLong(minDelay, maxDelay)

        Timber.d("Add random delay of ${randomDelaySec}sec")

        val statusWorkRequest = PeriodicWorkRequestBuilder<StatusWorker>(minDelay, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(randomDelaySec, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(RobertConstant.STATUS_WORKER_NAME, ExistingPeriodicWorkPolicy.KEEP, statusWorkRequest)
    }

    private fun stopStatusWorker(context: Context) {
        Timber.d("Stop worker status")
        WorkManager.getInstance(context).cancelUniqueWork(RobertConstant.STATUS_WORKER_NAME)
    }
}
