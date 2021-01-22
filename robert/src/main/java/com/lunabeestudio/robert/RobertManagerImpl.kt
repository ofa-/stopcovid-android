/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.robert

import android.content.Context
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.HelloBuilder
import com.lunabeestudio.domain.model.HelloSettings
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.domain.model.RegisterReport
import com.lunabeestudio.domain.model.SSUBuilder
import com.lunabeestudio.domain.model.SSUSettings
import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.robert.datasource.ConfigurationDataSource
import com.lunabeestudio.robert.datasource.LocalEphemeralBluetoothIdentifierDataSource
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.datasource.LocalLocalProximityDataSource
import com.lunabeestudio.robert.datasource.RemoteServiceDataSource
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
import com.lunabeestudio.robert.extension.safeEnumValueOf
import com.lunabeestudio.robert.extension.toAtRiskStatus
import com.lunabeestudio.robert.extension.use
import com.lunabeestudio.robert.manager.LocalProximityFilter
import com.lunabeestudio.robert.model.AtRiskStatus
import com.lunabeestudio.robert.model.ForbiddenException
import com.lunabeestudio.robert.model.NoEphemeralBluetoothIdentifierFound
import com.lunabeestudio.robert.model.NoEphemeralBluetoothIdentifierFoundForEpoch
import com.lunabeestudio.robert.model.NoKeyException
import com.lunabeestudio.robert.model.ReportDelayException
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.model.RobertUnknownException
import com.lunabeestudio.robert.model.UnknownException
import com.lunabeestudio.robert.repository.EphemeralBluetoothIdentifierRepository
import com.lunabeestudio.robert.repository.KeystoreRepository
import com.lunabeestudio.robert.repository.LocalProximityRepository
import com.lunabeestudio.robert.repository.RemoteServiceRepository
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.robert.worker.StatusWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

class RobertManagerImpl(
    application: RobertApplication,
    localEphemeralBluetoothIdentifierDataSource: LocalEphemeralBluetoothIdentifierDataSource,
    localKeystoreDataSource: LocalKeystoreDataSource,
    localLocalProximityDataSource: LocalLocalProximityDataSource,
    serviceDataSource: RemoteServiceDataSource,
    sharedCryptoDataSource: SharedCryptoDataSource,
    configurationDataSource: ConfigurationDataSource,
    private val localProximityFilter: LocalProximityFilter,
) : RobertManager {
    private val statusSemaphore: Semaphore = Semaphore(1)
    private val ephemeralBluetoothIdentifierRepository: EphemeralBluetoothIdentifierRepository =
        EphemeralBluetoothIdentifierRepository(localEphemeralBluetoothIdentifierDataSource, sharedCryptoDataSource, localKeystoreDataSource)
    private val keystoreRepository: KeystoreRepository =
        KeystoreRepository(localKeystoreDataSource, this)
    private val localProximityRepository: LocalProximityRepository =
        LocalProximityRepository(localLocalProximityDataSource)
    private val remoteServiceRepository: RemoteServiceRepository =
        RemoteServiceRepository(
            serviceDataSource,
            sharedCryptoDataSource,
            localKeystoreDataSource,
            configurationDataSource
        )

    private var _configuration: Configuration
    override val configuration: Configuration
        get() = keystoreRepository.configuration ?: _configuration

    override var shouldReloadBleSettings: Boolean
        get() = keystoreRepository.shouldReloadBleSettings
        set(value) {
            keystoreRepository.shouldReloadBleSettings = value
        }

    override val canActivateProximity: Boolean
        get() = keystoreRepository.reportDate?.let { reportDate ->
            val reportCalendar = Calendar.getInstance().apply {
                timeInMillis = reportDate
                add(Calendar.MONTH, RobertConstant.REGISTER_DELAY_MONTH)
            }
            System.currentTimeMillis() > reportCalendar.timeInMillis
        } ?: true

    override val isRegistered: Boolean
        get() = keystoreRepository.isRegistered

    override val isProximityActive: Boolean
        get() = keystoreRepository.proximityActive ?: false

    private val atRiskMutableLiveData: MutableLiveData<Event<AtRiskStatus>> = MutableLiveData()
    override val atRiskStatus: LiveData<Event<AtRiskStatus>> = atRiskMutableLiveData

    override val isAtRisk: Boolean?
        get() = keystoreRepository.lastRiskReceivedDate?.let {
            // if last time we've been notified is older than quarantine period minus the last exposure time frame :
            System.currentTimeMillis() - it < TimeUnit.DAYS.toMillis((configuration.quarantinePeriod - lastExposureTimeframe).toLong())
        } ?: atRiskLastRefresh?.let { false }

    override val lastRiskReceivedDate: Long?
        get() = keystoreRepository.lastRiskReceivedDate

    override val isWarningAtRisk: Boolean?
        get() = keystoreRepository.isWarningAtRisk

    override val lastWarningReceivedDate: Long?
        get() = keystoreRepository.lastWarningReceivedDate

    override val atRiskLastRefresh: Long?
        get() = keystoreRepository.atRiskLastRefresh

    override val lastExposureTimeframe: Int
        get() = keystoreRepository.lastExposureTimeframe ?: 0

    override val isSick: Boolean
        get() = keystoreRepository.isSick ?: false

    override val filteringMode: LocalProximityFilter.Mode
        get() = safeEnumValueOf<LocalProximityFilter.Mode>(configuration.filterMode) ?: RobertConstant.BLE_FILTER_MODE

    private val apiVersion: String
        get() = configuration.apiVersion

    override val reportSymptomsStartDate: Long?
        get() = keystoreRepository.reportSymptomsStartDate

    override val reportPositiveTestDate: Long?
        get() = keystoreRepository.reportPositiveTestDate

    init {
        _configuration = remoteServiceRepository.loadConfig(application.getAppContext())
        if (isRegistered) {
            startStatusWorker(application.getAppContext())
        }
    }

    override suspend fun refreshConfig(application: RobertApplication): RobertResult {
        val configResult = remoteServiceRepository.fetchOrLoadConfig(application.getAppContext())
        return when (configResult) {
            is RobertResultData.Success -> {
                try {
                    handleConfigChange(configResult.data)
                    RobertResult.Success()
                } catch (e: Exception) {
                    Timber.e(e)
                    if (e is RobertException) {
                        RobertResult.Failure(e)
                    } else {
                        RobertResult.Failure(RobertUnknownException())
                    }
                }
            }
            is RobertResultData.Failure -> {
                RobertResult.Failure(configResult.error)
            }
        }
    }

    override suspend fun generateCaptcha(type: String, local: String): RobertResultData<String> =
        remoteServiceRepository.generateCaptcha(apiVersion, type, local)

    override suspend fun getCaptchaImage(
        captchaId: String,
        path: String,
    ): RobertResult = remoteServiceRepository.getCaptchaImage(apiVersion, captchaId, path)

    override suspend fun getCaptchaAudio(
        captchaId: String,
        path: String,
    ): RobertResult = remoteServiceRepository.getCaptchaAudio(apiVersion, captchaId, path)

    override suspend fun registerV2(
        application: RobertApplication,
        captcha: String,
        captchaId: String,
        activateProximity: Boolean,
    ): RobertResult {
        val registerResult = remoteServiceRepository.registerV2(apiVersion, captcha, captchaId)
        return when (registerResult) {
            is RobertResultData.Success -> {
                finishRegister(application, registerResult.data, activateProximity)
            }
            is RobertResultData.Failure -> {
                clearLocalData(application)
                RobertResult.Failure(registerResult.error)
            }
        }
    }

    private suspend fun finishRegister(
        application: RobertApplication,
        registerReport: RegisterReport,
        activateProximity: Boolean,
    ): RobertResult {
        return try {
            keystoreRepository.timeStart = registerReport.timeStart
            keystoreRepository.atRiskLastRefresh = System.currentTimeMillis()
            ephemeralBluetoothIdentifierRepository.save(Base64.decode(registerReport.tuples, Base64.NO_WRAP))
            startStatusWorker(application.getAppContext())
            if (activateProximity) {
                activateProximity(application)
            }
            RobertResult.Success()
        } catch (e: Exception) {
            Timber.e(e)
            clearLocalData(application)
            if (e is RobertException) {
                RobertResult.Failure(e)
            } else {
                RobertResult.Failure(RobertUnknownException())
            }
        }
    }

    private fun handleConfigChange(configuration: Configuration) {
        if (configuration.version <= keystoreRepository.configuration?.version ?: -1) {
            return
        }
        keystoreRepository.configuration = configuration
    }

    override suspend fun activateProximity(application: RobertApplication, statusTried: Boolean): RobertResult {
        return if (!canActivateProximity) {
            RobertResult.Failure(ReportDelayException())
        } else {
            val isHelloAvailable = ephemeralBluetoothIdentifierRepository.getForTime() != null
            when {
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
    }

    override fun deactivateProximity(application: RobertApplication) {
        keystoreRepository.proximityActive = false
        application.refreshProximityService()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun updateStatus(robertApplication: RobertApplication): RobertResult {
        return withContext(Dispatchers.IO) {
            refreshConfig(robertApplication)
            statusSemaphore.withPermit {
                val ssu = getSSU(RobertConstant.PREFIX.C2)
                val checkStatusFrequencyHourMs = Duration.convert(
                    configuration.checkStatusFrequencyHour.toDouble(),
                    DurationUnit.HOURS,
                    DurationUnit.MILLISECONDS
                )
                val lastSuccessLongEnough =
                    abs((atRiskLastRefresh ?: 0L) - System.currentTimeMillis()) > checkStatusFrequencyHourMs
                val minStatusRetryDurationMs = Duration.convert(
                    configuration.minStatusRetryDuration.toDouble(),
                    DurationUnit.HOURS,
                    DurationUnit.MILLISECONDS
                )
                val lastErrorLongEnough =
                    abs((keystoreRepository.atRiskLastError ?: 0L) - System.currentTimeMillis()) > minStatusRetryDurationMs
                val shouldRefreshStatus = lastSuccessLongEnough || lastErrorLongEnough
                if (shouldRefreshStatus) {
                    if (ssu is RobertResultData.Success) {
                        val venueQrCodeList: List<VenueQrCode>? = robertApplication.getVenueQrCodeList(null)
                        if (!venueQrCodeList.isNullOrEmpty()) {
                            val wResult = remoteServiceRepository.wstatus(configuration.warningApiVersion, venueQrCodeList)
                            when (wResult) {
                                is RobertResultData.Success -> {
                                    keystoreRepository.atRiskLastRefresh = System.currentTimeMillis()
                                    if (keystoreRepository.isWarningAtRisk != wResult.data.atRisk && wResult.data.atRisk) {
                                        robertApplication.warningAtRiskDetected()
                                        keystoreRepository.lastWarningReceivedDate = System.currentTimeMillis()
                                    }
                                    keystoreRepository.isWarningAtRisk = wResult.data.atRisk
                                }
                                is RobertResultData.Failure -> {
                                    Timber.e(wResult.error)
                                }
                            }
                        }
                        val result = remoteServiceRepository.status(apiVersion, ssu.data)
                        when (result) {
                            is RobertResultData.Success -> {
                                try {
                                    keystoreRepository.atRiskLastRefresh = System.currentTimeMillis()
                                    ephemeralBluetoothIdentifierRepository.save(Base64.decode(result.data.tuples, Base64.NO_WRAP))
                                    // Only notify at risk became true every 24h
                                    val atRiskDelayOK = keystoreRepository.lastRiskReceivedDate?.let {
                                        it < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1L)
                                    } ?: true
                                    if (result.data.atRisk && atRiskDelayOK) {
                                        keystoreRepository.lastRiskReceivedDate = System.currentTimeMillis()
                                        keystoreRepository.lastExposureTimeframe = result.data.lastExposureTimeframe
                                        robertApplication.atRiskDetected()
                                    }
                                    keystoreRepository.shouldReloadBleSettings = true

                                    Timber.v("Updating worker status to new random date")
                                    StatusWorker.scheduleStatusWorker(
                                        robertApplication.getAppContext(),
                                        this@RobertManagerImpl,
                                        ExistingPeriodicWorkPolicy.REPLACE
                                    )

                                    RobertResult.Success()
                                } catch (e: Exception) {
                                    keystoreRepository.atRiskLastError = System.currentTimeMillis()
                                    when (e) {
                                        is RobertException -> {
                                            RobertResult.Failure(e)
                                        }
                                        else -> {
                                            RobertResult.Failure(RobertUnknownException())
                                        }
                                    }
                                }
                            }
                            is RobertResultData.Failure -> {
                                keystoreRepository.atRiskLastError = System.currentTimeMillis()
                                RobertResult.Failure(result.error)
                            }
                        }
                    } else {
                        val error = ssu as RobertResultData.Failure
                        Timber.e("hello or timeStart not found (${error.error?.message})")
                        RobertResult.Failure(error.error ?: UnknownException())
                    }
                } else {
                    Timber.v("Previous success Status called too close")
                    RobertResult.Failure(UnknownException())
                }
            }
        }
    }

    override fun clearOldData() {
        Timber.v("clear old data")
        val ephemeralBluetoothIdentifierExpiredTime = System.currentTimeMillis().unixTimeMsToNtpTimeS()
        ephemeralBluetoothIdentifierRepository.removeUntilTimeKeepLast(ephemeralBluetoothIdentifierExpiredTime)
        val localProximityExpiredTime: Long =
            (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(
                configuration.dataRetentionPeriod.toLong()
            )).unixTimeMsToNtpTimeS()
        localProximityRepository.removeUntilTime(localProximityExpiredTime)
    }

    override suspend fun report(
        token: String,
        firstSymptoms: Int?,
        positiveTest: Int?,
        application: RobertApplication,
    ): RobertResult {
        // Max take hello 14 days from now
        var originDayInPast = configuration.dataRetentionPeriod.toLong()
        when {
            firstSymptoms != null -> {
                // if symptoms take `preSymptomsSpan` days before first symptoms
                val preSymptomsSpan = configuration.preSymptomsSpan.toLong()
                originDayInPast = min(originDayInPast, firstSymptoms.toLong() + preSymptomsSpan)
            }
            positiveTest != null -> {
                // if positive test take `positiveSampleSpan` days before positive test
                val positiveSampleSpan = configuration.positiveSampleSpan.toLong()
                originDayInPast = min(originDayInPast, positiveTest.toLong() + positiveSampleSpan)
            }
        }
        val reportStartTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(originDayInPast)
        val firstProximityToSendTime = reportStartTime.unixTimeMsToNtpTimeS()

        val localProximityList = localProximityRepository.getUntilTime(firstProximityToSendTime)
        val filteredLocalProximityList = localProximityFilter.filter(
            localProximityList,
            filteringMode,
            configuration.filterConfig
        )

        val result = remoteServiceRepository.report(apiVersion, token, filteredLocalProximityList)
        return when (result) {
            is RobertResultData.Success -> {
                keystoreRepository.isSick = true
                val reportCalendar = Calendar.getInstance()
                reportCalendar.add(Calendar.DAY_OF_YEAR, -originDayInPast.toInt())
                keystoreRepository.reportDate = reportCalendar.timeInMillis
                deactivateProximity(application)
                keystoreRepository.reportSymptomsStartDate = firstSymptoms?.let { System.currentTimeMillis() - TimeUnit.DAYS.toMillis(it.toLong()) }
                keystoreRepository.reportPositiveTestDate = positiveTest?.let { System.currentTimeMillis() - TimeUnit.DAYS.toMillis(it.toLong()) }
                keystoreRepository.reportValidationToken = result.data.reportValidationToken
                keystoreRepository.reportToSendTime = firstProximityToSendTime
                wreportIfNeeded(application, true)
                RobertResult.Success()
            }
            is RobertResultData.Failure -> RobertResult.Failure(result.error)
        }
    }

    override suspend fun wreportIfNeeded(application: RobertApplication, shouldRetry: Boolean) {
        val wToken = keystoreRepository.reportValidationToken
        val reportToSendTime = keystoreRepository.reportToSendTime
        if (wToken != null && reportToSendTime != null) {
            val venueQrCodeList = application.getVenueQrCodeList(reportToSendTime)
            if (!venueQrCodeList.isNullOrEmpty()) {
                val result = remoteServiceRepository.wreport(configuration.warningApiVersion, wToken, venueQrCodeList)
                when (result) {
                    is RobertResult.Success -> {
                        keystoreRepository.reportValidationToken = null
                        keystoreRepository.reportToSendTime = null
                        application.clearVenueQrCodeList()
                    }
                    is RobertResult.Failure -> {
                        // 403 means token invalid, erase token
                        if (result.error is ForbiddenException) {
                            keystoreRepository.reportValidationToken = null
                            keystoreRepository.reportToSendTime = null
                            application.clearVenueQrCodeList()
                        } else if (shouldRetry) {
                            wreportIfNeeded(application, false)
                        }
                    }
                }
            } else {
                keystoreRepository.reportValidationToken = null
                keystoreRepository.reportToSendTime = null
                application.clearVenueQrCodeList()
            }
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

    override suspend fun eraseRemoteExposureHistory(application: RobertApplication): RobertResult {
        val ssu = getSSU(RobertConstant.PREFIX.C4)
        return when (ssu) {
            is RobertResultData.Success -> {
                val result = remoteServiceRepository.deleteExposureHistory(apiVersion, ssu.data)
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
        keystoreRepository.lastRiskReceivedDate = null
        keystoreRepository.atRiskLastRefresh = null
        keystoreRepository.lastExposureTimeframe = null
        keystoreRepository.isWarningAtRisk = null
        return RobertResult.Success()
    }

    override suspend fun quitStopCovid(application: RobertApplication): RobertResult {
        val ssu = getSSU(RobertConstant.PREFIX.C3)
        return when (ssu) {
            is RobertResultData.Success -> {
                val result = remoteServiceRepository.unregister(apiVersion, ssu.data)
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
        keystoreRepository.isRegistered = false
        keystoreRepository.kA = null
        keystoreRepository.kEA = null
        keystoreRepository.timeStart = null
        keystoreRepository.lastRiskReceivedDate = null
        keystoreRepository.atRiskLastRefresh = null
        keystoreRepository.lastExposureTimeframe = null
        keystoreRepository.proximityActive = null
        keystoreRepository.isSick = null
        keystoreRepository.reportDate = null
        keystoreRepository.configuration = configuration.apply { version = 0 }
        keystoreRepository.reportPositiveTestDate = null
        keystoreRepository.reportSymptomsStartDate = null
        keystoreRepository.isWarningAtRisk = null
        keystoreRepository.reportValidationToken = null
        keystoreRepository.reportToSendTime = null
    }

    private fun startStatusWorker(context: Context) {
        Timber.i("Create worker status")
        StatusWorker.scheduleStatusWorker(context, this, ExistingPeriodicWorkPolicy.KEEP)
    }

    private fun stopStatusWorker(context: Context) {
        Timber.i("Stop worker status")
        WorkManager.getInstance(context).cancelUniqueWork(RobertConstant.STATUS_WORKER_NAME)
    }

    override fun refreshAtRisk() {
        isAtRisk.toAtRiskStatus(isWarningAtRisk).let { atRisk ->
            if (atRiskMutableLiveData.value == null || atRiskMutableLiveData.value?.peekContent() != atRisk) {
                atRiskMutableLiveData.postValue(Event(atRisk))
            }
        }
    }
}
