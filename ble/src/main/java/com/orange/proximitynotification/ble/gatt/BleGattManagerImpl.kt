/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.orange.proximitynotification.ProximityNotificationEventId
import com.orange.proximitynotification.ProximityNotificationLogger
import com.orange.proximitynotification.ble.BleSettings
import com.orange.proximitynotification.ble.gatt.BleGattManager.Callback.PayloadReceivedStatus.INVALID_PAYLOAD
import com.orange.proximitynotification.ble.gatt.BleGattManager.Callback.PayloadReceivedStatus.PAYLOAD_HANDLED
import com.orange.proximitynotification.ble.gatt.BleGattManager.Callback.PayloadReceivedStatus.UNKNOWN_DEVICE_REQUEST_RSSI_NEEDED
import com.orange.proximitynotification.tools.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal class BleGattManagerImpl(
    override val settings: BleSettings,
    private val context: Context,
    private val bluetoothManager: BluetoothManager,
    private val gattClientProvider: BleGattClientProvider
) : BleGattManager {

    companion object {
        private const val GATT_APPLICATION_ERROR = 80
    }

    private var bluetoothGattServer: BluetoothGattServer? = null

    private val gattConnectedDevices: Set<BluetoothDevice>
        get() = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
            .plus(bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER))
            .toSet()

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
        return runCatching {
            bluetoothGattServer =
                bluetoothManager.openGattServer(context, GattServerCallback(callback))?.apply {
                    try {
                        gattConnectedDevices.forEach { cancelConnection(it) }
                        clearServices()
                        addService(buildGattService())
                    } catch (t: Throwable) {
                        close()
                        throw t
                    }
                }

            requireNotNull(bluetoothGattServer) { "openGattServer returned null" }
            ProximityNotificationLogger.info(
                eventId = ProximityNotificationEventId.BLE_GATT_START_SUCCESS,
                message = "Succeed to start GATT server"
            )

        }.onFailure {
            ProximityNotificationLogger.error(
                eventId = ProximityNotificationEventId.BLE_GATT_START_ERROR,
                message = "Failed to start GATT server",
                cause = it
            )

            doStop()
        }.isSuccess
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
    }

    override suspend fun requestRemoteRssi(device: BluetoothDevice): Result<Int> {
        return withContext(Dispatchers.IO) {

            try {
                ProximityNotificationLogger.debug(
                    eventId = ProximityNotificationEventId.BLE_GATT_REQUEST_REMOTE_RSSI,
                    message = "Requesting remote RSSI"
                )

                val rssi = withConnectedClient(device) {
                    withTimeout(settings.readRemoteRssiTimeout) { it.readRemoteRssi() }
                }

                ProximityNotificationLogger.debug(
                    eventId = ProximityNotificationEventId.BLE_GATT_REQUEST_REMOTE_RSSI_SUCCESS,
                    message = "Succeed to request remote RSSI"
                )

                Result.Success(rssi)

            } catch (t: Throwable) {

                ProximityNotificationLogger.error(
                    eventId = ProximityNotificationEventId.BLE_GATT_REQUEST_REMOTE_RSSI_ERROR,
                    message = "Failed to request remote RSSI. Exception thrown",
                    cause = t
                )

                Result.Failure(t)
            }
        }
    }

    override suspend fun exchangePayload(
        device: BluetoothDevice,
        value: ByteArray,
        shouldReadRemotePayload: Boolean
    ): Result<RemoteRssiAndPayload?> = withContext(Dispatchers.IO) {

        try {
            ProximityNotificationLogger.debug(
                eventId = ProximityNotificationEventId.BLE_GATT_EXCHANGE_PAYLOAD,
                message = "Exchange payload"
            )

            val result = withConnectedClient(device) { client ->

                val remoteServices =
                    withTimeout(settings.discoverServicesTimeout) { client.discoverServices() }

                val payloadCharacteristic = remoteServices
                    .singleOrNull { it.uuid == settings.serviceUuid }
                    ?.getCharacteristic(payloadCharacteristic.uuid)
                    ?: throw BleGattManagerException.IncorrectPayloadService("Bluetooth services does not contain expected UUID")

                val properties = payloadCharacteristic.properties

                if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != BluetoothGattCharacteristic.PROPERTY_WRITE) {
                    throw BleGattManagerException.IncorrectPayloadService("payload characteristic is not writeable (properties=$properties)")
                }

                payloadCharacteristic.value = value.copyOf()
                payloadCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                withTimeout(settings.writeRemotePayloadTimeout) {
                    client.writeCharacteristic(payloadCharacteristic)
                }

                if (shouldReadRemotePayload
                    && (properties and BluetoothGattCharacteristic.PROPERTY_READ == BluetoothGattCharacteristic.PROPERTY_READ)
                ) {

                    val rssi =
                        withTimeout(settings.readRemoteRssiTimeout) { client.readRemoteRssi() }
                    val remotePayload = withTimeout(settings.readRemotePayloadTimeout) {
                        client.readCharacteristic(payloadCharacteristic).value?.copyOf()
                    }

                    requireNotNull(remotePayload) { "remote payload could not be null" }
                    RemoteRssiAndPayload(rssi, remotePayload)
                } else {
                    null
                }
            }
            ProximityNotificationLogger.debug(
                eventId = ProximityNotificationEventId.BLE_GATT_EXCHANGE_PAYLOAD_SUCCESS,
                message = "Succeed to exchange payload"
            )

            Result.Success(result)

        } catch (t: Throwable) {

            ProximityNotificationLogger.error(
                eventId = ProximityNotificationEventId.BLE_GATT_EXCHANGE_PAYLOAD_ERROR,
                message = "Failed to exchange payload. Exception thrown",
                cause = t
            )

            Result.Failure(t)
        }
    }

    private suspend fun <T> withConnectedClient(
        device: BluetoothDevice,
        action: suspend (BleGattClient) -> T
    ): T {
        val client: BleGattClient = gattClientProvider.fromDevice(device)

        return try {
            try {
                withTimeout(settings.connectionTimeout) { client.open() }

                ProximityNotificationLogger.debug(
                    eventId = ProximityNotificationEventId.BLE_GATT_CONNECT_SUCCESS,
                    message = "Succeed to connect"
                )
            } catch (t: Throwable) {
                ProximityNotificationLogger.error(
                    eventId = ProximityNotificationEventId.BLE_GATT_CONNECT_ERROR,
                    message = "Failed to connect",
                    cause = t
                )

                throw BleGattManagerException.ConnectionFailed(cause = t)
            }

            try {
                action(client)
            } catch (t: BleGattManagerException) {
                throw t
            } catch (t: Throwable) {
                throw BleGattManagerException.OperationFailed(t)
            }

        } finally {
            withContext(NonCancellable) {
                client.close()
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

            val result = when {
                value == null || characteristic.uuid != payloadCharacteristic.uuid -> BluetoothGatt.GATT_FAILURE
                offset != 0 -> BluetoothGatt.GATT_INVALID_OFFSET
                else -> {
                    val clonedValue = value.copyOf()
                    val result = callback.runCatching { onPayloadReceived(device, clonedValue) }
                        .onFailure {
                            ProximityNotificationLogger.error(
                                eventId = ProximityNotificationEventId.BLE_GATT_ON_CHARACTERISTIC_WRITE_REQUEST_ERROR,
                                message = "onCharacteristicWriteRequest failed to handle payload",
                                cause = it
                            )
                        }
                        .onSuccess {
                            ProximityNotificationLogger.debug(
                                eventId = ProximityNotificationEventId.BLE_GATT_ON_CHARACTERISTIC_WRITE_REQUEST_SUCCESS,
                                message = "onCharacteristicWriteRequest succeed to handle payload, result=$it"
                            )
                        }
                        .getOrNull()

                    when (result) {
                        null -> BluetoothGatt.GATT_FAILURE
                        INVALID_PAYLOAD -> BluetoothGatt.GATT_FAILURE
                        UNKNOWN_DEVICE_REQUEST_RSSI_NEEDED -> {
                            // Specific case.
                            // In that case we need to request remote rssi
                            // so we want to inform remote to keep the connection alive
                            // We used for that GATT application error
                            GATT_APPLICATION_ERROR
                        }
                        PAYLOAD_HANDLED -> BluetoothGatt.GATT_SUCCESS
                    }
                }
            }

            if (responseNeeded) {
                runCatching {
                    val sendResponseResult =
                        bluetoothGattServer?.sendResponse(device, requestId, result, offset, value)
                            ?: true
                    check(sendResponseResult) { "Failed to send response" }
                }.onFailure { throwable ->
                    ProximityNotificationLogger.error(
                        eventId = ProximityNotificationEventId.BLE_GATT_ON_CHARACTERISTIC_WRITE_REQUEST_ERROR,
                        message = "onCharacteristicWriteRequest failed to send response",
                        cause = throwable
                    )
                }.onSuccess {
                    ProximityNotificationLogger.debug(
                        eventId = ProximityNotificationEventId.BLE_GATT_ON_CHARACTERISTIC_WRITE_REQUEST_SUCCESS,
                        message = "onCharacteristicWriteRequest succeed (with response)"
                    )
                }
            } else {
                ProximityNotificationLogger.debug(
                    eventId = ProximityNotificationEventId.BLE_GATT_ON_CHARACTERISTIC_WRITE_REQUEST_SUCCESS,
                    message = "onCharacteristicWriteRequest succeed (without response)"
                )
            }

        }
    }


}