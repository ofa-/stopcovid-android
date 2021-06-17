/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/16 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble.gatt

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.orange.proximitynotification.ble.BleSettings
import com.orange.proximitynotification.ble.bluetoothDevice
import com.orange.proximitynotification.ble.payload
import com.orange.proximitynotification.ble.proximityPayload
import com.orange.proximitynotification.tools.Result
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BleGattManagerImplTest {

    private val settings: BleSettings = BleSettings(
        serviceUuid = UUID.randomUUID(),
        servicePayloadCharacteristicUuid = UUID.randomUUID(),
        backgroundServiceManufacturerDataIOS = byteArrayOf(),
        txCompensationGain = 0,
        rxCompensationGain = 5
    )

    private val context: Context = mock()
    private val bluetoothGattServer: BluetoothGattServer = mock()
    private val bleGattClientProvider: BleGattClientProvider = mock()
    private val bluetoothManager: BluetoothManager = mock {
        on { openGattServer(any(), any()) } doReturn bluetoothGattServer
    }

    private val bleGattManager = BleGattManagerImpl(
        settings = settings,
        context = context,
        bluetoothManager = bluetoothManager,
        gattClientProvider = bleGattClientProvider
    )

    @Test
    fun start_should_return_true() {

        // Given

        // When
        val result = bleGattManager.start(mock())

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun start_given_openGattServer_return_null_should_return_false() {

        // Given
        doReturn(null).whenever(bluetoothManager).openGattServer(any(), any())

        // When
        val result = bleGattManager.start(mock())

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun start_given_bluetoothGattServer_throws_exception_should_close_opened_gatt_server_and_return_false() {

        // Given
        doThrow(RuntimeException::class).whenever(bluetoothGattServer).addService(any())

        // When
        val result = bleGattManager.start(mock())

        // Then
        assertThat(result).isFalse()
        verify(bluetoothGattServer, times(1)).close()
    }

    @Test
    fun stop_given_exception_should_not_throw_exception() {

        // Given
        doThrow(RuntimeException::class).whenever(bluetoothGattServer).close()

        // When
        val result = runCatching { bleGattManager.stop() }

        // Then
        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun requestRemoteRssi_given_gattClient_in_success_should_return_success() {
        runBlocking {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            val rssi = -20
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doReturn(rssi).whenever(client).readRemoteRssi()

            // When
            val result = onLifecycle(mock()) {
                it.requestRemoteRssi(device)
            }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).open()
            verify(client, times(1)).readRemoteRssi()
            verify(client, times(1)).close()
            assertThat(result).isEqualTo(Result.Success(rssi))
        }
    }

    @Test
    fun requestRemoteRssi_given_gattClient_connection_error_should_return_failure() {
        runBlocking {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doAnswer { throw BleGattClientException() }.whenever(client).open()

            // When
            val result = onLifecycle(mock()) {
                it.requestRemoteRssi(device)
            }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).open()
            verify(client, times(0)).readRemoteRssi()
            verify(client, times(1)).close()
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).throwable).isInstanceOf(BleGattManagerException.ConnectionFailed::class.java)
        }
    }

    @Test
    fun requestRemoteRssi_given_gattClient_in_error_should_return_failure() {
        runBlocking {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doAnswer { throw BleGattClientException() }.whenever(client).readRemoteRssi()

            // When
            val result = onLifecycle(mock()) {
                it.requestRemoteRssi(device)
            }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).open()
            verify(client, times(1)).readRemoteRssi()
            verify(client, times(1)).close()
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).throwable).isInstanceOf(BleGattManagerException.OperationFailed::class.java)
        }
    }

    @Test
    fun exchangePayload_without_requesting_remote_payload_given_gattClient_in_success_should_return_success() {
        runBlocking {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            val payload = payload()
            val services = givenBluetoothService()
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doReturn(services).whenever(client).discoverServices()

            // When
            val result = onLifecycle(mock()) {
                it.exchangePayload(
                    device,
                    value = payload.toByteArray(),
                    shouldReadRemotePayload = false
                )
            }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).open()
            verify(client, times(1)).discoverServices()
            verify(client, times(1)).writeCharacteristic(any())
            verify(client, times(0)).readRemoteRssi()
            verify(client, times(0)).readCharacteristic(any())
            verify(client, times(1)).close()
            assertThat(result).isEqualTo(Result.Success(null))
        }
    }

    @Test
    fun exchangePayload_without_requesting_remote_payload_given_gattClient_having_different_services_should_return_failure() {
        runBlocking {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            val payload = payload()
            val services = givenBluetoothService(serviceUuid = UUID.randomUUID())
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doReturn(services).whenever(client).discoverServices()

            // When
            val result = onLifecycle(mock()) {
                it.exchangePayload(
                    device,
                    value = payload.toByteArray(),
                    shouldReadRemotePayload = false
                )
            }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).open()
            verify(client, times(1)).discoverServices()
            verify(client, times(0)).writeCharacteristic(any())
            verify(client, times(0)).readRemoteRssi()
            verify(client, times(0)).readCharacteristic(any())
            verify(client, times(1)).close()
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).throwable).isInstanceOf(BleGattManagerException.IncorrectPayloadService::class.java)
        }
    }

    @Test
    fun exchangePayload_without_requesting_remote_payload_given_gattClient_having_different_characteristic_should_return_failure() {
        runBlocking {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            val payload = payload()
            val services =
                givenBluetoothService(
                    writeCharacteristic = givenBluetoothGattCharacteristic(
                        servicePayloadCharacteristicUuid = UUID.randomUUID()
                    )
                )
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doReturn(services).whenever(client).discoverServices()

            // When
            val result = onLifecycle(mock()) {
                it.exchangePayload(
                    device,
                    value = payload.toByteArray(),
                    shouldReadRemotePayload = false
                )
            }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).open()
            verify(client, times(1)).discoverServices()
            verify(client, times(0)).writeCharacteristic(any())
            verify(client, times(0)).readRemoteRssi()
            verify(client, times(0)).readCharacteristic(any())
            verify(client, times(1)).close()
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).throwable).isInstanceOf(BleGattManagerException.IncorrectPayloadService::class.java)
        }
    }

    @Test
    fun exchangePayload_requesting_remote_payload_given_gattClient_in_success_should_return_success() {
        runBlocking {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            val payload = payload()
            val remotePayload = payload(proximityPayload(1))
            val remoteRssi = 5
            val readCharacteristic =
                givenBluetoothGattCharacteristic(value = remotePayload.toByteArray())
            val services = givenBluetoothService(readCharacteristic = readCharacteristic)
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doReturn(services).whenever(client).discoverServices()
            doReturn(remoteRssi).whenever(client).readRemoteRssi()
            doReturn(readCharacteristic).whenever(client).readCharacteristic(any())

            // When
            val result = onLifecycle(mock()) {
                it.exchangePayload(
                    device,
                    value = payload.toByteArray(),
                    shouldReadRemotePayload = true
                )
            }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).open()
            verify(client, times(1)).discoverServices()
            verify(client, times(1)).writeCharacteristic(any())
            verify(client, times(1)).readRemoteRssi()
            verify(client, times(1)).readCharacteristic(any())
            verify(client, times(1)).close()

            assertThat(result).isEqualTo(
                Result.Success(
                    RemoteRssiAndPayload(
                        rssi = remoteRssi,
                        payload = remotePayload.toByteArray()
                    )
                )
            )
        }
    }

    @Test
    fun exchangePayload_given_gattClient_connection_error_should_return_failure() {
        runBlocking {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            val payload = payload()
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doAnswer { throw BleGattClientException() }.whenever(client).open()

            // When
            val result = onLifecycle(mock()) {
                it.exchangePayload(
                    device,
                    value = payload.toByteArray(),
                    shouldReadRemotePayload = false
                )
            }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).open()
            verify(client, times(0)).discoverServices()
            verify(client, times(0)).writeCharacteristic(any())
            verify(client, times(0)).readRemoteRssi()
            verify(client, times(0)).readCharacteristic(any())
            verify(client, times(1)).close()
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).throwable).isInstanceOf(BleGattManagerException.ConnectionFailed::class.java)
        }
    }

    @Test
    fun exchangePayload_given_gattClient_throwing_error_should_return_failure() {
        runBlocking {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            val payload = payload()
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doAnswer { throw BleGattClientException() }.whenever(client).discoverServices()

            // When
            val result = onLifecycle(mock()) {
                it.exchangePayload(
                    device,
                    value = payload.toByteArray(),
                    shouldReadRemotePayload = false
                )
            }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).open()
            verify(client, times(1)).discoverServices()
            verify(client, times(0)).writeCharacteristic(any())
            verify(client, times(0)).readRemoteRssi()
            verify(client, times(0)).readCharacteristic(any())
            verify(client, times(1)).close()
            assertThat(result).isInstanceOf(Result.Failure::class.java)
            assertThat((result as Result.Failure).throwable).isInstanceOf(BleGattManagerException.OperationFailed::class.java)
        }
    }

    private fun givenBluetoothService(
        serviceUuid: UUID = settings.serviceUuid,
        writeCharacteristic: BluetoothGattCharacteristic? = givenBluetoothGattCharacteristic(),
        readCharacteristic: BluetoothGattCharacteristic? = null
    ): List<BluetoothGattService> {

        val service = BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        service.addCharacteristic(writeCharacteristic)
        readCharacteristic?.let { service.addCharacteristic(readCharacteristic) }

        return listOf(service)
    }

    private fun givenBluetoothGattCharacteristic(
        servicePayloadCharacteristicUuid: UUID = settings.servicePayloadCharacteristicUuid,
        value: ByteArray = byteArrayOf()
    ) = BluetoothGattCharacteristic(
        servicePayloadCharacteristicUuid,
        BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
        BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
    ).apply { setValue(value) }

    private suspend fun <T> onLifecycle(
        callback: BleGattManager.Callback,
        block: suspend (bleGattManager: BleGattManager) -> T
    ): T {
        bleGattManager.start(callback)
        val result: T = block(bleGattManager)
        bleGattManager.stop()

        return result
    }
}