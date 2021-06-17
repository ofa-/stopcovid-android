/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. 
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2021/02/19 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble.scanner

import android.os.ParcelUuid
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.nordicsemi.android.support.v18.scanner.ScanRecord
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ScanRecordExtKtTest {

    companion object {
        private const val MANUFACTURER_ID = 1
        private val MANUFACTURER_DATA_MASK = byteArrayOf(1, 0, 8, 0)
    }

    @Test
    fun `hasServiceUuid given scanRecord without service UUID should return false`() {
        // Given
        val serviceUUId = ParcelUuid(UUID.randomUUID())
        val scanRecord = givenScanRecord()

        // When
        val result = scanRecord.hasServiceUuid(serviceUUId)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasServiceUuid given scanRecord with expected service UUID should return true`() {
        // Given
        val serviceUUId = ParcelUuid(UUID.randomUUID())
        val scanRecord = givenScanRecord(serviceUuids = listOf(serviceUUId))

        // When
        val result = scanRecord.hasServiceUuid(serviceUUId)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `hasServiceUuid given scanRecord with another service UUID should return false`() {
        // Given
        val serviceUUId = ParcelUuid(UUID.randomUUID())
        val otherServiceUUId = ParcelUuid(UUID.randomUUID())
        val scanRecord = givenScanRecord(serviceUuids = listOf(otherServiceUUId))

        // When
        val result = scanRecord.hasServiceUuid(serviceUUId)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasServiceUuid given scanRecord having multiple service UUID including expected one should return true`() {
        // Given
        val serviceUUId = ParcelUuid(UUID.randomUUID())
        val otherServiceUUId = ParcelUuid(UUID.randomUUID())
        val scanRecord = givenScanRecord(serviceUuids = listOf(serviceUUId, otherServiceUUId))

        // When
        val result = scanRecord.hasServiceUuid(serviceUUId)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `hasServiceUuid given scanRecord with serviceData having data for expected service UUID should return true`() {
        // Given
        val serviceUUId = ParcelUuid(UUID.randomUUID())
        val scanRecord = givenScanRecord(serviceData = mapOf(serviceUUId to byteArrayOf()))

        // When
        val result = scanRecord.hasServiceUuid(serviceUUId)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `hasServiceUuid given scanRecord with serviceData having data for another service UUID should return false`() {
        // Given
        val serviceUUId = ParcelUuid(UUID.randomUUID())
        val otherServiceUUId = ParcelUuid(UUID.randomUUID())
        val scanRecord = givenScanRecord(serviceData = mapOf(otherServiceUUId to byteArrayOf()))

        // When
        val result = scanRecord.hasServiceUuid(serviceUUId)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `matchesManufacturerDataMask given scanRecord without manufacturer data should return false`() {
        // Given
        val scanRecord = givenScanRecord()

        // When
        val result = scanRecord.matchesExpectedManufacturerData()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `matchesManufacturerDataMask given scanRecord with unexpected manufacturerId should return false`() {
        // Given
        val scanRecord =
            givenScanRecord(manufacturerData = mapOf(2 to MANUFACTURER_DATA_MASK.copyOf()))

        // When
        val result = scanRecord.matchesExpectedManufacturerData()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `matchesManufacturerDataMask given scanRecord with incorrect manufacturer data return false`() {
        // Given
        val scanRecord =
            givenScanRecord(manufacturerData = mapOf(MANUFACTURER_ID to byteArrayOf(1)))

        // When
        val result = scanRecord.matchesExpectedManufacturerData()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `matchesManufacturerDataMask given scanRecord with manufacturer data equal to mask should return true`() {
        // Given
        val scanRecord =
            givenScanRecord(manufacturerData = mapOf(MANUFACTURER_ID to MANUFACTURER_DATA_MASK.copyOf()))

        // When
        val result = scanRecord.matchesExpectedManufacturerData()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `matchesManufacturerDataMask given scanRecord with manufacturer data matching mask should return true`() {
        // Given
        val scanRecord =
            givenScanRecord(manufacturerData = mapOf(MANUFACTURER_ID to byteArrayOf(1, 1, 8, 8)))

        // When
        val result = scanRecord.matchesExpectedManufacturerData()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `matchesManufacturerDataMask given scanRecord with another manufacturer data matching mask should return true`() {
        // Given
        val scanRecord =
            givenScanRecord(manufacturerData = mapOf(MANUFACTURER_ID to byteArrayOf(1, 0, 10, 0)))

        // When
        val result = scanRecord.matchesExpectedManufacturerData()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `matchesManufacturerDataMask given scanRecord without manufacturer data matching mask should return false`() {
        // Given
        val scanRecord =
            givenScanRecord(manufacturerData = mapOf(MANUFACTURER_ID to byteArrayOf(1, 0, 1, 0)))

        // When
        val result = scanRecord.matchesExpectedManufacturerData()

        // Then
        assertThat(result).isFalse()
    }

    private fun ScanRecord.matchesExpectedManufacturerData() =
        matchesManufacturerDataMask(MANUFACTURER_ID, MANUFACTURER_DATA_MASK)

    private fun givenScanRecord(
        serviceUuids: List<ParcelUuid>? = null,
        serviceData: Map<ParcelUuid, ByteArray>? = null,
        manufacturerData: Map<Int, ByteArray> = emptyMap()
    ): ScanRecord {

        val scanRecord = mock<ScanRecord>()
        whenever(scanRecord.serviceUuids).thenReturn(serviceUuids)
        whenever(scanRecord.serviceData).thenReturn(serviceData)

        manufacturerData.forEach { (manufacturerId, data) ->
            whenever(scanRecord.getManufacturerSpecificData(manufacturerId)).thenReturn(data)
        }

        return scanRecord
    }
}