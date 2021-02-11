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
import com.orange.proximitynotification.ble.gatt.BleGattManager
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import com.orange.proximitynotification.ble.scanner.BleScanner
import com.orange.proximitynotification.tools.CoroutineContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                data = buildPayload(proximityPayload),
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
        bleGattManager.start(callback = object : BleGattManager.Callback {
            override fun onPayloadReceived(
                device: BluetoothDevice,
                payload: ByteArray
            ): BleGattManager.Callback.PayloadReceivedStatus {

                val decodedPayload = decodePayload(payload)
                    ?: return BleGattManager.Callback.PayloadReceivedStatus.INVALID_PAYLOAD

                // Try to match record from payload
                bleRecordFromPayload(device, decodedPayload)?.let {
                    // Notify in another coroutine in order to free callback
                    coroutineScope.launch(coroutineContextProvider.default) { notifyProximity(it) }
                    return BleGattManager.Callback.PayloadReceivedStatus.PAYLOAD_HANDLED
                }

                // If not try to request remote rssi to complete record
                coroutineScope.launch(coroutineContextProvider.io) {
                    bleGattManager.requestRemoteRssi(device).valueOrNull()?.let { rssi ->
                        val scannedDevice = BleScannedDevice(device = device, rssi = rssi)
                        notifyProximity(scannedDevice, decodedPayload)
                    }
                }

                return BleGattManager.Callback.PayloadReceivedStatus.UNKNOWN_DEVICE_REQUEST_RSSI_NEEDED
            }
        })
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



