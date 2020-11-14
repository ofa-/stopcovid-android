/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble.scanner

import android.os.ParcelUuid
import com.orange.proximitynotification.ProximityNotificationEventId
import com.orange.proximitynotification.ProximityNotificationLogger
import com.orange.proximitynotification.ble.BleSettings
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat
import no.nordicsemi.android.support.v18.scanner.ScanCallback
import no.nordicsemi.android.support.v18.scanner.ScanFilter
import no.nordicsemi.android.support.v18.scanner.ScanResult
import no.nordicsemi.android.support.v18.scanner.ScanSettings

class BleScannerImpl(
    override val settings: BleSettings,
    private val bluetoothScanner: BluetoothLeScannerCompat
) : BleScanner {

    companion object {
        private const val APPLE_MANUFACTURER_ID = 76
    }

    private var scanCallback: ScanCallback? = null

    override fun start(callback: BleScanner.Callback): Boolean {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.BLE_SCANNER_START,
            "Starting scanner"
        )

        doStop()
        return doStart(callback)
    }

    override fun stop() {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.BLE_SCANNER_STOP,
            "Stopping scanner"
        )

        doStop()
    }

    private fun doStart(callback: BleScanner.Callback): Boolean {
        InnerScanCallback(callback).also {
            scanCallback = it
            return@doStart runCatching {
                bluetoothScanner.startScan(buildScanFilter(), buildScanSettings(), it)
            }.onFailure { throwable ->
                ProximityNotificationLogger.error(
                    eventId = ProximityNotificationEventId.BLE_SCANNER_START_ERROR,
                    message = "Failed to start scanner",
                    cause = throwable
                )
            }.isSuccess
        }
    }

    private fun doStop() {
        scanCallback?.runCatching {
            bluetoothScanner.stopScan(this)
        }?.onFailure { throwable ->
            ProximityNotificationLogger.error(
                eventId = ProximityNotificationEventId.BLE_SCANNER_STOP_ERROR,
                message = "Failed to stop scanner",
                cause = throwable
            )
        }?.onSuccess {
            ProximityNotificationLogger.info(
                eventId = ProximityNotificationEventId.BLE_SCANNER_STOP_SUCCESS,
                message = "Succeed to stop scanner"
            )
        }
        scanCallback = null
    }

    private fun buildScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setLegacy(true)
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setReportDelay(settings.scanReportDelay)
            .setUseHardwareBatchingIfSupported(true)
            .setUseHardwareFilteringIfSupported(true)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .build()
    }

    private fun buildScanFilter(): List<ScanFilter> {
        return listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(settings.serviceUuid)).build(),
            ScanFilter.Builder()
                .setServiceUuid(null)
                .setManufacturerData(
                    APPLE_MANUFACTURER_ID,
                    settings.backgroundServiceManufacturerDataIOS
                )
                .build()
        )
    }

    private inner class InnerScanCallback(private val callback: BleScanner.Callback) :
        ScanCallback() {

        private var hasLoggedStartStatus = false

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)

            logStartStatus { logStartSuccess() }

            ProximityNotificationLogger.verbose(
                ProximityNotificationEventId.BLE_SCANNER_ON_BATCH_SCAN_RESULT,
                "onBatchScanResults with result count = ${results.size}"
            )

            results
                .toBleScannedDevices(settings.serviceUuid)
                .takeIf { it.isNotEmpty() }
                ?.let { callback.onResult(it) }
        }

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            super.onScanResult(callbackType, result)

            logStartStatus { logStartSuccess() }

            ProximityNotificationLogger.debug(
                ProximityNotificationEventId.BLE_SCANNER_ON_SCAN_RESULT,
                "onScanResult with 1 result"
            )

            callback.onResult(listOf(result.toBleScannedDevice(settings.serviceUuid)))
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)

            logStartStatus { logStartError(errorCode) }

            callback.onError(errorCode)
        }

        private fun logStartStatus(action: () -> Unit) {
            if (!hasLoggedStartStatus) {
                hasLoggedStartStatus = true
                action()
            }
        }

        private fun logStartSuccess() = ProximityNotificationLogger.info(
            ProximityNotificationEventId.BLE_SCANNER_START_SUCCESS,
            "Succeed to start scanner"
        )

        private fun logStartError(errorCode: Int) = ProximityNotificationLogger.error(
            ProximityNotificationEventId.BLE_SCANNER_START_ERROR,
            "Failed to start scanner (errorCode = $errorCode)"
        )
    }

}


