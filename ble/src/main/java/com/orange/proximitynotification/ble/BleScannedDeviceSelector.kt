/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/05/19 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import com.orange.proximitynotification.ProximityPayloadIdProvider
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import kotlin.math.roundToInt

internal data class BleDeviceStats(
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val successiveFailureCount: Int = 0,
    val lastSuccessTime: Long? = null,
    val shouldIgnore: Boolean = false
) {
    val confidenceScore: Int
        get() = successCount - failureCount
}

internal typealias BleDeviceStatsProvider = (BleDeviceId) -> BleDeviceStats?

internal class BleScannedDeviceSelector(
    private val maxDelayBetweenSuccess: Long,
    private val maxSuccessiveFailureCount: Int,
    private val deviceStatsProvider: BleDeviceStatsProvider,
    private val payloadIdProvider: ProximityPayloadIdProvider,
    private val timestampIsImportantInSelection: Boolean = true
) {
    private val deviceScans = mutableListOf<BleScannedDevice>()

    private val deviceScansComparator = compareByDescending<DeviceScansById> { it.rssiBracket }
        .thenByDescending { deviceStatsProvider(it.deviceId)?.confidenceScore ?: 0 }
        .thenByDescending {
            when (timestampIsImportantInSelection) {
                true -> it.timestampBracket
                false -> 0
            }
        }
        .thenByDescending { it.scansCount }
        .thenByDescending { it.rssiAverage }
        .thenByDescending { it.timestamp }

    @Synchronized
    fun isEmpty() = deviceScans.isEmpty()

    @Synchronized
    fun add(scans: List<BleScannedDevice>) {
        deviceScans.addAll(scans)
    }

    @Synchronized
    suspend fun select(): List<BleScannedDevice> {

        val newDeviceScans = deviceScans
            .keepMostRecentDeviceAddress()
            .keepRelevant()
        deviceScans.clear()

        val sortedDeviceScans = newDeviceScans.groupBy { it.deviceAddress() }
            .map { DeviceScansById(it.value.first().deviceId(payloadIdProvider), it.value) }
            .sortedWith(deviceScansComparator)

        return sortedDeviceScans.map { it.mostRecentScan }
    }

    /**
     * Scans for a same payload id could have different device address (rotating device address)
     * In that case we should keep only scans with the most recent device address
     */
    private suspend fun List<BleScannedDevice>.keepMostRecentDeviceAddress(): List<BleScannedDevice> {

        val result = mutableListOf<BleScannedDevice>()

        // We can't distinguish scans without service data so keep all of them
        result.addAll(filter { it.serviceData == null })

        // Keep most recent scans having same address
        filter { it.serviceData != null }
            .groupBy { it.deviceId(payloadIdProvider) }
            .values.forEach { scans ->
                scans.maxByOrNull { it.timestamp }?.deviceAddress()
                    ?.let { mostRecentDeviceAddress ->
                        result.addAll(scans.filter { it.deviceAddress() == mostRecentDeviceAddress })
                    }
            }

        return result
    }

    /**
     * Keep most relevant scans.
     * If a device has succeed recently or has too many failure -> remove it
     */
    private suspend fun List<BleScannedDevice>.keepRelevant(): List<BleScannedDevice> {
        val now = System.currentTimeMillis()

        return filter {

            val deviceStats = deviceStatsProvider(it.deviceId(payloadIdProvider))

            when {
                deviceStats == null -> true

                deviceStats.shouldIgnore -> false

                deviceStats.successiveFailureCount >= maxSuccessiveFailureCount -> false

                deviceStats.lastSuccessTime?.let { time ->
                    (now - time) < maxDelayBetweenSuccess
                } == true -> false

                else -> true
            }
        }
    }
}

private data class DeviceScansById(
    val deviceId: BleDeviceId,
    val scans: List<BleScannedDevice>
) {
    val rssiAverage: Double by lazy { scans.map { it.rssi }.average() }
    val rssiBracket: Int by lazy { rssiAverage.div(10).roundToInt() }
    val scansCount: Int by lazy { scans.size }
    val mostRecentScan: BleScannedDevice by lazy { scans.maxByOrNull { it.timestamp }!! }
    val timestamp: Long by lazy { mostRecentScan.timestamp.time }
    val timestampBracket: Long by lazy { timestamp / 200 }
}
