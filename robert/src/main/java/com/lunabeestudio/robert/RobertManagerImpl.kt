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
import com.lunabeestudio.robert.extension.safeEnumValueOf
import com.lunabeestudio.robert.extension.toAtRiskStatus
import com.lunabeestudio.robert.extension.use
import com.lunabeestudio.robert.manager.LocalProximityFilter
import com.lunabeestudio.robert.model.AtRiskStatus
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
import com.lunabeestudio.robert.utils.Event
import com.lunabeestudio.robert.worker.StatusWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
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
    private val localProximityFilter: LocalProximityFilter
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

    init {
        if (isRegistered) {
            startStatusWorker(application.getAppContext())
        }
    }

    override var shouldReloadBleSettings: Boolean
        get() = keystoreRepository.shouldReloadBleSettings
        set(value) {
            keystoreRepository.shouldReloadBleSettings = value
        }

    override val isRegistered: Boolean
        get() = keystoreRepository.kA != null && keystoreRepository.kEA != null

    override val isProximityActive: Boolean
        get() = keystoreRepository.proximityActive ?: false

    private val atRiskMutableLiveData: MutableLiveData<Event<AtRiskStatus>> = MutableLiveData()
    override val atRiskStatus: LiveData<Event<AtRiskStatus>> = atRiskMutableLiveData

    override val isAtRisk: Boolean?
        get() = keystoreRepository.lastRiskReceivedDate?.let {
            // if last time we've been notified is older than quarantine period minus the last exposure time frame :
            System.currentTimeMillis() - it < TimeUnit.DAYS.toMillis((quarantinePeriod - lastExposureTimeframe).toLong())
        } ?: atRiskLastRefresh?.let { false }

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

    override val filteringConfig: String
        get() = keystoreRepository.filteringConfig ?: RobertConstant.BLE_FILTER_CONFIG

    override val filteringMode: LocalProximityFilter.Mode
        get() = keystoreRepository.filteringMode?.let { safeEnumValueOf<LocalProximityFilter.Mode>(it) } ?: RobertConstant.BLE_FILTER_MODE

    override val serviceUUID: String
        get() = keystoreRepository.serviceUUID ?: RobertConstant.BLE_SERVICE_UUID

    override val characteristicUUID: String
        get() = keystoreRepository.characteristicUUID ?: RobertConstant.BLE_CHARACTERISTIC_UUID

    override val backgroundServiceManufacturerData: String
        get() = keystoreRepository.backgroundServiceManufacturerData ?: RobertConstant.BLE_BACKGROUND_SERVICE_MANUFACTURER_DATA_IOS

    override val checkStatusFrequencyHour: Float
        get() = keystoreRepository.checkStatusFrequencyHour ?: RobertConstant.CHECK_STATUS_FREQUENCY_HOURS

    override val minStatusRetryDuration: Float
        get() = keystoreRepository.minStatusRetryDuration ?: RobertConstant.MIN_STATUS_RETRY_DURATION

    override val randomStatusHour: Float
        get() = keystoreRepository.randomStatusHour ?: RobertConstant.RANDOM_STATUS_HOUR

    override val apiVersion: String
        get() = keystoreRepository.apiVersion ?: RobertConstant.API_VERSION

    override val qrCodeDeletionHours: Float
        get() = keystoreRepository.qrCodeDeletionHours ?: RobertConstant.QR_CODE_DELETION_HOURS

    override val qrCodeExpiredHours: Float
        get() = keystoreRepository.qrCodeExpiredHours ?: RobertConstant.QR_CODE_EXPIRED_HOURS

    override val qrCodeFormattedString: String
        get() = keystoreRepository.qrCodeFormattedString ?: RobertConstant.QR_CODE_FORMATTED_STRING

    override val qrCodeFormattedStringDisplayed: String
        get() = keystoreRepository.qrCodeFormattedStringDisplayed ?: RobertConstant.QR_CODE_FORMATTED_STRING_DISPLAYED

    override val qrCodeFooterString: String
        get() = keystoreRepository.qrCodeFooterString ?: RobertConstant.QR_CODE_FOOTER_STRING

    override val displayDepartmentLevel: Boolean
        get() = keystoreRepository.displayDepartmentLevel ?: false

    override val proximityReactivationReminderHours: List<Int>
        get() = keystoreRepository.proximityReactivationReminderHours ?: emptyList()

    override suspend fun refreshConfig(application: RobertApplication): RobertResult {
        val configResult = remoteServiceRepository.fetchOrLoadConfig(application.getAppContext())
        return when (configResult) {
            is RobertResultData.Success -> {
                if (configResult.data.isNullOrEmpty()) {
                    RobertResult.Failure(RobertUnknownException())
                } else {
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
            }
            is RobertResultData.Failure -> {
                RobertResult.Failure(configResult.error)
            }
        }
    }

    override suspend fun generateCaptcha(type: String, local: String): RobertResultData<String> =
        remoteServiceRepository.generateCaptcha(apiVersion, type, local)

    override suspend fun getCaptchaImage(captchaId: String,
        path: String): RobertResult = remoteServiceRepository.getCaptchaImage(apiVersion, captchaId, path)

    override suspend fun getCaptchaAudio(captchaId: String,
        path: String): RobertResult = remoteServiceRepository.getCaptchaAudio(apiVersion, captchaId, path)

    override suspend fun registerV2(application: RobertApplication, captcha: String, captchaId: String): RobertResult {
        val registerResult = remoteServiceRepository.registerV2(apiVersion, captcha, captchaId)
        return when (registerResult) {
            is RobertResultData.Success -> {
                finishRegister(application, registerResult.data)
            }
            is RobertResultData.Failure -> {
                clearLocalData(application)
                RobertResult.Failure(registerResult.error)
            }
        }
    }

    private suspend fun finishRegister(application: RobertApplication, registerReport: RegisterReport): RobertResult {
        return try {
            keystoreRepository.timeStart = registerReport.timeStart
            keystoreRepository.atRiskLastRefresh = System.currentTimeMillis()
            ephemeralBluetoothIdentifierRepository.save(Base64.decode(registerReport.tuples, Base64.NO_WRAP))
            startStatusWorker(application.getAppContext())
            activateProximity(application)
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

    private fun handleConfigChange(configuration: List<Configuration>?) {
        val newConfigVersion = (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.CONFIG_VERSION
        }?.value as? Number ?: 0).toInt()

        if (newConfigVersion <= keystoreRepository.configVersion ?: -1) {
            return
        }

        keystoreRepository.configVersion = newConfigVersion

        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.CALIBRATION
        }?.value as? List<*>)?.let { calibrations ->
            val gson = Gson()
            val typeToken = object : TypeToken<List<DeviceParameterCorrection>>() {}.type
            keystoreRepository.calibration = gson.fromJson(gson.toJson(calibrations), typeToken)
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.FILTER_CONFIG
        }?.value as? String)?.let { filterConfig ->
            keystoreRepository.filteringConfig = filterConfig
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.FILTER_MODE
        }?.value as? String)?.let { filterMode ->
            keystoreRepository.filteringMode = filterMode
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
            it.name == RobertConstant.CONFIG.MIN_STATUS_RETRY_DURATION
        }?.value as? Number)?.let { minStatusRetryDuration ->
            keystoreRepository.minStatusRetryDuration = minStatusRetryDuration.toFloat()
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.CHECK_STATUS_FREQUENCY
        }?.value as? Number)?.let { checkStatusFrequency ->
            keystoreRepository.checkStatusFrequencyHour = checkStatusFrequency.toFloat()
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.RANDOM_STATUS_HOUR
        }?.value as? Number)?.let { randomStatusHour ->
            keystoreRepository.randomStatusHour = randomStatusHour.toFloat()
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
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.API_VERSION
        }?.value as? String)?.let { apiVersion ->
            keystoreRepository.apiVersion = apiVersion
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.QR_CODE_DELETION_HOURS
        }?.value as? Number)?.let { deletionHours ->
            keystoreRepository.qrCodeDeletionHours = deletionHours.toFloat()
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.QR_CODE_EXPIRED_HOURS
        }?.value as? Number)?.let { expiredHours ->
            keystoreRepository.qrCodeExpiredHours = expiredHours.toFloat()
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.QR_CODE_FORMATTED_STRING
        }?.value as? String)?.let { formattedString ->
            keystoreRepository.qrCodeFormattedString = formattedString
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.QR_CODE_FORMATTED_STRING_DISPLAYED
        }?.value as? String)?.let { formattedStringDisplayed ->
            keystoreRepository.qrCodeFormattedStringDisplayed = formattedStringDisplayed
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.QR_CODE_FOOTER_STRING
        }?.value as? String)?.let { footerString ->
            keystoreRepository.qrCodeFooterString = footerString
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.DISPLAY_DEPARTMENT_LEVEL
        }?.value as? Boolean)?.let { displayDepartmentLevel ->
            keystoreRepository.displayDepartmentLevel = displayDepartmentLevel
        }
        (configuration?.firstOrNull {
            it.name == RobertConstant.CONFIG.PROXIMITY_REACTICATION_REMINDER_HOURS
        }?.value as? List<*>)?.let { proximityReactivateReminderHours ->
            val gson = Gson()
            val typeToken = object : TypeToken<List<Int>>() {}.type
            keystoreRepository.proximityReactivationReminderHours = gson.fromJson(gson.toJson(proximityReactivateReminderHours), typeToken)
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

    @OptIn(ExperimentalTime::class)
    override suspend fun updateStatus(robertApplication: RobertApplication): RobertResult {
        return withContext(Dispatchers.IO) {
            refreshConfig(robertApplication)
            statusSemaphore.withPermit {
                val ssu = getSSU(RobertConstant.PREFIX.C2)
                val checkStatusFrequencyHourMs = Duration.convert(
                    checkStatusFrequencyHour.toDouble(),
                    DurationUnit.HOURS,
                    DurationUnit.MILLISECONDS
                )
                val lastSuccessLongEnough =
                    abs((atRiskLastRefresh ?: 0L) - System.currentTimeMillis()) > checkStatusFrequencyHourMs
                val minStatusRetryDurationMs = Duration.convert(
                    minStatusRetryDuration.toDouble(),
                    DurationUnit.HOURS,
                    DurationUnit.MILLISECONDS
                )
                val lastErrorLongEnough =
                    abs((keystoreRepository.atRiskLastError ?: 0L) - System.currentTimeMillis()) > minStatusRetryDurationMs
                val shouldRefreshStatus = lastSuccessLongEnough || lastErrorLongEnough
                if (shouldRefreshStatus) {
                    if (ssu is RobertResultData.Success) {
                        val result = remoteServiceRepository.status(apiVersion, ssu.data)
                        when (result) {
                            is RobertResultData.Success -> {
                                try {
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
                                    keystoreRepository.atRiskLastRefresh = System.currentTimeMillis()
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
                        RobertResult.Failure(UnknownException())
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
                (keystoreRepository.dataRetentionPeriod
                    ?: RobertConstant.DATA_RETENTION_PERIOD).toLong()
            )).unixTimeMsToNtpTimeS()
        localProximityRepository.removeUntilTime(localProximityExpiredTime)
    }

    override suspend fun report(token: String, firstSymptoms: Int?, positiveTest: Int?, application: RobertApplication): RobertResult {
        // Max take hello 14 days from now
        var originDayInPast = (keystoreRepository.dataRetentionPeriod ?: RobertConstant.DATA_RETENTION_PERIOD).toLong()
        when {
            firstSymptoms != null -> {
                // if symptoms take `preSymptomsSpan` days before first symptoms
                val preSymptomsSpan = (keystoreRepository.preSymptomsSpan ?: RobertConstant.PRE_SYMPTOMS_SPAN).toLong()
                originDayInPast = min(originDayInPast, firstSymptoms.toLong() + preSymptomsSpan)
            }
            positiveTest != null -> {
                // if positive test take `positiveSampleSpan` days before positive test
                val positiveSampleSpan = (keystoreRepository.positiveSampleSpan ?: RobertConstant.POSITIVE_SAMPLE_SPAN).toLong()
                originDayInPast = min(originDayInPast, positiveTest.toLong() + positiveSampleSpan)
            }
        }
        val firstProximityToSendTime = (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(originDayInPast))
            .unixTimeMsToNtpTimeS()

        val localProximityList = localProximityRepository.getUntilTime(firstProximityToSendTime)
        val filteredLocalProximityList = localProximityFilter.filter(
            localProximityList,
            filteringMode,
            filteringConfig
        )

        val result = remoteServiceRepository.report(apiVersion, token, filteredLocalProximityList)
        return when (result) {
            is RobertResult.Success -> {
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
        keystoreRepository.kA = null
        keystoreRepository.kEA = null
        keystoreRepository.timeStart = null
        keystoreRepository.lastRiskReceivedDate = null
        keystoreRepository.atRiskLastRefresh = null
        keystoreRepository.lastExposureTimeframe = null
        keystoreRepository.proximityActive = null
        keystoreRepository.isSick = null
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
        isAtRisk.toAtRiskStatus().let { atRisk ->
            if (atRiskMutableLiveData.value == null || atRiskMutableLiveData.value?.peekContent() != atRisk) {
                atRiskMutableLiveData.postValue(Event(atRisk))
            }
        }
    }
}
