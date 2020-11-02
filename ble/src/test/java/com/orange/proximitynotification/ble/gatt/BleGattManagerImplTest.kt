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

import android.bluetooth.BluetoothGattServer
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
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.orange.proximitynotification.CoroutineTestRule
import com.orange.proximitynotification.ble.BleSettings
import com.orange.proximitynotification.ble.bluetoothDevice
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BleGattManagerImplTest {

    @get:Rule
    val testCoroutineRule = CoroutineTestRule()

    private val settings: BleSettings = BleSettings(
        serviceUuid = mock(),
        servicePayloadCharacteristicUuid = mock(),
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
        gattClientProvider = bleGattClientProvider,
        coroutineContextProvider = testCoroutineRule
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
    fun requestRemoteRssi_multiple_times_for_a_same_device_should_request_a_new_one_every_time() =
        testCoroutineRule.runBlockingTest {

            // Given
            val device = bluetoothDevice()
            doReturn(mock<BleGattClient>()).whenever(bleGattClientProvider).fromDevice(eq(device))

            // When
            onLifecycle(mock()) {
                it.requestRemoteRssi(device, false)
                it.requestRemoteRssi(device, false)
                it.requestRemoteRssi(device, false)
            }

            // Then
            verify(bleGattClientProvider, times(3)).fromDevice(eq(device))
        }

    @Test
    fun requestRemoteRssi_multiple_times_for_a_different_devices_should_request_a_new_one_every_time() =
        testCoroutineRule.runBlockingTest {

            // Given
            doReturn(mock<BleGattClient>()).whenever(bleGattClientProvider).fromDevice(any())

            // When
            onLifecycle(mock()) {
                it.requestRemoteRssi(mock(), false)
                it.requestRemoteRssi(mock(), false)
                it.requestRemoteRssi(mock(), false)
            }

            // Then
            verify(bleGattClientProvider, times(3)).fromDevice(any())
        }

    @Test
    fun requestRemoteRssi_given_gattClient_in_success_should_return_remote_rssi() =
        testCoroutineRule.runBlockingTest {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            val rssi = 5
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doReturn(rssi).whenever(client).readRemoteRssi()

            // When
            val result = onLifecycle(mock()) { it.requestRemoteRssi(device, false) }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).readRemoteRssi()
            verify(client, times(1)).open()
            verify(client, never()).close()
            assertThat(result).isEqualTo(rssi)
        }

    @Test
    fun requestRemoteRssi_with_close_given_gattClient_in_success_should_return_remote_rssi_and_close_client() =
        testCoroutineRule.runBlockingTest {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            val rssi = 5
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doReturn(rssi).whenever(client).readRemoteRssi()

            // When
            val result = onLifecycle(mock()) { it.requestRemoteRssi(device, true) }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).readRemoteRssi()
            verify(client, times(1)).open()
            verify(client, times(1)).close()
            assertThat(result).isEqualTo(rssi)
        }

    @Test
    fun requestRemoteRssi_given_gattClient_with_error_should_return_null() =
        testCoroutineRule.runBlockingTest {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doAnswer { throw BleGattClientException() }.whenever(client).readRemoteRssi()

            // When
            val result = onLifecycle(mock()) { it.requestRemoteRssi(device, false) }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).readRemoteRssi()
            verify(client, times(1)).open()
            verify(client, never()).close()
            assertThat(result).isNull()
        }

    @Test
    fun requestRemoteRssi_with_close_given_gattClient_with_error_should_close_client_and_return_null() =
        testCoroutineRule.runBlockingTest {

            // Given
            val device = bluetoothDevice()
            val client: BleGattClient = mock()
            doReturn(client).whenever(bleGattClientProvider).fromDevice(eq(device))
            doAnswer { throw BleGattClientException() }.whenever(client).readRemoteRssi()

            // When
            val result = onLifecycle(mock()) { it.requestRemoteRssi(device, true) }

            // Then
            verify(bleGattClientProvider).fromDevice(eq(device))
            verify(client, times(1)).readRemoteRssi()
            verify(client, times(1)).open()
            verify(client, times(1)).close()
            assertThat(result).isNull()
        }

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