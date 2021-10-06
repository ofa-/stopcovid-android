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
import com.lunabeestudio.analytics.manager.AnalyticsManager
import com.lunabeestudio.analytics.model.AppEventName
import com.lunabeestudio.analytics.model.HealthEventName
import com.lunabeestudio.domain.extension.unixTimeMsToNtpTimeS
import com.lunabeestudio.domain.model.AtRiskStatus
import com.lunabeestudio.domain.model.Calibration
import com.lunabeestudio.domain.model.CaptchaType
import com.lunabeestudio.domain.model.Cluster
import com.lunabeestudio.domain.model.ClusterIndex
import com.lunabeestudio.domain.model.Configuration
import com.lunabeestudio.domain.model.EphemeralBluetoothIdentifier
import com.lunabeestudio.domain.model.HelloBuilder
import com.lunabeestudio.domain.model.HelloSettings
import com.lunabeestudio.domain.model.LocalProximity
import com.lunabeestudio.domain.model.RegisterReport
import com.lunabeestudio.domain.model.SSUBuilder
import com.lunabeestudio.domain.model.SSUSettings
import com.lunabeestudio.domain.model.ServerStatusUpdate
import com.lunabeestudio.domain.model.VenueQrCode
import com.lunabeestudio.robert.datasource.LocalEphemeralBluetoothIdentifierDataSource
import com.lunabeestudio.robert.datasource.LocalKeystoreDataSource
import com.lunabeestudio.robert.datasource.LocalLocalProximityDataSource
import com.lunabeestudio.robert.datasource.RemoteCleaDataSource
import com.lunabeestudio.robert.datasource.RemoteServiceDataSource
import com.lunabeestudio.robert.datasource.RobertCalibrationDataSource
import com.lunabeestudio.robert.datasource.RobertConfigurationDataSource
import com.lunabeestudio.robert.datasource.SharedCryptoDataSource
import com.lunabeestudio.robert.extension.safeEnumValueOf
import com.lunabeestudio.robert.extension.use
import com.lunabeestudio.robert.manager.LocalProximityFilter
import com.lunabeestudio.robert.model.ForbiddenException
import com.lunabeestudio.robert.model.NoEphemeralBluetoothIdentifierFound
import com.lunabeestudio.robert.model.NoEphemeralBluetoothIdentifierFoundForEpoch
import com.lunabeestudio.robert.model.NoKeyException
import com.lunabeestudio.robert.model.ReportDelayException
import com.lunabeestudio.robert.model.RequireRobertRegisterException
import com.lunabeestudio.robert.model.RequireRobertResetException
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.robert.model.RobertResult
import com.lunabeestudio.robert.model.RobertResultData
import com.lunabeestudio.robert.model.RobertUnknownException
import com.lunabeestudio.robert.model.UnknownException
import com.lunabeestudio.robert.repository.CleaRepository
import com.lunabeestudio.robert.repository.EphemeralBluetoothIdentifierRepository
import com.lunabeestudio.robert.repository.KeystoreRepository
import com.lunabeestudio.robert.repository.LocalProximityRepository
import com.lunabeestudio.robert.repository.RemoteServiceRepository
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.robert.worker.StatusWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

class RobertManagerImpl(
    application: RobertApplication,
    localEphemeralBluetoothIdentifierDataSource: LocalEphemeralBluetoothIdentifierDataSource,
    localKeystoreDataSource: LocalKeystoreDataSource,
    localLocalProximityDataSource: LocalLocalProximityDataSource,
    serviceDataSource: RemoteServiceDataSource,
    cleaDataSource: RemoteCleaDataSource,
    sharedCryptoDataSource: SharedCryptoDataSource,
    configurationDataSource: RobertConfigurationDataSource,
    calibrationDataSource: RobertCalibrationDataSource,
    serverPublicKey: String,
    private val localProximityFilter: LocalProximityFilter,
    private val analyticsManager: AnalyticsManager,
    coroutineScope: CoroutineScope,
) : RobertManager {
    val disseminatedEbidsFile = File(application.getAppContext().filesDir, "disseminatedEbids.txt")
    val localProximityFile = File(application.getAppContext().filesDir, "localProximity.txt")

    private val statusMtx: Mutex = Mutex()
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
            configurationDataSource,
            calibrationDataSource,
            serverPublicKey
        )
    private val cleaRepository = CleaRepository(cleaDataSource, localKeystoreDataSource)

    private var _configuration: Configuration = remoteServiceRepository.loadConfig(application.getAppContext())
    override val configuration: Configuration
        get() = keystoreRepository.configuration ?: _configuration

    private var _calibration: Calibration = remoteServiceRepository.loadCalibration(application.getAppContext())
    override val calibration: Calibration
        get() = keystoreRepository.calibration ?: _calibration

    override var shouldReloadBleSettings: Boolean
        get() = keystoreRepository.shouldReloadBleSettings
        set(value) {
            keystoreRepository.shouldReloadBleSettings = value
        }

    override val isImmune: Boolean
        get() = false
    /*
        get() = keystoreRepository.reportDate?.let { reportDate ->
            val endOfSickCal = Calendar.getInstance().apply {
                timeInMillis = reportDate
                add(Calendar.DAY_OF_YEAR, configuration.covidPlusNoTracing)
            }
            System.currentTimeMillis() <= endOfSickCal.timeInMillis
        } ?: false
    */

    override val isSick: Boolean
        get() = keystoreRepository.reportDate?.let { reportDate ->
            val endOfVenueWarningCal = Calendar.getInstance().apply {
                timeInMillis = reportDate
                add(Calendar.DAY_OF_YEAR, configuration.covidPlusWarning)
            }
            System.currentTimeMillis() <= endOfVenueWarningCal.timeInMillis
        } ?: false

    override val isRegistered: Boolean
        get() = keystoreRepository.isRegistered

    override val isProximityActive: Boolean
        get() = keystoreRepository.proximityActive ?: false

    private val atRiskMutableLiveData: MutableLiveData<Event<AtRiskStatus>> = MutableLiveData()
    override val liveAtRiskStatus: LiveData<Event<AtRiskStatus>> = atRiskMutableLiveData

    private val updatingRiskStatus: MutableLiveData<Event<Boolean>> = MutableLiveData()
    override val liveUpdatingRiskStatus: LiveData<Event<Boolean>> = updatingRiskStatus

    override val atRiskStatus: AtRiskStatus?
        get() = keystoreRepository.atRiskStatus

    override val atRiskLastRefresh: Long?
        get() = keystoreRepository.atRiskLastRefresh

    override val filteringMode: LocalProximityFilter.Mode
        get() = safeEnumValueOf<LocalProximityFilter.Mode>(configuration.filterMode) ?: RobertConstant.BLE_FILTER_MODE

    private val apiVersion: String
        get() = configuration.apiVersion

    override val reportSymptomsStartDate: Long?
        get() = keystoreRepository.reportSymptomsStartDate

    override val reportPositiveTestDate: Long?
        get() = keystoreRepository.reportPositiveTestDate

    override val declarationToken: String?
        get() = keystoreRepository.declarationToken

    init {
        val isRegistered = try {
            isRegistered
        } catch (e: Exception) {
            Timber.e(e)
            false
        }
        if (isRegistered) {
            startStatusWorker(application.getAppContext())
            migrateOldIsAtRisk()
        } else {
            coroutineScope.launch {
                if (!localKeystoreDataSource.venuesQrCode().isNullOrEmpty()) {
                    startStatusWorker(application.getAppContext())
                }
            }
        }
    }

    private fun migrateOldIsAtRisk() {
        if (keystoreRepository.atRiskModelVersion != AT_RISK_MODEL_VERSION) {
            if (keystoreRepository.atRiskStatus == null && (keystoreRepository.atRiskModelVersion ?: 0) < 1) {
                val wasAtRisk = keystoreRepository.deprecatedLastRiskReceivedDate?.let {
                    // if last time we've been notified is older than quarantine period minus the last exposure time frame :
                    System.currentTimeMillis() - it < TimeUnit.DAYS.toMillis(
                        (configuration.quarantinePeriod - (keystoreRepository.deprecatedLastExposureTimeframe ?: 0)).toLong()
                    )
                } ?: atRiskLastRefresh?.let { false }
                keystoreRepository.atRiskStatus = when (wasAtRisk) {
                    true -> AtRiskStatus(4f, keystoreRepository.deprecatedLastRiskReceivedDate?.unixTimeMsToNtpTimeS(), null)
                    false -> AtRiskStatus(0f, null, null)
                    else -> null
                }
                keystoreRepository.deprecatedLastRiskReceivedDate = null
                keystoreRepository.deprecatedLastExposureTimeframe = null
            }
        }
        keystoreRepository.atRiskModelVersion = AT_RISK_MODEL_VERSION
    }

    override suspend fun refreshConfig(application: RobertApplication): RobertResult {
        val configResult = remoteServiceRepository.fetchOrLoadConfig(application.getAppContext())
        return when (configResult) {
            is RobertResultData.Success -> {
                try {
                    handleConfigChange(configResult.data)
                    refreshCalibration(application)
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

    private suspend fun refreshCalibration(application: RobertApplication): RobertResult {
        return if (configuration.versionCalibrationBle > calibration.version) {
            val calibrationResult = remoteServiceRepository.fetchOrLoadCalibration(application.getAppContext())
            when (calibrationResult) {
                is RobertResultData.Success -> {
                    try {
                        handleCalibrationChange(calibrationResult.data)
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
                    RobertResult.Failure(calibrationResult.error)
                }
            }
        } else {
            RobertResult.Success()
        }
    }

    override suspend fun generateCaptcha(type: CaptchaType, local: String): RobertResultData<String> =
        remoteServiceRepository.generateCaptcha(apiVersion, type, local)

    override suspend fun getCaptchaImage(
        captchaId: String,
        path: String,
    ): RobertResult = remoteServiceRepository.getCaptchaImage(apiVersion, captchaId, path)

    override suspend fun getCaptchaAudio(
        captchaId: String,
        path: String,
    ): RobertResult = remoteServiceRepository.getCaptchaAudio(apiVersion, captchaId, path)

    override suspend fun register(
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
            analyticsManager.register()
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

    private fun handleCalibrationChange(calibration: Calibration) {
        if (calibration.version <= keystoreRepository.calibration?.version ?: -1) {
            return
        }
        keystoreRepository.calibration = calibration
    }

    override suspend fun activateProximity(application: RobertApplication, statusTried: Boolean): RobertResult {
        return if (isImmune) {
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
        try {
            keystoreRepository.proximityActive = false
        } catch (e: Exception) {
            Timber.e(e)
        }
        application.refreshProximityService()
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun updateStatus(robertApplication: RobertApplication): RobertResult {
        updatingRiskStatus.postValue(Event(true))
        val result = withContext(Dispatchers.IO) {
            refreshConfig(robertApplication)
            statusMtx.withLock {
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
                    supervisorScope {
                        val cleaStatusResult = async {
                            Timber.v("Start CLEA status")
                            cleaStatus(robertApplication)
                        }
                        val statusResult = async {
                            if (isRegistered) {
                                Timber.v("Start ROBERT status")
                                val ssu = getSSU(RobertConstant.PREFIX.C2)
                                if (ssu is RobertResultData.Success) {
                                    status(robertApplication, ssu)
                                } else {
                                    val error = ssu as RobertResultData.Failure
                                    Timber.e("hello or timeStart not found (${error.error?.message})")
                                    RobertResultData.Failure(error.error ?: UnknownException())
                                }
                            } else {
                                RobertResultData.Failure(RequireRobertRegisterException())
                            }
                        }
                        try {
                            processStatusResults(robertApplication, statusResult.await(), cleaStatusResult.await())
                        } catch (e: RobertException) {
                            Timber.e(e)
                            RobertResult.Failure(e)
                        }
                    }
                } else {
                    Timber.v("Previous success Status called too close")
                    RobertResult.Failure(UnknownException())
                }
            }
        }
        updatingRiskStatus.postValue(Event(false))
        return result
    }

    suspend fun cleaStatus(robertApplication: RobertApplication): RobertResultData<AtRiskStatus> {
        val venueQrCodeList: List<VenueQrCode>? = try {
            robertApplication.getVenueQrCodeList(null, null)
        } catch (e: Exception) {
            Timber.e(e)
            return RobertResultData.Failure((e as? RobertException) ?: UnknownException())
        }
        if (!venueQrCodeList.isNullOrEmpty()) {
            val cleaStatusStart = System.currentTimeMillis()

            // Fetch the latest index file
            val clusterIndexResult: RobertResultData<ClusterIndex> = cleaRepository.cleaClusterIndex(configuration.cleaStatusApiVersion)
            val currentRiskStatus = currentCleaRiskStatus()

            return when (clusterIndexResult) {
                is RobertResultData.Success -> {
                    if (clusterIndexResult.data.iteration <= keystoreRepository.cleaLastStatusIteration ?: 0) {
                        RobertResultData.Success(currentRiskStatus)
                    } else {
                        val clusterIndex = clusterIndexResult.data
                        val riskStatus = calculateRiskForNewIteration(clusterIndex, venueQrCodeList)

                        val appStatus = when (robertApplication.isAppInForeground) {
                            true -> "f"
                            else -> "b"
                        }

                        analyticsManager.reportAppEvent(
                            robertApplication.getAppContext(),
                            AppEventName.e15,
                            "$appStatus ${System.currentTimeMillis() - cleaStatusStart}"
                        )
                        keystoreRepository.cleaLastStatusIteration = clusterIndexResult.data.iteration
                        RobertResultData.Success(riskStatus)
                    }
                }
                is RobertResultData.Failure -> {
                    RobertResultData.Failure(clusterIndexResult.error)
                }
            }
        } else {
            return RobertResultData.Success(AtRiskStatus(0f, null, null))
        }
    }

    private suspend fun calculateRiskForNewIteration(clusterIndex: ClusterIndex, venueQrCodeList: List<VenueQrCode>?): AtRiskStatus {
        val matchingPrefix = matchingCleaPrefix(clusterIndex, venueQrCodeList)

        val allClusters = mutableListOf<Cluster>()
        matchingPrefix.forEach {
            val clusters = cleaRepository.cleaClusterList(configuration.cleaStatusApiVersion, clusterIndex.iteration.toString(), it)

            // We simply skip clusters if we can't get them, it means that there is an error with the index file
            if (clusters is RobertResultData.Success) {
                // We add to the allClusters list, the ones that match our venues :
                allClusters.addAll(matchingClusters(clusters.data, venueQrCodeList))
            }
        }

        return newRiskStatus(allClusters)
    }

    private fun newRiskStatus(clusterList: List<Cluster>?): AtRiskStatus {
        // Calculate the max risk level
        var currentRiskLevel = 0f
        var currentDate: Long = 0
        clusterList?.forEach { cluster ->
            cluster.exposures?.forEach { exposure ->
                when {
                    exposure.riskLevel > currentRiskLevel -> {
                        currentRiskLevel = exposure.riskLevel
                        currentDate = exposure.startTimeNTP
                    }
                    exposure.riskLevel == currentRiskLevel && exposure.startTimeNTP > currentDate -> {
                        currentDate = exposure.startTimeNTP
                    }
                }
            }
        }
        return AtRiskStatus(currentRiskLevel, currentDate, currentDate)
    }

    private fun matchingClusters(clusterList: List<Cluster>?, venueQrCodeList: List<VenueQrCode>?): List<Cluster> {
        val clusterListWithTimeMatch = mutableListOf<Cluster>()
        clusterList?.forEach { cluster ->
            venueQrCodeList?.forEach { (_, ltid, ntpTimestamp) ->
                if (cluster.ltid == ltid) {
                    val expo = cluster.exposures?.filter { exposure ->
                        exposure.startTimeNTP <= ntpTimestamp
                            && ntpTimestamp <= exposure.startTimeNTP + exposure.duration
                    }
                    if (expo?.isNotEmpty() == true) {
                        clusterListWithTimeMatch.add(Cluster(ltid, expo))
                    }
                }
            }
        }
        return clusterListWithTimeMatch
    }

    private fun matchingCleaPrefix(
        clusterIndex: ClusterIndex,
        venueQrCodeList: List<VenueQrCode>?
    ): List<String> = clusterIndex.clusterPrefixList.filter { prefix ->
        (prefix as String?) != null &&
        venueQrCodeList?.any { (it.ltid as String?) != null && it.ltid.startsWith(prefix, true) } == true
    }

    private fun currentCleaRiskStatus(): AtRiskStatus = keystoreRepository.currentWarningAtRiskStatus ?: AtRiskStatus(0f, null, null)
    private fun currentRobertRiskStatus(): AtRiskStatus = keystoreRepository.currentRobertAtRiskStatus ?: AtRiskStatus(0f, null, null)

    private suspend fun status(
        robertApplication: RobertApplication,
        ssu: RobertResultData.Success<ServerStatusUpdate>,
    ): RobertResultData<AtRiskStatus> {
        val result = remoteServiceRepository.status(apiVersion, ssu.data)
        return when (result) {
            is RobertResultData.Success -> {
                try {
                    keystoreRepository.atRiskLastRefresh = System.currentTimeMillis()
                    keystoreRepository.declarationToken = result.data.declarationToken
                    ephemeralBluetoothIdentifierRepository.save(Base64.decode(result.data.tuples, Base64.NO_WRAP))
                    keystoreRepository.shouldReloadBleSettings = true
                    analyticsManager.statusDidSucceed(robertApplication.getAppContext())
                    if (result.data.analyticsToken != null) {
                        analyticsManager.sendAnalytics(
                            robertApplication.getAppContext(),
                            this,
                            robertApplication,
                            result.data.analyticsToken!!
                        )
                    } else {
                        analyticsManager.reset(robertApplication.getAppContext())
                    }
                    RobertResultData.Success(
                        AtRiskStatus(
                            result.data.riskLevel,
                            result.data.ntpLastRiskScoringS,
                            result.data.ntpLastContactS
                        )
                    )
                } catch (e: Exception) {
                    when (e) {
                        is RobertException -> {
                            RobertResultData.Failure(e)
                        }
                        else -> {
                            RobertResultData.Failure(RobertUnknownException())
                        }
                    }
                }
            }
            is RobertResultData.Failure -> {
                RobertResultData.Failure(result.error)
            }
        }
    }

    suspend fun processStatusResults(
        robertApplication: RobertApplication,
        statusResult: RobertResultData<AtRiskStatus>,
        wStatusResult: RobertResultData<AtRiskStatus>
    ): RobertResult {
        Timber.v("Process status results")

        if (statusResult is RobertResultData.Success) {
            if (statusResult.data.ntpLastRiskScoringS == null
                || (statusResult.data.ntpLastRiskScoringS ?: 0) > (
                    keystoreRepository.currentRobertAtRiskStatus?.ntpLastRiskScoringS
                        ?: -1
                    )
            ) {
                keystoreRepository.currentRobertAtRiskStatus = statusResult.data
            }
        } else if ((statusResult as? RobertResultData.Failure)?.error is RequireRobertResetException) {
            clearRobert(robertApplication)
        }

        if (wStatusResult is RobertResultData.Success) {
            if (wStatusResult.data.ntpLastRiskScoringS == null
                || (wStatusResult.data.ntpLastRiskScoringS ?: 0) > (
                    keystoreRepository.currentWarningAtRiskStatus?.ntpLastRiskScoringS
                        ?: -1
                    )
            ) {
                keystoreRepository.currentWarningAtRiskStatus = wStatusResult.data
            }
        }

        val prevAtRiskStatus = atRiskStatus
        val newStatusList = listOfNotNull(currentRobertRiskStatus(), currentCleaRiskStatus())
        val newAtRiskStatus: AtRiskStatus = newStatusList.maxByOrNull { it.riskLevel }!!

        newAtRiskStatus.ntpLastContactS = newAtRiskStatus.ntpLastContactS?.let { ntpLastContactS ->
            (ntpLastContactS + Random.nextLong(-RobertConstant.LAST_CONTACT_DELTA_S, RobertConstant.LAST_CONTACT_DELTA_S))
                .coerceAtMost(System.currentTimeMillis().unixTimeMsToNtpTimeS() - RobertConstant.LAST_CONTACT_DELTA_S)
        }

        // Edge case: robert status might failed because user has been unregistered after 18 days of failing status
        // In this case, only consider clea risk & continue to trigger status update
        val isRobertStatusOk = statusResult is RobertResultData.Success || !isRegistered
        val isCleaStatusOk = wStatusResult is RobertResultData.Success
        val isRiskRaised = newAtRiskStatus.riskLevel > prevAtRiskStatus?.riskLevel ?: 0f

        return if (isRobertStatusOk && isCleaStatusOk || isRiskRaised) {
            keystoreRepository.atRiskLastRefresh = System.currentTimeMillis()
            keystoreRepository.atRiskStatus = newAtRiskStatus
            if (!isImmune) {
                if (prevAtRiskStatus?.ntpLastRiskScoringS != newAtRiskStatus.ntpLastRiskScoringS) {
                    robertApplication.alertAtRiskLevelChange()
                }
                if (newAtRiskStatus.riskLevel > 0f
                    && (
                        (prevAtRiskStatus?.riskLevel ?: 0f) < newAtRiskStatus.riskLevel
                            || (
                                prevAtRiskStatus?.riskLevel == newAtRiskStatus.riskLevel
                                    && prevAtRiskStatus.ntpLastRiskScoringS ?: 0L < newAtRiskStatus.ntpLastRiskScoringS ?: 0L
                                )
                        )
                ) {
                    robertApplication.notifyAtRiskLevelChange()
                }
            }

            Timber.v("Updating worker status to new random date")
            StatusWorker.scheduleStatusWorker(
                robertApplication.getAppContext(),
                this@RobertManagerImpl,
                ExistingPeriodicWorkPolicy.REPLACE
            )
            RobertResult.Success()
        } else {
            keystoreRepository.atRiskLastError = System.currentTimeMillis()
            RobertResult.Failure((statusResult as? RobertResultData.Failure)?.error ?: (wStatusResult as? RobertResultData.Failure)?.error)
        }
    }

    override suspend fun clearOldData() {
        Timber.v("clear old data")
        val ephemeralBluetoothIdentifierExpiredTime = System.currentTimeMillis().unixTimeMsToNtpTimeS()
        ephemeralBluetoothIdentifierRepository.removeUntilTimeKeepLast(ephemeralBluetoothIdentifierExpiredTime)
        val localProximityExpiredTime: Long =
            (
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(
                    configuration.dataRetentionPeriod.toLong()
                )
                ).unixTimeMsToNtpTimeS()
        localProximityRepository.removeUntilTime(localProximityExpiredTime)
    }

    override suspend fun report(
        token: String,
        firstSymptoms: Int?,
        positiveTest: Int?,
        application: RobertApplication,
        onProgressUpdate: (Float) -> Unit,
    ): RobertResult {
        // Max take hello 14 days from now
        var originDayInPast = configuration.dataRetentionPeriod.toLong()
        // Min take hello before now
        var endDayInPast = 0L
        when {
            firstSymptoms != null -> {
                // if symptoms take `preSymptomsSpan` days before first symptoms
                val preSymptomsSpan = configuration.preSymptomsSpan.toLong()
                originDayInPast = min(originDayInPast, firstSymptoms.toLong() + preSymptomsSpan)
                // to `contagiousSpan` days after first symptoms
                endDayInPast = abs(min(endDayInPast, -firstSymptoms.toLong() + configuration.contagiousSpan))
            }
            positiveTest != null -> {
                // if positive test take `positiveSampleSpan` days before positive test
                val positiveSampleSpan = configuration.positiveSampleSpan.toLong()
                originDayInPast = min(originDayInPast, positiveTest.toLong() + positiveSampleSpan)
                // to `contagiousSpan` days after positive test
                endDayInPast = abs(min(endDayInPast, -positiveTest.toLong() + configuration.contagiousSpan))
            }
        }
        Timber.d("Will report data from $originDayInPast days to $endDayInPast days from now")
        val reportStartTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(originDayInPast)
        val reportEndTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(endDayInPast)
        val firstProximityToSendTime = reportStartTime.unixTimeMsToNtpTimeS()
        val lastProximityToSendTime = reportEndTime.unixTimeMsToNtpTimeS()

        var localProximityList = localProximityRepository.getBetweenTime(firstProximityToSendTime, lastProximityToSendTime) { progress ->
            onProgressUpdate(progress * 0.8f)
        }

        localProximityList = localProximityFilter.filter(
            localProximityList,
            filteringMode,
            configuration.filterConfig
        )

        val result = remoteServiceRepository.report(apiVersion, token, localProximityList) {
            onProgressUpdate(0.8f + (it * 0.2f))
        }
        return when (result) {
            is RobertResultData.Success -> {
                analyticsManager.reportHealthEvent(application.getAppContext(), HealthEventName.eh1, null)
                val reportCalendar = Calendar.getInstance()
                reportCalendar.add(Calendar.DAY_OF_YEAR, -originDayInPast.toInt())
                keystoreRepository.reportDate = reportCalendar.timeInMillis
                deactivateProximity(application)
                keystoreRepository.reportSymptomsStartDate =
                    firstSymptoms?.let { System.currentTimeMillis() - TimeUnit.DAYS.toMillis(it.toLong()) }
                keystoreRepository.reportPositiveTestDate =
                    positiveTest?.let { System.currentTimeMillis() - TimeUnit.DAYS.toMillis(it.toLong()) }
                keystoreRepository.reportValidationToken = result.data.reportValidationToken
                keystoreRepository.reportToSendStartTime = firstProximityToSendTime
                keystoreRepository.reportToSendEndTime = lastProximityToSendTime
                cleaReportIfNeeded(application, true)
                RobertResult.Success()
            }
            is RobertResultData.Failure -> RobertResult.Failure(result.error)
        }
    }

    override suspend fun cleaReportIfNeeded(application: RobertApplication, shouldRetry: Boolean) {

        val wToken = keystoreRepository.reportValidationToken
        if (wToken != null) {
            val pivotDate = keystoreRepository.reportSymptomsStartDate?.let {
                it - TimeUnit.DAYS.toMillis(configuration.preSymptomsSpan.toLong())
            } ?: keystoreRepository.reportPositiveTestDate?.let {
                it - TimeUnit.DAYS.toMillis(configuration.positiveSampleSpan.toLong())
            } ?: System.currentTimeMillis() - TimeUnit.DAYS.toMillis(configuration.venuesRetentionPeriod.toLong())

            // we send everything
            val venueQrCodeList = application.getVenueQrCodeList(null, null)
            if (!venueQrCodeList.isNullOrEmpty()) {
                val result = cleaRepository.wreportClea(
                    configuration.cleaReportApiVersion,
                    wToken,
                    pivotDate.unixTimeMsToNtpTimeS(),
                    venueQrCodeList
                )

                when (result) {
                    is RobertResult.Success -> {
                        keystoreRepository.reportValidationToken = null
                        keystoreRepository.reportToSendStartTime = null
                        keystoreRepository.reportToSendEndTime = null
                        application.clearVenueQrCodeList()
                    }
                    is RobertResult.Failure -> {
                        // 403 means token invalid, erase token
                        if (result.error is ForbiddenException) {
                            keystoreRepository.reportValidationToken = null
                            keystoreRepository.reportToSendStartTime = null
                            keystoreRepository.reportToSendEndTime = null
                            application.clearVenueQrCodeList()
                        } else if (shouldRetry) {
                            cleaReportIfNeeded(application, false)
                        }
                    }
                }
            } else {
                keystoreRepository.reportValidationToken = null
                keystoreRepository.reportToSendStartTime = null
                keystoreRepository.reportToSendEndTime = null
                application.clearVenueQrCodeList()
            }
        }
    }

    override suspend fun storeLocalProximity(vararg localProximity: LocalProximity) {
        localProximityRepository.save(*localProximity)
    }

    override suspend fun getSSU(prefix: Byte): RobertResultData<ServerStatusUpdate> {
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

    override suspend fun getCurrentHelloBuilder(): RobertResultData<HelloBuilder> {
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
        localProximityFile.run { exists() && delete() }
        disseminatedEbidsFile.run { exists() && delete() }
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

    override fun eraseRemoteAlert(): RobertResult {
        keystoreRepository.atRiskLastRefresh = null
        keystoreRepository.atRiskStatus = null
        keystoreRepository.currentRobertAtRiskStatus = null
        keystoreRepository.currentWarningAtRiskStatus = null
        keystoreRepository.cleaLastStatusIteration = null
        keystoreRepository.declarationToken = null
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

    override suspend fun clearLocalData(application: RobertApplication) {
        clearRobert(application)
        keystoreRepository.configuration = configuration.apply { version = 0 }
        keystoreRepository.calibration = calibration.apply { version = 0 }
        analyticsManager.unregister(application.getAppContext())
        try {
            keystoreRepository.atRiskModelVersion = AT_RISK_MODEL_VERSION
        } catch (e: Exception) {
            Timber.e(e)
        }
        keystoreRepository.cleaLastStatusIteration = null
        keystoreRepository.currentWarningAtRiskStatus = null
        keystoreRepository.reportPositiveTestDate = null
        keystoreRepository.reportSymptomsStartDate = null
    }

    private suspend fun clearRobert(application: RobertApplication) {
        stopStatusWorker(application.getAppContext())
        deactivateProximity(application)
        ephemeralBluetoothIdentifierRepository.removeAll()
        localProximityRepository.removeAll()
        try {
            keystoreRepository.isRegistered = false
        } catch (e: Exception) {
            Timber.e(e)
        }
        keystoreRepository.kA = null
        keystoreRepository.kEA = null
        keystoreRepository.timeStart = null
        keystoreRepository.atRiskLastRefresh = null
        keystoreRepository.atRiskLastError = null
        keystoreRepository.atRiskStatus = null
        keystoreRepository.currentRobertAtRiskStatus = null
        keystoreRepository.deprecatedLastRiskReceivedDate = null
        keystoreRepository.deprecatedLastExposureTimeframe = null
        keystoreRepository.proximityActive = null
        keystoreRepository.reportDate = null
        keystoreRepository.reportValidationToken = null
        keystoreRepository.reportToSendStartTime = null
        keystoreRepository.reportToSendEndTime = null
        keystoreRepository.declarationToken = null
    }

    /*suspend*/ fun getLocalProximityItems(timeMs : Long = 0): List<LocalProximity> {
        return localProximityRepository.getBetweenTime(0, timeMs)
    }

    suspend fun getLocalEbids(): List<EphemeralBluetoothIdentifier> {
        return ephemeralBluetoothIdentifierRepository.getAll()
    }

    fun getCurrentEbid(): EphemeralBluetoothIdentifier? {
        return runBlocking { ephemeralBluetoothIdentifierRepository.getForTime() }
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
        atRiskStatus?.let { atRisk ->
            if (atRiskMutableLiveData.value == null || atRiskMutableLiveData.value?.peekContent() != atRisk) {
                atRiskMutableLiveData.postValue(Event(atRisk))
            }
        }
    }

    override suspend fun getLocalProximityCount(): Int {
        return withContext(Dispatchers.IO) {
            localProximityRepository.getBetweenTime(Long.MIN_VALUE, Long.MAX_VALUE).size
        }
    }

    companion object {
        private const val AT_RISK_MODEL_VERSION: Int = 1
    }
}
