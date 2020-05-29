/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/17 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble.gatt

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhaarman.mockitokotlin2.mock
import com.orange.proximitynotification.CoroutineTestRule
import com.orange.proximitynotification.ble.bluetoothDevice
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BleGattClientImplTest {

    @get:Rule
    val testCoroutineRule = CoroutineTestRule()

    private val context: Context = mock()

    @Test
    fun close_given_open_not_called_should_not_fail() = testCoroutineRule.runBlockingTest {
        // Given
        val device = bluetoothDevice()
        val client = BleGattClientImpl(device, context)

        // When
        client.close()

        // Then
    }
}