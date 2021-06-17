/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/06/04 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble.advertiser

import android.bluetooth.le.BluetoothLeAdvertiser
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.orange.proximitynotification.ble.BleSettings
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BleAdvertiserImplTest {

    private val settings: BleSettings = BleSettings(
        serviceUuid = mock(),
        servicePayloadCharacteristicUuid = mock(),
        backgroundServiceManufacturerDataIOS = byteArrayOf(),
        txCompensationGain = 0,
        rxCompensationGain = 5
    )

    private val bluetoothAdvertiser: BluetoothLeAdvertiser = mock()

    private val bleAdvertiser = BleAdvertiserImpl(
        settings = settings,
        bluetoothAdvertiser = bluetoothAdvertiser
    )

    @Test
    fun start_should_return_true() {

        // Given

        // When
        val result = bleAdvertiser.start(byteArrayOf(), mock())

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun start_given_exception_should_return_false() {

        // Given
        doThrow(RuntimeException::class).whenever(bluetoothAdvertiser)
            .startAdvertising(any(), any(), any())

        // When
        val result = bleAdvertiser.start(byteArrayOf(), mock())

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun stop_given_exception_should_not_throw_exception() {

        // Given
        doThrow(RuntimeException::class).whenever(bluetoothAdvertiser).stopAdvertising(any())

        // When
        val result = runCatching { bleAdvertiser.stop() }

        // Then
        assertThat(result.isSuccess).isTrue()
    }
}