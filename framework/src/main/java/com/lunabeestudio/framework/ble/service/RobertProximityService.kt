/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the TOUS-ANTI-COVID project
 */

package com.lunabeestudio.framework.ble.service

import androidx.annotation.WorkerThread
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeMs
import com.lunabeestudio.domain.model.DeviceParameterCorrection
import com.lunabeestudio.domain.model.Hello
import com.lunabeestudio.domain.model.HelloBuilder
import com.lunabeestudio.framework.ble.extension.toLocalProximity
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.extension.splitToByteArray
import com.lunabeestudio.robert.model.BLEAdvertiserException
import com.lunabeestudio.robert.model.BLEGattException
import com.lunabeestudio.robert.model.BLEScannerException
import com.lunabeestudio.robert.model.InvalidEphemeralBluetoothIdentifierForEpoch
import com.lunabeestudio.robert.model.NoEphemeralBluetoothIdentifierFoundForEpoch
import com.lunabeestudio.robert.model.ProximityException
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.robert.model.RobertResultData
import com.orange.proximitynotification.ProximityInfo
import com.orange.proximitynotification.ProximityNotificationError
import com.orange.proximitynotification.ProximityNotificationEvent
import com.orange.proximitynotification.ProximityNotificationService
import com.orange.proximitynotification.ProximityPayload
import com.orange.proximitynotification.ProximityPayloadId
import com.orange.proximitynotification.ble.BleSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

abstract class RobertProximityService : ProximityNotificationService() {

    abstract val robertManager: RobertManager
    protected abstract fun sendErrorBluetoothNotification()
    protected open fun useProximityBleIds(): Boolean = false

    private var payloadUpdateSchedulerJob: Job? = null

    // Clear errors if the service is working as expected
    protected var noNewErrorJob: Job? = null

    // Help to distinguish between between error at the beginning of the service and after some time
    protected var creationDate: Long = System.currentTimeMillis()

    override val exceptionHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, t ->
        handleBleException(t)
    }

    final override suspend fun current(): ProximityPayload {
        return withContext(Dispatchers.IO) {
            getProximityPayload(true)
        }
    }

    override suspend fun fromProximityPayload(proximityPayload: ProximityPayload): ProximityPayloadId? {
        return withContext(Dispatchers.Default) {
            Hello(proximityPayload.data).ebidArray
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            creationDate = System.currentTimeMillis()
            start()
        } catch (e: Exception) {
            handleBleException(e)
        }
    }

    private fun handleBleException(t: Throwable) {
        Timber.e(t)
        val robertException = t as? RobertException ?: ProximityException(
            t.cause,
            t.localizedMessage ?: "An error occurred in BLE proximity"
        )
        onError(robertException)
        stop()
    }

    override suspend fun onProximity(proximityInfo: ProximityInfo) {
        withContext(Dispatchers.IO) {
            proximityInfo.toLocalProximity()?.let { robertManager.storeLocalProximity(it) }
        }
    }

    override fun onBluetoothDisabled() {
        super.onBluetoothDisabled()
        if (!isBluetoothRestartInProgress) {
            sendErrorBluetoothNotification()
        }
    }

    override val bleSettings: BleSettings
        get() {
            Timber.v("Fetch new BLE settings")

            val deviceParameterCorrection = robertManager.configuration.calibration.firstOrNull {
                it.deviceHandsetModel == android.os.Build.MODEL
            } ?: robertManager.configuration.calibration.firstOrNull {
                it.deviceHandsetModel == "DEFAULT"
            } ?: DeviceParameterCorrection("", FALLBACK_TX, FALLBACK_RX)

            val useScannerHardwareBatching =
                robertManager.configuration.dontUseScannerHardwareBatching?.contains(android.os.Build.MODEL) != true

            val serviceUUID: String = if (useProximityBleIds()) {
                "0000f061-0000-1000-8000-00805f9b34fb"
            } else {
                robertManager.configuration.serviceUUID
            }
            val backgroundServiceManufacturerData: String = if (useProximityBleIds()) {
                "1.0.0.0.0.0.0.0.0.0.64.0.0.0.0.0.0"
            } else {
                robertManager.configuration.backgroundServiceManufacturerData
            }

            val settings = BleSettings(
                serviceUuid = UUID.fromString(serviceUUID),
                servicePayloadCharacteristicUuid = UUID.fromString(robertManager.configuration.characteristicUUID),
                backgroundServiceManufacturerDataIOS = backgroundServiceManufacturerData.splitToByteArray(),
                txCompensationGain = deviceParameterCorrection.txRssCorrectionFactor.toInt(),
                rxCompensationGain = deviceParameterCorrection.rxRssCorrectionFactor.toInt(),
                useScannerHardwareBatching = useScannerHardwareBatching
            )

            robertManager.shouldReloadBleSettings = false

            return settings
        }

    @WorkerThread
    private suspend fun getProximityPayload(canRetry: Boolean): ProximityPayload {
        val result = robertManager.getCurrentHelloBuilder()
        return when (result) {
            is RobertResultData.Success -> {
                val helloBuilder = result.data
                val proximityPayload = try {
                    proximityPayload(helloBuilder)
                } catch (e: IllegalArgumentException) {
                    if (canRetry) {
                        getProximityPayload(false)
                    } else {
                        val exception =
                            InvalidEphemeralBluetoothIdentifierForEpoch(e.localizedMessage)
                        throw exception
                    }
                }
                proximityPayload
            }
            is RobertResultData.Failure -> {
                val exception = NoEphemeralBluetoothIdentifierFoundForEpoch()
                throw exception
            }
        }
    }

    private fun proximityPayload(helloBuilder: HelloBuilder) =
        ProximityPayload(helloBuilder.build().data)

    override fun doStart() {
        super.doStart()

        if (!isBluetoothRestartInProgress) {
            launchRefreshBle()
        }

    }

    override fun doStop() {
        super.doStop()

        if (!isBluetoothRestartInProgress) {
            payloadUpdateSchedulerJob?.cancel()
        }
    }

    private fun launchRefreshBle() {
        payloadUpdateSchedulerJob?.cancel()
        payloadUpdateSchedulerJob = launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val result = robertManager.getCurrentHelloBuilder()
                    when (result) {
                        is RobertResultData.Success -> {
                            val helloBuilder = result.data
                            val validUntilTimeMs = helloBuilder.isValidUntil.ntpTimeSToUnixTimeMs()
                            val nextPayloadUpdateDelay: Long =
                                (validUntilTimeMs - System.currentTimeMillis())
                                    .coerceAtLeast(0)
                                    .coerceAtMost(HELLO_REFRESH_MAX_DELAY_MS)
                            Timber.v("Next payload update in ${nextPayloadUpdateDelay}ms")
                            delay(nextPayloadUpdateDelay)
                            try {
                                val shouldRestartProximityService: Boolean =
                                    robertManager.shouldReloadBleSettings
                                        || (RESTART_SERVICE_ON_EBID_CHANGE && validUntilTimeMs - System.currentTimeMillis() < 0L)
                                startWaitForErrorOrClear()
                                if (shouldRestartProximityService) {
                                    restart()
                                } else {
                                    notifyProximityPayloadUpdated(current())
                                }
                            } catch (_: CancellationException) {
                                // no-op
                                Timber.v("Coroutines was cancelled inside of launchRefreshBle loop")
                            } catch (t: Throwable) {
                                Timber.e(t, "Error inside of launchRefreshBle loop")
                                val robertException = t as? RobertException ?: ProximityException(t)
                                onError(robertException)
                            }
                        }
                        is RobertResultData.Failure -> {
                            throw NoEphemeralBluetoothIdentifierFoundForEpoch()
                        }
                    }
                }
            } catch (_: CancellationException) {
                // no-op
                Timber.v("Coroutines was cancelled outside of launchRefreshBle loop")
            } catch (t: Throwable) {
                Timber.e(t, "Error outside of launchRefreshBle loop")
                val robertException = t as? RobertException ?: ProximityException(t)
                onError(robertException)
            }
        }
    }

    private fun startWaitForErrorOrClear() {
        noNewErrorJob?.cancel()
        noNewErrorJob = launch(Dispatchers.Main) {
            delay(CLEAR_ERROR_DELAY_MS)
            if (isActive) {
                // No new error detected, we can clear the errors
                clearErrorNotification()
            }
        }
    }

    abstract fun clearErrorNotification()

    final override suspend fun onError(error: ProximityNotificationError) {
        withContext(Dispatchers.Main) {
            onError(
                when (error.type) {
                    ProximityNotificationError.Type.BLE_ADVERTISER -> BLEAdvertiserException(
                        "(${error.cause} [${error.rootErrorCode}])",
                        shouldRestartBle = error.rootErrorCode == ProximityNotificationError.UNHEALTHY_BLUETOOTH_ERROR_CODE
                    )
                    ProximityNotificationError.Type.BLE_SCANNER -> BLEScannerException("(${error.cause} [${error.rootErrorCode}])")
                    ProximityNotificationError.Type.BLE_GATT -> BLEGattException("(${error.cause} [${error.rootErrorCode}])")
                }
            )
        }
    }

    abstract fun onError(error: RobertException)

    override fun onEvent(event: ProximityNotificationEvent) {
        when (event) {
            is ProximityNotificationEvent.Verbose -> {
                Timber.v("BLE-SDK-${event.id.name}-${event.message}")
            }
            is ProximityNotificationEvent.Debug -> {
                Timber.d("BLE-SDK-${event.id.name}-${event.message}")
            }
            is ProximityNotificationEvent.Info -> {
                Timber.i("BLE-SDK-${event.id.name}-${event.message}")
            }
            is ProximityNotificationEvent.Error -> {
                Timber.e(event.cause, "BLE-SDK-${event.id.name}-${event.message}")
            }
        }
    }

    companion object {
        private const val HELLO_REFRESH_MAX_DELAY_MS: Long = 30 * 1000
        private const val CLEAR_ERROR_DELAY_MS: Long = 1 * 1000
        private const val RESTART_SERVICE_ON_EBID_CHANGE: Boolean = true
        private const val FALLBACK_TX: Double = -6.52
        private const val FALLBACK_RX: Double = -19.71
    }
}

