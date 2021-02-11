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
import com.orange.proximitynotification.ProximityNotificationError
import com.orange.proximitynotification.ProximityNotificationEventId
import com.orange.proximitynotification.ProximityNotificationLogger
import com.orange.proximitynotification.ProximityPayload
import com.orange.proximitynotification.ble.advertiser.BleAdvertiser
import com.orange.proximitynotification.ble.calibration.BleRssiCalibration
import com.orange.proximitynotification.ble.gatt.BleGattManager
import com.orange.proximitynotification.ble.gatt.RemoteRssiAndPayload
import com.orange.proximitynotification.ble.scanner.BleScannedDevice
import com.orange.proximitynotification.ble.scanner.BleScanner
import com.orange.proximitynotification.tools.CoroutineContextProvider
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


private class BluetoothStackUnhealthyException : Exception()
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
    }


    private enum class Timeouts(
        val serviceScan: Long = 2_000L,
        val deviceScan: Long = 2_000L,
        val afterStoppingScanner: Long,
        val advertiseProcess: Long,
        val betweenDeviceAdvertisement: Long,
        val scannerRestart: Long = 30_000L
    ) {
        ANDROID_LT_7(
            advertiseProcess = 15_000L,
            afterStoppingScanner = 100L,
            betweenDeviceAdvertisement = 500L,
        ) {
            override val beforeStartingScanner = 3_000L
        },
        ANDROID_GE_7(
            advertiseProcess = 10_000L,
            afterStoppingScanner = 100L,
            betweenDeviceAdvertisement = 100L
        ) {
            // Since Android 7 we should not start and stop scans more than 5 times
            // in a window of 30 seconds (scannerRestart).
            override val beforeStartingScanner: Long
                get() = maxOf(500, 8_000L - timeSinceLastScannerStart)
        };

        abstract val beforeStartingScanner: Long
    }

    override val shouldRestartBluetooth = hasUnstableBluetoothStack()

    private val scannedDeviceSelector: BleScannedDeviceSelector by lazy {
        BleScannedDeviceSelector(
            cacheTimeout = settings.deviceSelectorCacheTimeout,
            minConfidenceScore = settings.deviceSelectorMinConfidenceScore,
            minStatsCount = settings.deviceSelectorMinStatsCount,
            // On Android7+ we don't scan device again so best scans are the most recent ones
            timestampIsImportantInSelection = isAndroidGreaterOrEqual7(),
            payloadIdProvider = proximityPayloadIdProvider
        )
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var advertiseJob: Job? = null
    private val proximityPayload = AtomicReference<ProximityPayload?>(null)
    private val stats =
        Stats(minStatCount = 20, errorRateThreshold = 0.55F, minScanErrorCount = 2)

    private val timeouts: Timeouts = when {
        isAndroidGreaterOrEqual7() -> Timeouts.ANDROID_GE_7
        else -> Timeouts.ANDROID_LT_7
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
        super.handleScanResults(results)
        scannedDeviceSelector.add(results)
    }

    private fun advertiseJob() = coroutineScope.launch(coroutineContextProvider.io) {

        try {

            wakeLock?.release()
            @SuppressLint("WakelockTimeout")
            wakeLock = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BleProximityNotification:WakeLock")
                ?.apply { acquire() }

            proximityPayload.set(proximityPayloadProvider.current())
            advertiseLoop(this)
        } catch (_: CancellationException) {
            // no-op
        } catch (_: BluetoothStackUnhealthyException) {
            notifyErrorAsync(
                ProximityNotificationError(
                    ProximityNotificationError.Type.BLE_ADVERTISER,
                    rootErrorCode = ProximityNotificationError.UNHEALTHY_BLUETOOTH_ERROR_CODE,
                    cause = "Advertise job failed (Bluetooth stack seems unhealthy)"
                )
            )
        } catch (t: Throwable) {
            notifyErrorAsync(
                ProximityNotificationError(
                    ProximityNotificationError.Type.BLE_ADVERTISER,
                    cause = "Advertise job failed (throwable = $t)"
                )
            )

        } finally {

            withContext(NonCancellable) {
                wakeLock?.release()
                wakeLock = null
            }
        }
    }

    private suspend fun advertiseLoop(coroutineScope: CoroutineScope) {

        while (coroutineScope.isActive) {

            if (stats.isUnHealthy()) {
                ProximityNotificationLogger.info(
                    ProximityNotificationEventId.BLE_PROXIMITY_NOTIFICATION_WITHOUT_ADVERTISER,
                    "Bluetooth seems to be unhealthy"
                )

                if (hasUnstableBluetoothStack()) {
                    throw BluetoothStackUnhealthyException()
                }
            }

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
                scannedDeviceSelector.failedToScan(deviceScan)
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

                stats.succeed()
                scannedDeviceSelector.succeed(scannedDevice)
            }

            is Result.Failure -> {
                stats.failed()
                scannedDeviceSelector.failed(scannedDevice)
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
            withTimeout(timeouts.deviceScan) { scanForDevice(deviceScan.deviceId()) }
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
            payload,
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
                                stats.scanSucceed()
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
            stats.scanFailed()

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


    internal class Stats(
        private val minStatCount: Int,
        private val errorRateThreshold: Float,
        private val minScanErrorCount: Int
    ) {
        private val lastStatusHistory = mutableListOf<Boolean>()
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
        fun add(result: Boolean) {
            lastStatusHistory.add(result)
        }

        @Synchronized
        fun isUnHealthy(): Boolean {
            return isErrorRateExceeded() || hasTooManyScannerStartInError()
        }

        private fun hasTooManyScannerStartInError() =
            scanErrorCount.get() >= minScanErrorCount

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

}



