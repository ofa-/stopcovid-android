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

class BleProximityNotification(
    private val bleScanner: BleScanner,
    private val bleAdvertiser: BleAdvertiser,
    private val bleGattManager: BleGattManager,
    private val settings: BleSettings,
    private val coroutineScope: CoroutineScope,
    private val coroutineContextProvider: CoroutineContextProvider = CoroutineContextProvider.Default()
) : ProximityNotification {

    companion object {
        private val TAG: String = BleProximityNotification::class.java.simpleName

        private const val VERSION = 1
    }

    private val bleRecordProviderForScanWithPayload = RecordProviderForScanWithPayload()
    private val bleRecordProviderForScanWithoutPayload = RecordProviderForScanWithoutPayload(settings)
    private val bleRecordMapper = BleRecordMapper(settings)

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
        }
    }

    private fun startAdvertiser() {
        bleAdvertiser.start(
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
    }

    private fun startGattServer() {
        bleGattManager.start(callback = object : BleGattManager.Callback {
            override fun onWritePayloadRequest(device: BluetoothDevice, value: ByteArray) {

                coroutineScope.launch(coroutineContextProvider.default) {

                    decodePayload(value)?.let { payload ->

                        // Try to match record from payload
                        bleRecordProviderForScanWithoutPayload.fromPayload(device, payload) ?: run {

                            // If not try to request remote rssi to complete record
                            bleGattManager.requestRemoteRssi(device)?.let { rssi ->
                                val scannedDevice =
                                    BleScannedDevice(device = device, rssi = rssi)
                                bleRecordProviderForScanWithoutPayload.fromScan(scannedDevice, payload)
                            }
                        }
                    }?.let { notifyProximity(it) }
                }
            }
        })
    }

    private fun startScanner() {
        bleScanner.start(callback = object : BleScanner.Callback {
            override fun onResult(results: List<BleScannedDevice>) {

                if (results.isNotEmpty()) {
                    coroutineScope.launch(coroutineContextProvider.default) {
                        results.mapNotNull { scannedDevice ->
                            val serviceData = scannedDevice.serviceData

                            if (serviceData != null) {
                                // Android case
                                decodePayload(serviceData)
                                    ?.let { bleRecordProviderForScanWithPayload.fromScan(scannedDevice, it) }
                            } else {
                                // iOS case
                                bleRecordProviderForScanWithoutPayload.fromScan(scannedDevice, null)
                            }
                        }.forEach { notifyProximity(it) }
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

    private fun decodePayload(value: ByteArray) = BlePayload.fromOrNull(value)

    private fun buildPayload() = BlePayload(
        proximityPayload = runBlocking { proximityPayloadProvider.current() },
        version = VERSION,
        txPowerLevel = settings.txCompensationGain
    ).toByteArray()

}



