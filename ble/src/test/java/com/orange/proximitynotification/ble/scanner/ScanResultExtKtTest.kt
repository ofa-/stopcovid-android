/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. 
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble.scanner

import android.bluetooth.BluetoothDevice
import android.os.ParcelUuid
import com.google.common.truth.Truth.assertThat
import com.googlecode.zohhak.api.TestWith
import com.googlecode.zohhak.api.runners.ZohhakRunner
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.nordicsemi.android.support.v18.scanner.ScanRecord
import no.nordicsemi.android.support.v18.scanner.ScanResult
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(ZohhakRunner::class)
class ScanResultExtKtTest {

    @TestWith(value = [
        "data",
        "null"])
    fun toBleScannedDevice_should_create_BLEScannedDevice(data: String?) {
        // Given
        val uuid = UUID.randomUUID()
        val device = mock<BluetoothDevice>()
        val rssi = -65
        val expected = BleScannedDevice(
            device = device,
            rssi = rssi,
            serviceData = data?.toByteArray()
        )
        val scanRecord = givenScanRecord(
            data = data?.toByteArray()
        )
        val scanResult = givenScanResult(
            device = device,
            rssi = rssi,
            scanRecord = scanRecord
        )

        // When
        val result = scanResult.toBleScannedDevice(uuid)

        // Then
        assertThat(result).isEqualTo(expected.copy(timestamp = result.timestamp))
    }

    private fun givenScanResult(
        device: BluetoothDevice = mock(),
        rssi: Int = 0,
        scanRecord: ScanRecord? = null
    ): ScanResult {

        val scanResult = mock<ScanResult>()
        whenever(scanResult.device).thenReturn(device)
        whenever(scanResult.rssi).thenReturn(rssi)
        whenever(scanResult.scanRecord).thenReturn(scanRecord)
        return scanResult
    }

    private fun givenScanRecord(data: ByteArray? = null): ScanRecord {
        val scanRecord = mock<ScanRecord>()
        val serviceData = mock<Map<ParcelUuid, ByteArray>>()
        whenever(scanRecord.serviceData).thenReturn(serviceData)
        whenever(serviceData[any()]).thenReturn(data)
        return scanRecord
    }

}