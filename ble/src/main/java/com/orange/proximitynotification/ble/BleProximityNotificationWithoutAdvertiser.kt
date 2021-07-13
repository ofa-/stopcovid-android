/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.orange.proximitynotification.ProximityNotificationCallback
import com.orange.proximitynotification.ProximityNotificationError
import com.orange.proximitynotification.ProximityPayload
import com.orange.proximitynotification.ProximityPayloadIdProvider
import com.orange.proximitynotification.ProximityPayloadProvider
import com.orange.proximitynotification.ble.advertiser.BleAdvertiser
import com.orange.proximitynotification.ble.calibration.BleRssiCalibration
import com.orange.proximitynotification.ble.gatt.BleGattManager
import com.orange.proximitynotification.ble.gatt.BleGattManagerException
import com.orange.proximitynotification.ble.gatt.RemoteRssiAndPayload
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import com.orange.proximitynotification.ble.scanner.BleScanner
import com.orange.proximitynotification.tools.CoroutineContextProvider
import com.orange.proximitynotification.tools.ExpiringCache
import com.orange.proximitynotification.tools.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

private data class BleScannerException(
    val errorCode: Int? = null,
    override val cause: Throwable? = null
) : Exception()

private fun isAndroidGreaterOrEqual7() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
private fun isAndroidLessThan7() = Build.VERSION.SDK_INT < Build.VERSION_CODES.N

private fun hasUnstableBluetoothStack() = isAndroidLessThan7()

/**
 * [BleProximityNotification] implementation without any [BleAdvertiser]
 *
 * It scans and select best devices using [BleScannedDeviceSelector] then try to connect to each one
 * in order to exchange our current payload + their partially calibrated RSSI.
 *
 * Partial calibrated RSSI is computed with remote RSSI and our RX compensation gain.
 * Final calibrated RSSI computation will be done by remote using their TX compensation gain
 */
internal class BleProximityNotificationWithoutAdvertiser(
    private val context: Context,
    private val bleGattManager: BleGattManager,
    private val bleScanner: BleScanner,
    settings: BleSettings,
    coroutineScope: CoroutineScope,
    coroutineContextProvider: CoroutineContextProvider = CoroutineContextProvider.Default()
) : BleProximityNotification(settings, coroutineScope, coroutineContextProvider) {

    companion object {
        private val lastScannerStart = AtomicLong(0)

        private val timeSinceLastScannerStart: Long
            get() = System.currentTimeMillis() - lastScannerStart.get()

        private var deviceStatsRepository: BleDeviceStatsRepository? = null
    }

    private enum class Timeouts(
        val serviceScan: Long = 2_000L,
        val deviceScan: Long = 2_000L,
        val afterStoppingScanner: Long = 100L,
        val advertiseProcess: Long,
        val betweenDeviceAdvertisement: Long = 100L,
        val scannerRestart: Long = 30_000L,
        private val betweenScannerRestart: Long
    ) {
        ANDROID_LT_7(
            betweenScannerRestart = 5_000L,
            advertiseProcess = 30_000L
        ),

        // Since Android 7 we should not start and stop scans more than 5 times
        // in a window of 30 seconds (scannerRestart).
        ANDROID_GE_7(
            betweenScannerRestart = 8_000L,
            advertiseProcess = 20_000L
        );

        val beforeStartingScanner: Long
            get() = maxOf(1_000L, betweenScannerRestart - timeSinceLastScannerStart)
    }

    override val shouldRestartBluetooth: Boolean
        get() = hasUnstableBluetoothStack() && bleStats.isUnHealthy()

    override val couldRestartFrequently = false
    override val bleRecordProvider = BleRecordProvider()

    private var wakeLock: PowerManager.WakeLock? = null
    private var advertiseJob: Job? = null
    private val proximityPayload = AtomicReference<ProximityPayload?>(null)
    private val bleStats =
        BleStats(
            minStatCount = 40,
            errorRateThreshold = 0.55F,
            minScanErrorCount = 2,
            maxTimeSinceLastStatus = 15L * 60L * 1000L
        )

    private val timeouts: Timeouts = when {
        isAndroidGreaterOrEqual7() -> Timeouts.ANDROID_GE_7
        else -> Timeouts.ANDROID_LT_7
    }

    private lateinit var scannedDeviceSelector: BleScannedDeviceSelector

    override fun setUp(
        proximityPayloadProvider: ProximityPayloadProvider,
        proximityPayloadIdProvider: ProximityPayloadIdProvider,
        callback: ProximityNotificationCallback
    ) {
        super.setUp(proximityPayloadProvider, proximityPayloadIdProvider, callback)

        if (deviceStatsRepository == null) {
            deviceStatsRepository = BleDeviceStatsRepository(
                maxCacheSize = settings.maxCacheSize,
                cacheTimeout = settings.identityCacheTimeout
            )
        }

        scannedDeviceSelector = BleScannedDeviceSelector(
            maxDelayBetweenSuccess = settings.maxDelayBetweenExchange,
            maxSuccessiveFailureCount = settings.maxSuccessiveFailure,
            // On Android7+ we don't scan device again so best scans are the most recent ones
            timestampIsImportantInSelection = isAndroidGreaterOrEqual7(),
            deviceStatsProvider = { deviceStatsRepository?.get(it) },
            payloadIdProvider = proximityPayloadIdProvider
        )
    }

    override suspend fun start() {
        startAdvertiser()

        super.start()
    }

    override suspend fun stop() {
        super.stop()

        stopAdvertiser()
    }

    override suspend fun notifyPayloadUpdated(proximityPayload: ProximityPayload) {
        this.proximityPayload.set(proximityPayload)
    }

    private fun startAdvertiser() {
        if (advertiseJob == null || advertiseJob?.isCompleted == true) {
            advertiseJob = advertiseJob()
        }
    }

    private suspend fun stopAdvertiser() {
        advertiseJob?.cancelAndJoin()
        advertiseJob = null
    }

    override suspend fun handleScanResults(results: List<BleScannedDevice>) {
        scannedDeviceSelector.add(results)

        withContext(coroutineContextProvider.default) {
            // Android case
            results.forEach { scannedDevice ->
                scannedDevice.serviceData?.let { decodePayload(it) }
                    ?.let { payload -> notifyProximity(scannedDevice, payload) }
            }
        }
    }

    private fun advertiseJob() = coroutineScope.launch(coroutineContextProvider.io) {

        try {

            wakeLock?.takeIf { it.isHeld }?.release()
            @SuppressLint("WakelockTimeout")
            wakeLock = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BleProximityNotification:WakeLock")
                ?.apply { acquire() }

            proximityPayload.set(proximityPayloadProvider.current())
            advertiseLoop(this)
        } catch (_: CancellationException) {
            // no-op
        } catch (t: Throwable) {
            notifyErrorAsync(
                ProximityNotificationError(
                    ProximityNotificationError.Type.BLE_ADVERTISER,
                    cause = "Advertise job failed (throwable = $t)"
                )
            )
        } finally {

            withContext(NonCancellable) {
                wakeLock?.takeIf { it.isHeld }?.release()
                wakeLock = null
            }
        }
    }

    private suspend fun advertiseLoop(coroutineScope: CoroutineScope) {

        while (coroutineScope.isActive) {

            delay(timeouts.beforeStartingScanner)
            if (!coroutineScope.isActive) return

            runCatching {
                withTimeout(timeouts.scannerRestart) { scanForDevices() }

                if (!coroutineScope.isActive) return

                delay(timeouts.afterStoppingScanner)
                if (coroutineScope.isActive && !scannedDeviceSelector.isEmpty()) {
                    advertiseDevices(coroutineScope, scannedDeviceSelector.select())
                }
            }
        }
    }

    /**
     * Advertise as much devices as possible during [Timeouts.advertiseProcess]
     */
    private suspend fun advertiseDevices(
        coroutineScope: CoroutineScope,
        devices: List<BleScannedDevice>
    ) {
        val advertiseProcessTimeout = timeouts.advertiseProcess
        val advertiseProcessStartTime = System.currentTimeMillis()

        val scannedDeviceIterator = devices.iterator()
        var scannedDeviceIndex = 0

        while (coroutineScope.isActive && scannedDeviceIterator.hasNext()) {
            val elapsedTime = System.currentTimeMillis() - advertiseProcessStartTime

            if (elapsedTime >= advertiseProcessTimeout) {
                break
            }

            if (scannedDeviceIndex > 0) {
                val predictedElapsedTime =
                    ((elapsedTime / scannedDeviceIndex.toDouble()).roundToInt() * (scannedDeviceIndex + 1))
                if (predictedElapsedTime > advertiseProcessTimeout) {
                    break
                }
            }

            val deviceScan = scannedDeviceIterator.next()
            val refreshedDeviceScan = refreshDeviceScanIfNeeded(deviceScan)

            if (refreshedDeviceScan == null) {
                deviceStatsRepository?.failed(deviceScan.deviceId(proximityPayloadIdProvider))
            } else if (coroutineScope.isActive) {
                proximityPayload.get()?.let { currentProximityPayload ->
                    val result = advertiseDevice(currentProximityPayload, refreshedDeviceScan)
                    handleAdvertisedDevice(refreshedDeviceScan, result)
                }
            }

            scannedDeviceIndex++

            if (scannedDeviceIterator.hasNext()) {
                delay(timeouts.betweenDeviceAdvertisement)
            }
        }
    }

    private suspend fun handleAdvertisedDevice(
        scannedDevice: BleScannedDevice,
        result: Result<RemoteRssiAndPayload?>
    ) {
        when (result) {
            is Result.Success -> {

                result.value?.let { remoteRssiAndPayload ->
                    decodePayload(remoteRssiAndPayload.payload)?.let {
                        val updatedScannedDevice =
                            scannedDevice.copy(rssi = remoteRssiAndPayload.rssi, timestamp = Date())
                        notifyProximity(updatedScannedDevice, it)
                    }
                }

                bleStats.succeed()
                deviceStatsRepository?.succeed(scannedDevice.deviceId(proximityPayloadIdProvider))
            }

            is Result.Failure -> {

                val deviceId = scannedDevice.deviceId(proximityPayloadIdProvider)
                when (result.throwable) {

                    // Consider BLE is working but blacklist device
                    is BleGattManagerException.IncorrectPayloadService -> {
                        bleStats.succeed()
                        deviceStatsRepository?.blacklist(deviceId)
                    }

                    else -> {
                        bleStats.failed()
                        deviceStatsRepository?.failed(deviceId)
                    }
                }
            }
        }
    }

    /**
     * On older Android, scanning device before connecting it improves stability
     */
    private suspend fun refreshDeviceScanIfNeeded(deviceScan: BleScannedDevice): BleScannedDevice? {

        if (isAndroidGreaterOrEqual7()) {
            return deviceScan
        }

        val updatedDeviceScan = runCatching {
            withTimeout(timeouts.deviceScan) { scanForDevice(deviceScan.deviceAddress()) }
        }.getOrNull()

        delay(timeouts.afterStoppingScanner)

        return updatedDeviceScan
    }

    /**
     * Advertise device with our current [ProximityPayload].
     *
     * We will connect to device and exchange our current [ProximityPayload] plus the partial remote calibrated RSSI
     * In case of iOS device we will ask for remote payload in order to get its identity
     */
    private suspend fun advertiseDevice(
        proximityPayload: ProximityPayload,
        scannedDevice: BleScannedDevice
    ): Result<RemoteRssiAndPayload?> {
        val payload = buildPayload(proximityPayload, partialRssiCalibration(scannedDevice))
        val shouldReadRemotePayload = scannedDevice.serviceData == null

        BluetoothAdapter.getDefaultAdapter().cancelDiscovery()

        return bleGattManager.exchangePayload(
            scannedDevice.device,
            payload.toByteArray(),
            shouldReadRemotePayload
        )
    }

    /**
     * Remote RSSI calibration is done using RSSI and our RX compensation gain.
     * As we don't know their TX compensation gain, we set it at 0.
     * RSSI calibration computation will be finalized on remote side once received.
     */
    private fun partialRssiCalibration(scannedDevice: BleScannedDevice) =
        BleRssiCalibration.calibrate(
            rssi = scannedDevice.rssi,
            txCompensationGain = 0,
            rxCompensationGain = settings.rxCompensationGain
        )

    /**
     * Scan for devices having service UUID.
     */
    private suspend fun scanForDevices() = scannerWrapper(
        startScannerAction = { start(it) },
        afterFirstScanAction = {

            val elapsedTime = System.currentTimeMillis() - lastScannerStart.get()
            delay(maxOf(0, timeouts.serviceScan - elapsedTime))

            while (scannedDeviceSelector.isEmpty()) {
                delay(timeouts.serviceScan)
            }

            bleScanner.flushScans()
        }
    )

    /**
     * Scan for specific device (by device address)
     */
    private suspend fun scanForDevice(deviceAddress: String) = scannerWrapper(
        startScannerAction = { startForDevice(deviceAddress, it) },
        afterFirstScanAction = { it.lastOrNull() }
    )

    private suspend fun <T> scannerWrapper(
        startScannerAction: BleScanner.(callback: BleScanner.Callback) -> Boolean,
        afterFirstScanAction: suspend (results: List<BleScannedDevice>) -> T?
    ): T? {
        return try {
            val results: List<BleScannedDevice> = withContext(coroutineContextProvider.main) {
                lastScannerStart.set(System.currentTimeMillis())
                suspendCancellableCoroutine { continuation ->
                    val callback = object : BleScanner.Callback {
                        override fun onResult(results: List<BleScannedDevice>) {
                            checkAndHandleScanResults(results)

                            if (results.isNotEmpty()) {
                                bleStats.scanSucceed()
                            }

                            continuation.takeIf { it.isActive }?.resume(results)
                        }

                        override fun onError(errorCode: Int) {
                            continuation.takeIf { it.isActive }
                                ?.resumeWithException(BleScannerException(errorCode))
                        }
                    }

                    require(bleScanner.startScannerAction(callback)) { "Failed to start scanner" }
                    continuation.invokeOnCancellation { bleScanner.stop() }
                }
            }

            afterFirstScanAction(results)
        } catch (_: CancellationException) {
            null
        } catch (t: Throwable) {
            bleStats.scanFailed()

            throw when (t) {
                is BleScannerException -> t
                else -> BleScannerException(cause = t)
            }.also {
                notifyErrorAsync(
                    ProximityNotificationError(
                        ProximityNotificationError.Type.BLE_SCANNER,
                        rootErrorCode = it.errorCode,
                        cause = "Failed to scan devices (throwable = ${it.cause})"
                    )
                )
            }
        } finally {
            withContext(coroutineContextProvider.main + NonCancellable) {
                bleScanner.stop()
            }
        }
    }
}

/**
 * Overall BLE statistics
 */
internal class BleStats(
    private val minStatCount: Int,
    private val errorRateThreshold: Float,
    private val minScanErrorCount: Int,
    private val maxTimeSinceLastStatus: Long
) {
    private val lastStatusHistory = mutableListOf<Boolean>()
    private var lastStatusTime: Long = System.currentTimeMillis()
    private val scanErrorCount = AtomicInteger(0)

    fun scanSucceed() {
        scanErrorCount.set(0)
    }

    fun scanFailed() {
        scanErrorCount.incrementAndGet()
    }

    fun succeed() = add(true)
    fun failed() = add(false)

    @Synchronized
    fun isUnHealthy(): Boolean {
        return isErrorRateExceeded() || hasTooManyScanInError() || isLastStatusTooOld()
    }

    @Synchronized
    private fun add(result: Boolean) {
        lastStatusHistory.add(result)
        lastStatusTime = System.currentTimeMillis()
    }

    private fun isLastStatusTooOld() =
        (System.currentTimeMillis() - maxTimeSinceLastStatus) > lastStatusTime

    private fun hasTooManyScanInError() = scanErrorCount.get() >= minScanErrorCount

    private fun isErrorRateExceeded(): Boolean {

        if (lastStatusHistory.size < minStatCount) {
            return false
        }

        val lastStatuses = lastStatusHistory.takeLast(minStatCount)
        lastStatusHistory.clear()
        lastStatusHistory.addAll(lastStatuses)

        val errorCount = lastStatuses.count { !it }
        val errorRate = errorCount.toFloat() / lastStatuses.size

        return errorRate >= errorRateThreshold
    }
}

/**
 * [BleDeviceStats] internal repository using [ExpiringCache]
 */
internal class BleDeviceStatsRepository(maxCacheSize: Int, cacheTimeout: Long) {

    private val deviceStatsCache =
        ExpiringCache<BleDeviceId, BleDeviceStats>(maxCacheSize, cacheTimeout)

    @Synchronized
    fun get(deviceId: BleDeviceId): BleDeviceStats? = deviceStatsCache[deviceId]

    @Synchronized
    fun succeed(deviceId: BleDeviceId) {
        updateOrCreateDeviceStats(deviceId) {
            it.copy(
                successCount = it.successCount + 1,
                successiveFailureCount = 0,
                lastSuccessTime = System.currentTimeMillis()
            )
        }
    }

    @Synchronized
    fun failed(deviceId: BleDeviceId) {
        updateOrCreateDeviceStats(deviceId) {
            it.copy(
                failureCount = it.failureCount + 1,
                successiveFailureCount = it.successiveFailureCount + 1
            )
        }
    }

    @Synchronized
    fun blacklist(deviceId: BleDeviceId) {
        updateOrCreateDeviceStats(deviceId) {
            it.copy(shouldIgnore = true)
        }
    }

    private inline fun updateOrCreateDeviceStats(
        deviceId: BleDeviceId,
        crossinline updater: (BleDeviceStats) -> BleDeviceStats
    ) {
        val deviceStats = deviceStatsCache[deviceId] ?: BleDeviceStats()
        deviceStatsCache.put(deviceId, updater(deviceStats))
    }
}
