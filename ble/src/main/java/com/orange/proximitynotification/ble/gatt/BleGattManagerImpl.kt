/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import com.orange.proximitynotification.ProximityNotificationEventId
import com.orange.proximitynotification.ProximityNotificationLogger
import com.orange.proximitynotification.ble.BleSettings
import com.orange.proximitynotification.tools.CoroutineContextProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class BleGattManagerImpl(
    override val settings: BleSettings,
    private val context: Context,
    private val bluetoothManager: BluetoothManager,
    private val gattClientProvider: BleGattClientProvider,
    private val coroutineContextProvider: CoroutineContextProvider = CoroutineContextProvider.Default()
) : BleGattManager, CoroutineScope {

    private var job: Job? = null
    override val coroutineContext: CoroutineContext
        get() = coroutineContextProvider.io + (job ?: EmptyCoroutineContext)

    private val bleOperationLock = Mutex()

    private var bluetoothGattServer: BluetoothGattServer? = null

    // Characteristic used to read payload
    private val payloadCharacteristic = BluetoothGattCharacteristic(
        settings.servicePayloadCharacteristicUuid,
        BluetoothGattCharacteristic.PROPERTY_WRITE,
        BluetoothGattCharacteristic.PERMISSION_WRITE
    )

    override fun start(callback: BleGattManager.Callback): Boolean {

        ProximityNotificationLogger.info(
            ProximityNotificationEventId.BLE_GATT_START,
            "Starting GATT server"
        )

        doStop()
        return doStart(callback)
    }

    override fun stop() {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.BLE_GATT_STOP,
            "Stopping GATT server"
        )

        doStop()
    }

    private fun doStart(callback: BleGattManager.Callback): Boolean {
        job = SupervisorJob()

        return runCatching {
            bluetoothGattServer =
                bluetoothManager.openGattServer(context, GattServerCallback(callback))?.apply {
                    try {
                        clearServices()
                        addService(buildGattService())
                    } catch (t: Throwable) {
                        close()
                        throw t
                    }
                }

            if (bluetoothGattServer == null) {
                ProximityNotificationLogger.error(
                    eventId = ProximityNotificationEventId.BLE_GATT_START_ERROR,
                    message = "Failed to start GATT server (openGattServer returned null)"
                )
                false
            } else {
                ProximityNotificationLogger.info(
                    eventId = ProximityNotificationEventId.BLE_GATT_START_SUCCESS,
                    message = "Succeed to start GATT server"
                )
                true
            }

        }.onFailure {
            ProximityNotificationLogger.error(
                eventId = ProximityNotificationEventId.BLE_GATT_START_ERROR,
                message = "Failed to start GATT server",
                cause = it
            )
        }.getOrDefault(false)
    }

    private fun doStop() {

        bluetoothGattServer?.runCatching {
            clearServices()
            close()
        }?.onFailure {
            ProximityNotificationLogger.error(
                eventId = ProximityNotificationEventId.BLE_GATT_STOP_ERROR,
                message = "Failed to stop GATT server",
                cause = it
            )
        }?.onSuccess {
            ProximityNotificationLogger.info(
                eventId = ProximityNotificationEventId.BLE_GATT_STOP_SUCCESS,
                message = "Succeed to stop GATT server"
            )
        }
        bluetoothGattServer = null

        job?.cancel()
        job = null
    }

    override suspend fun requestRemoteRssi(device: BluetoothDevice, close: Boolean): Int? {
        return withContext(coroutineContextProvider.io) {
            val client = gattClientProvider.fromDevice(device)

            try {
                ProximityNotificationLogger.debug(
                    eventId = ProximityNotificationEventId.BLE_GATT_REQUEST_REMOTE_RSSI,
                    message = "Requesting remote RSSI"
                )

                withTimeout(settings.connectionTimeout) { client.open() }

                val rssi = bleOperation { client.readRemoteRssi() }

                ProximityNotificationLogger.debug(
                    eventId = ProximityNotificationEventId.BLE_GATT_REQUEST_REMOTE_RSSI_SUCCESS,
                    message = "Succeed to request remote RSSI"
                )

                rssi

            } catch (e: TimeoutCancellationException) {

                ProximityNotificationLogger.error(
                    eventId = ProximityNotificationEventId.BLE_GATT_REQUEST_REMOTE_RSSI_TIMEOUT,
                    message = "Failed to request remote RSSI. Connection timeout",
                    cause = e
                )

                null
            } catch (t: Throwable) {

                ProximityNotificationLogger.error(
                    eventId = ProximityNotificationEventId.BLE_GATT_REQUEST_REMOTE_RSSI_ERROR,
                    message = "Failed to request remote RSSI. Exception thrown",
                    cause = t
                )

                null
            } finally {
                if (close) {
                    bleOperation { client.close() }
                }
            }
        }

    }

    private suspend fun <T> bleOperation(operation: suspend () -> T): T {
        return bleOperationLock.withLock {
            withContext(coroutineContextProvider.main) {
                operation()
            }
        }
    }

    private fun buildGattService(): BluetoothGattService {
        return BluetoothGattService(
            settings.serviceUuid,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        ).apply {
            addCharacteristic(payloadCharacteristic)
        }
    }

    private inner class GattServerCallback(private val callback: BleGattManager.Callback) :
        BluetoothGattServerCallback() {

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            val exceptionHandler = CoroutineExceptionHandler { _, exception ->

                ProximityNotificationLogger.error(
                    eventId = ProximityNotificationEventId.BLE_GATT_ON_CHARACTERISTIC_WRITE_REQUEST_ERROR,
                    message = "onCharacteristicWriteRequest failed",
                    cause = exception
                )
            }

            val clonedValue = value?.copyOf()

            launch(coroutineContextProvider.io + exceptionHandler) {
                super.onCharacteristicWriteRequest(
                    device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value
                )

                ProximityNotificationLogger.debug(
                    eventId = ProximityNotificationEventId.BLE_GATT_ON_CHARACTERISTIC_WRITE_REQUEST,
                    message = "onCharacteristicWriteRequest"
                )

                val result = when (characteristic.uuid) {
                    payloadCharacteristic.uuid -> {
                        if (offset != 0) {
                            BluetoothGatt.GATT_INVALID_OFFSET
                        } else {
                            clonedValue?.let { callback.onWritePayloadRequest(device, it) }
                            BluetoothGatt.GATT_SUCCESS
                        }
                    }
                    else -> BluetoothGatt.GATT_FAILURE
                }

                ProximityNotificationLogger.debug(
                    eventId = ProximityNotificationEventId.BLE_GATT_ON_CHARACTERISTIC_WRITE_REQUEST_SUCCESS,
                    message = "onCharacteristicWriteRequest result=$result"
                )

                if (responseNeeded) {
                    bleOperation {
                        // It could happen that this call throws NullPointerException
                        // if client is disconnected
                        runCatching {
                            bluetoothGattServer?.sendResponse(
                                device,
                                requestId,
                                result,
                                offset,
                                null
                            )
                        }.onFailure { throwable ->
                            ProximityNotificationLogger.error(
                                eventId = ProximityNotificationEventId.BLE_GATT_ON_CHARACTERISTIC_WRITE_REQUEST_ERROR,
                                message = "onCharacteristicWriteRequest failed to send response",
                                cause = throwable
                            )
                        }
                    }
                }

            }
        }
    }

}