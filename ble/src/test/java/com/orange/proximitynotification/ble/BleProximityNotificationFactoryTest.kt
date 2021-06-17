/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/11/25 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import org.junit.Test

class BleProximityNotificationFactoryTest {

    private val settings: BleSettings = BleSettings(
        serviceUuid = mock(),
        servicePayloadCharacteristicUuid = mock(),
        backgroundServiceManufacturerDataIOS = byteArrayOf(),
        txCompensationGain = 0,
        rxCompensationGain = 5
    )
    private val bluetoothAdapter: BluetoothAdapter = mock()
    private val bluetoothManager: BluetoothManager = mock {
        on { adapter } doReturn bluetoothAdapter
    }

    private val context: Context = mock {
        on { getSystemService(Context.BLUETOOTH_SERVICE) } doReturn bluetoothManager
    }
    private val coroutineScope : CoroutineScope = mock()

    @Test
    fun `build on device having bluetooth advertiser should return BleProximityNotificationWithAdvertiser`() {

        // Given
        val bluetoothLeAdvertiser : BluetoothLeAdvertiser = mock()
        doReturn(bluetoothLeAdvertiser).whenever(bluetoothAdapter).bluetoothLeAdvertiser

        // When
        val result = BleProximityNotificationFactory.build(context, settings, coroutineScope)

        // Then
        verify(bluetoothAdapter, times(1)).bluetoothLeAdvertiser
        assertThat(result).isInstanceOf(BleProximityNotificationWithAdvertiser::class.java)
    }

    @Test
    fun `build on device without bluetooth advertiser should return BleProximityNotificationWithoutAdvertiser`() {

        // Given
        doReturn(null).whenever(bluetoothAdapter).bluetoothLeAdvertiser

        // When
        val result = BleProximityNotificationFactory.build(context, settings, coroutineScope)

        // Then
        verify(bluetoothAdapter, times(1)).bluetoothLeAdvertiser
        assertThat(result).isInstanceOf(BleProximityNotificationWithoutAdvertiser::class.java)
    }
}