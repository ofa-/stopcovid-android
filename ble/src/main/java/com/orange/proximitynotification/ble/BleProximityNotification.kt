/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble

import android.bluetooth.BluetoothDevice
import com.orange.proximitynotification.ProximityInfo
import com.orange.proximitynotification.ProximityNotification
import com.orange.proximitynotification.ProximityNotificationCallback
import com.orange.proximitynotification.ProximityNotificationError
import com.orange.proximitynotification.ProximityPayloadProvider
import com.orange.proximitynotification.ble.advertiser.BleAdvertiser
import com.orange.proximitynotification.ble.gatt.BleGattManager
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import com.orange.proximitynotification.ble.scanner.BleScanner
import com.orange.proximitynotification.tools.CoroutineContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class BleProximityNotification(
    private val bleScanner: BleScanner,
    private val bleAdvertiser: BleAdvertiser,
    private val bleGattManager: BleGattManager,
    private val settings: BleSettings,
    private val coroutineScope: CoroutineScope,
    private val coroutineContextProvider: CoroutineContextProvider = CoroutineContextProvider.Default()
) : ProximityNotification {

    companion object {
        private const val VERSION = 1
    }

    private val bleRecordProviderForScanWithPayload = RecordProviderForScanWithPayload()
    private val bleRecordProviderForScanWithoutPayload =
        RecordProviderForScanWithoutPayload(settings)
    private val bleRecordMapper = BleRecordMapper(settings)
    private val bleScannedDeviceFilter = BleScannedDeviceFilter()

    private lateinit var proximityPayloadProvider: ProximityPayloadProvider
    private lateinit var callback: ProximityNotificationCallback

    private var _isRunning = false
    override val isRunning: Boolean
        get() = _isRunning

    override fun setUp(
        proximityPayloadProvider: ProximityPayloadProvider,
        callback: ProximityNotificationCallback
    ) {
        this.proximityPayloadProvider = proximityPayloadProvider
        this.callback = callback
    }

    override fun start() {
        startAdvertiser()
        startGattServer()
        startScanner()

        _isRunning = true
    }

    override fun stop() {
        _isRunning = false

        stopScanner()
        stopGattServer()
        stopAdvertiser()
    }

    override fun notifyPayloadUpdated() {
        if (isRunning) {
            stopAdvertiser()
            startAdvertiser()
            startScanner() // make onProximity fire also while sleeping
        }
    }

    private fun startAdvertiser() {
        val status = bleAdvertiser.start(
            data = buildPayload(),
            callback = object : BleAdvertiser.Callback {
                override fun onError(errorCode: Int) {
                    callback.onError(
                        ProximityNotificationError(
                            ProximityNotificationError.Type.BLE_ADVERTISER,
                            errorCode
                        )
                    )
                }
            })

        if (!status) {
            callback.onError(
                ProximityNotificationError(
                    ProximityNotificationError.Type.BLE_ADVERTISER,
                    cause = "Failed to start advertiser"
                )
            )
        }
    }

    private fun startGattServer() {
        val status = bleGattManager.start(callback = object : BleGattManager.Callback {
            override suspend fun onWritePayloadRequest(device: BluetoothDevice, value: ByteArray) {

                withContext(coroutineContextProvider.default) {

                    decodePayload(value)?.let { payload ->

                        // Try to match record from payload
                        bleRecordProviderForScanWithoutPayload.fromPayload(device, payload) ?: run {

                            // If not try to request remote rssi to complete record
                            bleGattManager.requestRemoteRssi(device, false)?.let { rssi ->
                                val scannedDevice =
                                    BleScannedDevice(device = device, rssi = rssi)
                                bleRecordProviderForScanWithPayload.fromScan(
                                    scannedDevice,
                                    payload
                                )
                            }
                        }
                    }?.let {
                        // Notify in another coroutine in order to free the gatt callback
                        coroutineScope.launch(coroutineContextProvider.default) { notifyProximity(it) }
                    }
                }
            }
        })

        if (!status) {
            callback.onError(
                ProximityNotificationError(
                    ProximityNotificationError.Type.BLE_GATT,
                    cause = "Failed to start GATT"
                )
            )
        }

    }

    private fun startScanner() {
        val status = bleScanner.start(callback = object : BleScanner.Callback {
            override fun onResult(results: List<BleScannedDevice>) {
                if (results.isNotEmpty()) {
                    coroutineScope.launch(coroutineContextProvider.default) {
                        handleScanResults(bleScannedDeviceFilter.filter(results))
                    }
                }
            }

            override fun onError(errorCode: Int) {
                callback.onError(
                    ProximityNotificationError(
                        ProximityNotificationError.Type.BLE_SCANNER,
                        errorCode
                    )
                )
            }
        })

        if (!status) {
            callback.onError(
                ProximityNotificationError(
                    ProximityNotificationError.Type.BLE_SCANNER,
                    cause = "Failed to start scanner"
                )
            )
        }
    }

    private fun stopAdvertiser() {
        bleAdvertiser.stop()
    }

    private fun stopGattServer() {
        bleGattManager.stop()
    }

    private fun stopScanner() {
        bleScanner.stop()
    }

    private fun notifyProximity(bleRecord: BleRecord) {
        notifyProximity(bleRecordMapper.toProximityInfo(bleRecord))
    }

    private fun notifyProximity(proximityInfo: ProximityInfo) {
        callback.onProximity(proximityInfo)
    }

    private suspend fun handleScanResults(results: List<BleScannedDevice>) =
        withContext(coroutineContextProvider.default) {

            results.mapNotNull { scannedDevice ->
                val serviceData = scannedDevice.serviceData

                if (serviceData != null) {
                    // Android case
                    decodePayload(serviceData)?.let {
                        bleRecordProviderForScanWithPayload.fromScan(scannedDevice, it)
                    }
                } else {
                    // iOS case
                    bleRecordProviderForScanWithoutPayload.fromScan(scannedDevice, null)
                }
            }.forEach { notifyProximity(it) }
        }

    private fun decodePayload(value: ByteArray) = BlePayload.fromOrNull(value)

    private fun buildPayload() = BlePayload(
        proximityPayload = runBlocking { proximityPayloadProvider.current() },
        version = VERSION,
        txPowerLevel = settings.txCompensationGain
    ).toByteArray()

}



