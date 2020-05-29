/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Lunabee Studio / Date - 2020/04/05 - for the STOP-COVID project
 */

package com.lunabeestudio.framework.ble.service

import androidx.annotation.WorkerThread
import com.lunabeestudio.domain.extension.ntpTimeSToUnixTimeMs
import com.lunabeestudio.domain.model.DeviceParameterCorrection
import com.lunabeestudio.framework.ble.extension.toLocalProximity
import com.lunabeestudio.robert.RobertManager
import com.lunabeestudio.robert.extension.splitToByteArray
import com.lunabeestudio.robert.model.BLEAdvertiserException
import com.lunabeestudio.robert.model.BLEProximityNotificationException
import com.lunabeestudio.robert.model.BLEScannerException
import com.lunabeestudio.robert.model.NoEphemeralBluetoothIdentifierFound
import com.lunabeestudio.robert.model.NoEphemeralBluetoothIdentifierFoundForEpoch
import com.lunabeestudio.robert.model.ProximityException
import com.lunabeestudio.robert.model.RobertException
import com.lunabeestudio.robert.model.RobertResultData
import com.orange.proximitynotification.ProximityInfo
import com.orange.proximitynotification.ProximityNotificationError
import com.orange.proximitynotification.ProximityNotificationService
import com.orange.proximitynotification.ProximityPayload
import com.orange.proximitynotification.ble.BleSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

abstract class RobertProximityService : ProximityNotificationService() {

    abstract val robertManager: RobertManager

    private var payloadUpdateSchedulerScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    final override suspend fun current(): ProximityPayload {
        return withContext(Dispatchers.IO) {
            getProximityPayload()
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            start()
        } catch (e: Exception) {
            Timber.e(e)
            onError(ProximityException(e.cause, e.localizedMessage ?: "An error occurs in BLE proximity"))
        }
    }

    override fun onProximity(proximityInfo: ProximityInfo) {
        CoroutineScope(Dispatchers.IO).launch {
            proximityInfo.toLocalProximity()?.let { robertManager.storeLocalProximity(it) }
        }
    }

    override fun doStop() {
        super.doStop()
        payloadUpdateSchedulerScope.cancel("Proximity service has been stopped")
    }

    override val bleSettings: BleSettings
        get() {
            Timber.d("Fetch new BLE settings")

            val deviceParameterCorrection = robertManager.calibration.firstOrNull {
                it.device_handset_model == android.os.Build.MODEL
            } ?: robertManager.calibration.firstOrNull {
                it.device_handset_model == "DEFAULT"
            } ?: DeviceParameterCorrection("", FALLBACK_TX, FALLBACK_RX)

            return BleSettings(
                serviceUuid = UUID.fromString(robertManager.serviceUUID),
                servicePayloadCharacteristicUuid = UUID.fromString(robertManager.characteristicUUID),
                backgroundServiceManufacturerDataIOS = robertManager.backgroundServiceManufacturerData.splitToByteArray(),
                txCompensationGain = deviceParameterCorrection.tx_RSS_correction_factor.toInt(),
                rxCompensationGain = deviceParameterCorrection.rx_RSS_correction_factor.toInt()
            )
        }

    @WorkerThread
    private fun getProximityPayload(): ProximityPayload {
        val result = robertManager.getCurrentHelloBuilder()
        return when (result) {
            is RobertResultData.Success -> {
                val helloBuilder = result.data
                val proximityPayload = try {
                    ProximityPayload(helloBuilder.build().data)
                } catch (e: IllegalArgumentException) {
                    val exception = NoEphemeralBluetoothIdentifierFound(e.localizedMessage)
                    throw exception
                }

                scheduleNextUpdate(helloBuilder.isValidUntil.ntpTimeSToUnixTimeMs())
                proximityPayload
            }
            is RobertResultData.Failure -> {
                val exception = NoEphemeralBluetoothIdentifierFoundForEpoch()
                throw exception
            }
        }
    }

    private fun scheduleNextUpdate(validUntilTimeMs: Long) {
        val nextPayloadUpdateDelay =
            (validUntilTimeMs - System.currentTimeMillis())
                .coerceAtLeast(0)
                .coerceAtMost(HELLO_REFRESH_MAX_DELAY_MS)

        payloadUpdateSchedulerScope.launch {
            Timber.d("Next payload update in ${nextPayloadUpdateDelay}ms")
            delay(nextPayloadUpdateDelay)
            try {
                notifyProximityPayloadUpdated()
            } catch (e: Exception) {
                if (e is RobertException) {
                    onError(e)
                } else {
                    onError(ProximityException(e))
                }
            }
        }.invokeOnCompletion { error ->
            if (error != null && error !is CancellationException) {
                onError(ProximityException(error))
            }
        }
    }

    final override fun onError(error: ProximityNotificationError) {
        onError(when (error.type) {
            ProximityNotificationError.Type.BLE_ADVERTISER -> BLEAdvertiserException("(${error.rootErrorCode})")
            ProximityNotificationError.Type.BLE_SCANNER -> BLEScannerException("(${error.rootErrorCode})")
            ProximityNotificationError.Type.BLE_PROXIMITY_NOTIFICATION -> BLEProximityNotificationException("(${error.cause})")
        })
    }

    abstract fun onError(error: RobertException)

    companion object {
        private const val HELLO_REFRESH_MAX_DELAY_MS: Long = 30 * 1000
        private const val FALLBACK_TX: Double = -6.52
        private const val FALLBACK_RX: Double = -19.71
    }
}

