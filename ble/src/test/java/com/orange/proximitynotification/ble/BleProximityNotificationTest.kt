/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/08 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble

import android.bluetooth.BluetoothDevice
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.orange.proximitynotification.CoroutineTestRule
import com.orange.proximitynotification.ProximityInfo
import com.orange.proximitynotification.ProximityNotificationCallback
import com.orange.proximitynotification.ProximityNotificationError
import com.orange.proximitynotification.ProximityPayloadProvider
import com.orange.proximitynotification.ble.advertiser.BleAdvertiser
import com.orange.proximitynotification.ble.calibration.BleRssiCalibration
import com.orange.proximitynotification.ble.gatt.BleGattManager
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import com.orange.proximitynotification.ble.scanner.BleScanner
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class BleProximityNotificationTest {

    @get:Rule
    val testCoroutineRule = CoroutineTestRule()

    private val settings: BleSettings = BleSettings(
        serviceUuid = mock(),
        servicePayloadCharacteristicUuid = mock(),
        backgroundServiceManufacturerDataIOS = byteArrayOf(),
        txCompensationGain = 0,
        rxCompensationGain = 5
    )

    private val bleScanner: BleScanner = mock()
    private val bleGattManager: BleGattManager = mock()
    private val bleAdvertiser: BleAdvertiser = mock()

    private val bleProximityNotification = BleProximityNotification(
        bleAdvertiser = bleAdvertiser,
        bleGattManager = bleGattManager,
        bleScanner = bleScanner,
        settings = settings,
        coroutineScope = testCoroutineRule,
        coroutineContextProvider = testCoroutineRule
    )

    @Test
    fun scanner_with_good_result_should_call_onProximity_like_android() =
        testCoroutineRule.runBlockingTest {

            // Given
            val payload = payload(txPowerLevel = 2)
            val scannedDevice = bleScannedDevice(serviceData = payload.toByteArray(), rssi = 6)

            givenScan(scannedDevice)

            // When
            var callbackSucceed = false
            val callback = object : ProximityNotificationCallback {
                override fun onProximity(proximityInfo: ProximityInfo) {
                    assertThat(proximityInfo.payload).isEqualTo(payload.proximityPayload)
                    assertThat(proximityInfo.timestamp).isEqualTo(scannedDevice.timestamp)
                    with(proximityInfo.metadata as BleProximityMetadata) {
                        assertThat(rawRssi).isEqualTo(scannedDevice.rssi)
                        assertThat(txPowerLevel).isEqualTo(payload.txPowerLevel)
                        assertThat(calibratedRssi).isEqualTo(
                            BleRssiCalibration.calibrate(
                                scannedDevice.rssi,
                                settings.rxCompensationGain,
                                payload.txPowerLevel
                            )
                        )
                    }

                    callbackSucceed = true
                }

                override fun onError(error: ProximityNotificationError) {
                    Assert.fail()
                }
            }
            onStart(callback)

            // Then
            assertThat(callbackSucceed).isTrue()
        }

    @Test
    fun scanner_with_bad_result_should_never_call_onProximity() =
        testCoroutineRule.runBlockingTest {

            // Given
            val badPayload = ByteArray(0)
            val scannedDevice = bleScannedDevice(serviceData = badPayload, rssi = 6)

            givenScan(scannedDevice)

            // When
            val callback = object : ProximityNotificationCallback {
                override fun onProximity(proximityInfo: ProximityInfo) {
                    Assert.fail()
                }

                override fun onError(error: ProximityNotificationError) {
                    Assert.fail()
                }
            }
            onStart(callback)

            // Then
        }

    @Test
    fun scanner_on_error_should_call_onError() = testCoroutineRule.runBlockingTest {

        // Given
        val erroCode = 10

        whenever(bleScanner.start(any())).thenAnswer {
            val callback = it.arguments[0] as BleScanner.Callback
            callback.onError(erroCode)
            null
        }

        // When
        var callbackSucceed = false
        val callback = object : ProximityNotificationCallback {
            override fun onProximity(proximityInfo: ProximityInfo) {
                Assert.fail()
            }

            override fun onError(error: ProximityNotificationError) {
                assertThat(error.type).isEqualTo(ProximityNotificationError.Type.BLE_SCANNER)
                assertThat(error.rootErrorCode).isEqualTo(erroCode)
                callbackSucceed = true
            }
        }

        onStart(callback)

        // Then
        assertThat(callbackSucceed).isTrue()
    }

    @Test
    fun advertiser_with_good_payload_should_never_call_onError() =
        testCoroutineRule.runBlockingTest {

            // Given

            // When
            val callback = object : ProximityNotificationCallback {
                override fun onProximity(proximityInfo: ProximityInfo) {
                    Assert.fail()
                }

                override fun onError(error: ProximityNotificationError) {
                    Assert.fail()
                }
            }

            onStart(callback)

            // Then
        }

    @Test
    fun advertiser_on_error_should_call_onError() =
        testCoroutineRule.runBlockingTest {

            // Given
            val erroCode = 10

            whenever(bleAdvertiser.start(any(), any())).thenAnswer {
                val callback = it.arguments[1] as BleAdvertiser.Callback
                callback.onError(erroCode)
                null
            }

            // When
            var callbackSucceed = false
            val callback = object : ProximityNotificationCallback {
                override fun onProximity(proximityInfo: ProximityInfo) {
                    Assert.fail()
                }

                override fun onError(error: ProximityNotificationError) {
                    assertThat(error.type).isEqualTo(ProximityNotificationError.Type.BLE_ADVERTISER)
                    assertThat(error.rootErrorCode).isEqualTo(erroCode)
                    callbackSucceed = true
                }
            }

            onStart(callback)

            // Then
            assertThat(callbackSucceed).isTrue()
        }

    @Test
    fun scanner_and_gatt_with_good_result_should_call_onProximity_like_ios() =
        testCoroutineRule.runBlockingTest {

            // Given
            val payload = payload(txPowerLevel = 2)
            val bluetoothDevice = bluetoothDevice()
            val scannedDevice =
                bleScannedDevice(device = bluetoothDevice, serviceData = null, rssi = 6)

            // Ensure gattCallback is called after scan callback
            lateinit var gattCallback: BleGattManager.Callback
            whenever(bleGattManager.start(any())).thenAnswer {
                gattCallback = it.arguments[0] as BleGattManager.Callback
                null
            }

            whenever(bleScanner.start(any())).thenAnswer {
                val callback = it.arguments[0] as BleScanner.Callback
                callback.onResult(listOf(scannedDevice))
                gattCallback.onWritePayloadRequest(bluetoothDevice, payload.toByteArray())
                null
            }

            // When
            var callbackSucceed = false
            val callback = object : ProximityNotificationCallback {
                override fun onProximity(proximityInfo: ProximityInfo) {
                    assertThat(proximityInfo.payload).isEqualTo(payload.proximityPayload)
                    assertThat(proximityInfo.timestamp).isEqualTo(scannedDevice.timestamp)
                    with(proximityInfo.metadata as BleProximityMetadata) {
                        assertThat(rawRssi).isEqualTo(scannedDevice.rssi)
                        assertThat(txPowerLevel).isEqualTo(payload.txPowerLevel)
                        assertThat(calibratedRssi).isEqualTo(
                            BleRssiCalibration.calibrate(
                                scannedDevice.rssi,
                                settings.rxCompensationGain,
                                payload.txPowerLevel
                            )
                        )
                    }

                    callbackSucceed = true
                }

                override fun onError(error: ProximityNotificationError) {
                    Assert.fail()
                }
            }
            onStart(callback)

            // Then
            verify(bleGattManager, never()).requestRemoteRssi(eq(bluetoothDevice))
            assertThat(callbackSucceed).isTrue()
        }

    @Test
    fun gatt_only_with_good_result_and_request_rssi_should_call_onProximity_like_ios() =
        testCoroutineRule.runBlockingTest {

            // Given
            val now = Date()
            val payload = payload(txPowerLevel = 2)
            val rssi = 6
            val bluetoothDevice = bluetoothDevice()

            givenGattPayload(bluetoothDevice, payload)
            doReturn(rssi).whenever(bleGattManager).requestRemoteRssi(eq(bluetoothDevice))

            // When
            var callbackSucceed = false
            val callback = object : ProximityNotificationCallback {
                override fun onProximity(proximityInfo: ProximityInfo) {
                    assertThat(proximityInfo.payload).isEqualTo(payload.proximityPayload)
                    assertThat(proximityInfo.timestamp).isAtLeast(now)
                    with(proximityInfo.metadata as BleProximityMetadata) {
                        assertThat(rawRssi).isEqualTo(rssi)
                        assertThat(txPowerLevel).isEqualTo(payload.txPowerLevel)
                        assertThat(calibratedRssi).isEqualTo(
                            BleRssiCalibration.calibrate(
                                rssi,
                                settings.rxCompensationGain,
                                payload.txPowerLevel
                            )
                        )
                    }

                    callbackSucceed = true
                }

                override fun onError(error: ProximityNotificationError) {
                    Assert.fail()
                }
            }
            onStart(callback)

            // Then
            verify(bleGattManager, atLeastOnce()).requestRemoteRssi(eq(bluetoothDevice))
            assertThat(callbackSucceed).isTrue()
        }

    @Test
    fun start_should_call_start_on_each_components() = testCoroutineRule.runBlockingTest {

        // Given

        // When
        onStart(mock())

        // Then
        verify(bleAdvertiser, atLeastOnce()).start(any(), any())
        verify(bleScanner, atLeastOnce()).start(any())
        verify(bleGattManager, atLeastOnce()).start(any())
    }

    @Test
    fun stop_should_call_stop_on_each_components() = testCoroutineRule.runBlockingTest {

        // Given

        // When
        onStop()

        // Then
        verify(bleAdvertiser, atLeastOnce()).stop()
        verify(bleScanner, atLeastOnce()).stop()
        verify(bleGattManager, atLeastOnce()).stop()
    }

    @Test
    fun notifyPayloadUpdated_should_restart_advertiser() = testCoroutineRule.runBlockingTest {

        // Given
        val callback = object : ProximityNotificationCallback {
            override fun onProximity(proximityInfo: ProximityInfo) {
            }

            override fun onError(error: ProximityNotificationError) {
            }
        }
        onStart(callback)

        // When
        bleProximityNotification.notifyPayloadUpdated()

        // Then
        verify(bleAdvertiser, times(1)).stop()
        verify(bleAdvertiser, times(2)).start(any(), any())

        verify(bleScanner, never()).stop()
        verify(bleScanner, times(1)).start(any()) // onStart
        verify(bleGattManager, never()).stop()
        verify(bleGattManager, times(1)).start(any())  // onStart
    }

    private fun onStart(callback: ProximityNotificationCallback) {
        bleProximityNotification.setUp(
            proximityPayloadProvider = object : ProximityPayloadProvider {
                override suspend fun current() = proximityPayload()
            },
            callback = callback
        )
        bleProximityNotification.start()
    }

    private fun onStop() {
        bleProximityNotification.stop()
    }

    private fun givenScan(scannedDevice: BleScannedDevice) {
        whenever(bleScanner.start(any())).thenAnswer {
            val callback = it.arguments[0] as BleScanner.Callback
            callback.onResult(listOf(scannedDevice))
            null
        }
    }

    private fun givenGattPayload(bluetoothDevice: BluetoothDevice, payload: BlePayload) {
        whenever(bleGattManager.start(any())).thenAnswer {
            val callback = it.arguments[0] as BleGattManager.Callback
            callback.onWritePayloadRequest(bluetoothDevice, payload.toByteArray())
            null
        }
    }

}