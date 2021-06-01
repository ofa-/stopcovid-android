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

import android.bluetooth.BluetoothDevice
import com.orange.proximitynotification.ProximityNotificationError
import com.orange.proximitynotification.ProximityPayload
import com.orange.proximitynotification.ble.advertiser.BleAdvertiser
import com.orange.proximitynotification.ble.calibration.BleRssiCalibration
import com.orange.proximitynotification.ble.gatt.BleGattManager
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import com.orange.proximitynotification.ble.scanner.BleScanner
import com.orange.proximitynotification.tools.CoroutineContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * [BleProximityNotification] implementation using [BleAdvertiser] to broadcast payload
 */
internal class BleProximityNotificationWithAdvertiser(
    private val bleAdvertiser: BleAdvertiser,
    private val bleGattManager: BleGattManager,
    private val bleScanner: BleScanner,
    settings: BleSettings,
    coroutineScope: CoroutineScope,
    coroutineContextProvider: CoroutineContextProvider = CoroutineContextProvider.Default(),
) : BleProximityNotification(settings, coroutineScope, coroutineContextProvider) {

    override val shouldRestartBluetooth = false
    override val couldRestartFrequently = true

    override val bleRecordProvider: BleRecordProvider
        get() = bleRecordProviderForScanWithoutPayload

    private val bleRecordProviderForScanWithoutPayload = BleRecordProviderForScanWithoutPayload(
        maxCacheSize = settings.maxCacheSize,
        scanCacheTimeout = settings.scanCacheTimeout,
        payloadCacheTimeout = settings.identityCacheTimeout
    )

    override suspend fun start() {
        startGattServer()
        startAdvertiser()
        startScanner()

        super.start()
    }

    override suspend fun stop() {
        super.stop()

        stopScanner()
        stopAdvertiser()
        stopGattServer()
    }

    override suspend fun notifyPayloadUpdated(proximityPayload: ProximityPayload) {
        if (!isRunning) return

        stopAdvertiser()
        startAdvertiser(proximityPayload)
    }

    private suspend fun startAdvertiser() {
        startAdvertiser(proximityPayloadProvider.current())
    }

    private suspend fun startAdvertiser(proximityPayload: ProximityPayload) {
        if (!doStartAdvertiser(proximityPayload)) {
            notifyError(
                ProximityNotificationError(
                    ProximityNotificationError.Type.BLE_ADVERTISER,
                    cause = "Failed to start advertiser"
                )
            )
        }
    }

    private suspend fun doStartAdvertiser(proximityPayload: ProximityPayload): Boolean =
        withContext(coroutineContextProvider.main) {
            bleAdvertiser.start(
                data = buildPayload(proximityPayload).toByteArray(),
                callback = object : BleAdvertiser.Callback {
                    override fun onError(errorCode: Int) {
                        bleAdvertiser.stop()

                        notifyErrorAsync(
                            ProximityNotificationError(
                                ProximityNotificationError.Type.BLE_ADVERTISER,
                                errorCode
                            )
                        )
                    }
                })
        }

    private suspend fun startGattServer() {
        if (!doStartGattServer()) {
            notifyError(
                ProximityNotificationError(
                    ProximityNotificationError.Type.BLE_GATT,
                    cause = "Failed to start GATT"
                )
            )
        }
    }

    private suspend fun doStartGattServer() = withContext(coroutineContextProvider.main) {

        bleGattManager.start(object : BleGattManager.Callback {
            override fun onPayloadReceived(
                device: BluetoothDevice,
                payload: ByteArray
            ): BleGattManager.Callback.PayloadReceivedStatus {

                return when (val decodedPayload = decodePayload(payload)) {
                    null -> BleGattManager.Callback.PayloadReceivedStatus.INVALID_PAYLOAD
                    else -> handlePayload(device, decodedPayload)
                }
            }
        })
    }

    private fun handlePayload(
        device: BluetoothDevice,
        payload: BlePayload
    ): BleGattManager.Callback.PayloadReceivedStatus {

        // Payload with calibrated RSSI
        if (payload.calibratedRssi != null) {

            coroutineScope.launch(coroutineContextProvider.default) {
                val rssi = BleRssiCalibration.calibrate(
                    payload.calibratedRssi,
                    0,
                    settings.txCompensationGain
                )

                bleRecordProvider.buildRecord(
                    payload = payload,
                    rssi = rssi,
                    timestamp = Date(),
                    isRssiCalibrated = true
                ).let { notifyProximity(it) }
            }

            return BleGattManager.Callback.PayloadReceivedStatus.PAYLOAD_HANDLED
        }

        // Try to match record from payload
        bleRecordProviderForScanWithoutPayload.fromPayload(device, payload)?.let {
            // Notify in another coroutine in order to free callback
            coroutineScope.launch(coroutineContextProvider.default) { notifyProximity(it) }
            return BleGattManager.Callback.PayloadReceivedStatus.PAYLOAD_HANDLED
        }

        // If not try to request remote rssi to complete record
        coroutineScope.launch(coroutineContextProvider.default) {
            bleGattManager.requestRemoteRssi(device).valueOrNull()?.let { rssi ->
                val scan = BleScannedDevice(device = device, rssi = rssi)
                notifyProximity(bleRecordProviderForScanWithoutPayload.fromScanAndPayload(scan, payload))
            }
        }

        return BleGattManager.Callback.PayloadReceivedStatus.UNKNOWN_DEVICE_REQUEST_RSSI_NEEDED
    }

    private suspend fun startScanner() {
        if (!doStartScanner()) {
            notifyError(
                ProximityNotificationError(
                    ProximityNotificationError.Type.BLE_SCANNER,
                    cause = "Failed to start scanner"
                )
            )
        }
    }

    private suspend fun doStartScanner() = withContext(coroutineContextProvider.main) {
        bleScanner.start(callback = object : BleScanner.Callback {
            override fun onResult(results: List<BleScannedDevice>) {
                checkAndHandleScanResults(results)
            }

            override fun onError(errorCode: Int) {
                bleScanner.stop()

                notifyErrorAsync(
                    ProximityNotificationError(
                        ProximityNotificationError.Type.BLE_SCANNER,
                        errorCode
                    )
                )
            }
        })
    }


    override suspend fun handleScanResults(results: List<BleScannedDevice>) {
        withContext(coroutineContextProvider.default) {
            results.forEach { scannedDevice ->
                val serviceData = scannedDevice.serviceData

                if (serviceData != null) {
                    // Android case
                    decodePayload(serviceData)?.let { notifyProximity(scannedDevice, it) }
                } else {
                    // iOS case
                    bleRecordProviderForScanWithoutPayload.fromScan(scannedDevice)
                        ?.let { notifyProximity(it) }
                }
            }
        }
    }


    private suspend fun stopAdvertiser() = withContext(coroutineContextProvider.main) {
        bleAdvertiser.stop()
    }

    private suspend fun stopGattServer() = withContext(coroutineContextProvider.main) {
        bleGattManager.stop()
    }

    private suspend fun stopScanner() = withContext(coroutineContextProvider.main) {
        bleScanner.stop()
    }

}



