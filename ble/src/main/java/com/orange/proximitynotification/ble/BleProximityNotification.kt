/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import com.orange.proximitynotification.ProximityInfo
import com.orange.proximitynotification.ProximityNotification
import com.orange.proximitynotification.ProximityNotificationCallback
import com.orange.proximitynotification.ProximityNotificationError
import com.orange.proximitynotification.ProximityPayload
import com.orange.proximitynotification.ProximityPayloadIdProvider
import com.orange.proximitynotification.ProximityPayloadProvider
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import com.orange.proximitynotification.tools.CoroutineContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal abstract class BleProximityNotification(
    protected val settings: BleSettings,
    protected val coroutineScope: CoroutineScope,
    protected val coroutineContextProvider: CoroutineContextProvider = CoroutineContextProvider.Default()
) : ProximityNotification {

    private val bleRecordMapper = BleRecordMapper(settings)
    private val bleScannedDeviceFilter = BleScannedDeviceFilter()

    protected lateinit var proximityPayloadProvider: ProximityPayloadProvider
    protected lateinit var proximityPayloadIdProvider: ProximityPayloadIdProvider
    private lateinit var callback: ProximityNotificationCallback

    protected abstract val bleRecordProvider : BleRecordProvider
    abstract val shouldRestartBluetooth: Boolean
    abstract val couldRestartFrequently: Boolean

    private var _isRunning = false
    override val isRunning: Boolean
        get() = _isRunning

    override fun setUp(
        proximityPayloadProvider: ProximityPayloadProvider,
        proximityPayloadIdProvider: ProximityPayloadIdProvider,
        callback: ProximityNotificationCallback
    ) {
        this.proximityPayloadProvider = proximityPayloadProvider
        this.proximityPayloadIdProvider = proximityPayloadIdProvider
        this.callback = callback
    }

    override suspend fun start() {
        _isRunning = true
    }

    override suspend fun stop() {
        _isRunning = false
    }

    protected suspend fun notifyProximity(scannedDevice: BleScannedDevice, payload: BlePayload) {
        withContext(coroutineContextProvider.default) {
            notifyProximity(bleRecordProvider.buildRecord(payload, scannedDevice))
        }
    }

    protected suspend fun notifyProximity(bleRecord: BleRecord) {
        withContext(coroutineContextProvider.default) {
            notifyProximity(bleRecordMapper.toProximityInfo(bleRecord))
        }
    }

    private suspend fun notifyProximity(proximityInfo: ProximityInfo) {
        callback.onProximity(proximityInfo)
    }

    protected suspend fun notifyError(error: ProximityNotificationError) {
        callback.onError(error)
    }

    protected fun notifyErrorAsync(error: ProximityNotificationError) {
        coroutineScope.launch { notifyError(error) }
    }

    protected fun checkAndHandleScanResults(results: List<BleScannedDevice>) {
        if (results.isNotEmpty()) {
            coroutineScope.launch(coroutineContextProvider.default) {
                handleScanResults(bleScannedDeviceFilter.filter(results))
            }
        }
    }

    protected abstract suspend fun handleScanResults(results: List<BleScannedDevice>)

    protected fun decodePayload(value: ByteArray) = BlePayload.fromOrNull(value)

    protected fun buildPayload(
        proximityPayload: ProximityPayload,
        calibratedRssi: Int? = null
    ): BlePayload = BlePayload(
        proximityPayload = proximityPayload,
        txPowerLevel = settings.txCompensationGain,
        calibratedRssi = calibratedRssi
    )
}




