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
import com.orange.proximitynotification.ProximityPayloadIdProviderWithCache
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import com.orange.proximitynotification.tools.ExpiringCache
import kotlin.math.roundToInt

private typealias BleScannedDeviceRemoteId = Int

internal class BleScannedDeviceSelector(
    private val cacheTimeout: Long = 30 * 1000L,
    private val minConfidenceScore: Int = -2,
    private val minStatsCount: Int = 10,
    private val timestampIsImportantInSelection: Boolean = true,
    payloadIdProvider: ProximityPayloadIdProvider
) {
    companion object {
        private const val CACHE_MAX_SIZE = 100
    }

    private val deviceStatsCache =
        ExpiringCache<BleScannedDeviceRemoteId, DeviceStats>(CACHE_MAX_SIZE, cacheTimeout)
    private val deviceScans = mutableListOf<BleScannedDevice>()

    private val cachedProximityPayloadIdProvider = ProximityPayloadIdProviderWithCache(
        payloadIdProvider,
        maxSize = CACHE_MAX_SIZE,
        expiringTime = cacheTimeout
    )

    // Select by confidence score / THEN most recent scans / THEN by rssi average / THEN by scan count
    private val deviceScansComparator = compareByDescending<DeviceScansByRemoteId> {

        deviceStatsCache[it.remoteId]?.let { deviceStats ->
            val confidenceScore = deviceStats.confidenceScore
            var priority = 0

            if (deviceStats.scanFailureCount > 0) {
                priority -= deviceStats.scanFailureCount
            }

            if (deviceStats.totalCount > minStatsCount) {
                priority -= 1000
            }

            confidenceScore + priority
        } ?: run {
            0
        }
    }
        .thenByDescending {
            when (timestampIsImportantInSelection) {
                true -> it.timestampBracket
                false -> 0
            }
        }
        .thenByDescending { it.rssiBracket }
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
    suspend fun failedToScan(scan: BleScannedDevice) {
        updateOrCreateDeviceStats(scan.remoteId()) {
            it.scanFailureCount = it.scanFailureCount + 1
            it
        }
    }

    @Synchronized
    suspend fun succeed(scan: BleScannedDevice) {
        updateOrCreateDeviceStats(scan.remoteId()) {
            it.successCount = it.successCount + 1
            it.lastSuccessTime = System.currentTimeMillis()
            it.scanFailureCount = 0
            it
        }
    }

    @Synchronized
    suspend fun failed(scan: BleScannedDevice) {
        updateOrCreateDeviceStats(scan.remoteId()) {
            it.failureCount = it.failureCount + 1
            it.scanFailureCount = 0
            it
        }
    }

    @Synchronized
    suspend fun select(): List<BleScannedDevice> {

        val newDeviceScans = deviceScans
            .keepMostRecentDeviceIdScans()
            .keepScansHavingBestStats()
        deviceScans.clear()

        val sortedDeviceScans = newDeviceScans.groupBy { it.deviceId() }
            .map { DeviceScansByRemoteId(it.value.first().remoteId(), it.value) }
            .sortedWith(deviceScansComparator)

        return sortedDeviceScans.map { it.mostRecentScan }
    }


    private fun updateOrCreateDeviceStats(
        remoteId: BleScannedDeviceRemoteId,
        updater: (DeviceStats) -> DeviceStats
    ) {
        val deviceStats = deviceStatsCache[remoteId] ?: DeviceStats()

        deviceStatsCache.put(remoteId, updater(deviceStats))
    }

    /**
     * Scans for a same payload id could have different device id (rotating device id)
     * In that case we should keep only scans with the most recent device id
     */
    private suspend fun List<BleScannedDevice>.keepMostRecentDeviceIdScans(): List<BleScannedDevice> {

        val result = mutableListOf<BleScannedDevice>()

        // We can't distinguish scans without service data so keep all of them
        result.addAll(filter { it.serviceData == null })

        // Keep most recent scans having same id
        filter { it.serviceData != null }
            .groupBy { it.remoteId() }
            .values.forEach { scans ->
                scans.maxByOrNull { it.timestamp }?.deviceId()?.let { mostRecentDeviceId ->
                    result.addAll(scans.filter { it.deviceId() == mostRecentDeviceId })
                }
            }

        return result
    }

    /**
     * Remove all scans with bad DeviceStats
     */
    private suspend fun List<BleScannedDevice>.keepScansHavingBestStats(): List<BleScannedDevice> =
        filter {

            deviceStatsCache[it.remoteId()]?.let { deviceStats ->

                when {
                    deviceStats.confidenceScore <= minConfidenceScore -> false
                    deviceStats.lastSuccessTime != null && (System.currentTimeMillis() - deviceStats.lastSuccessTime!!) > cacheTimeout -> false
                    else -> true
                }

            } ?: run {
                true
            }

        }

    private suspend fun BleScannedDevice.remoteId(): BleScannedDeviceRemoteId = when (serviceData) {
        null -> deviceId().hashCode()
        else -> BlePayload.fromOrNull(serviceData)?.proximityPayload?.let {
            cachedProximityPayloadIdProvider.fromProximityPayload(it)?.contentHashCode()
        } ?: serviceData.contentHashCode()
    }
}

private data class DeviceScansByRemoteId(
    val remoteId: BleScannedDeviceRemoteId,
    val scans: List<BleScannedDevice>
) {
    val rssiAverage: Double by lazy { scans.map { it.rssi }.average() }
    val rssiBracket: Int by lazy { rssiAverage.div(5).roundToInt() }
    val scansCount: Int by lazy { scans.size }
    val mostRecentScan: BleScannedDevice by lazy { scans.maxByOrNull { it.timestamp }!! }
    val timestamp: Long by lazy { mostRecentScan.timestamp.time }
    val timestampBracket: Long by lazy { timestamp / 200 }
}

private data class DeviceStats(
    var scanFailureCount: Int = 0,
    var successCount: Int = 0,
    var failureCount: Int = 0,
    var lastSuccessTime: Long? = null
) {
    val totalCount: Int
        get() = successCount + failureCount

    val confidenceScore: Int
        get() = successCount - failureCount

}
