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

import android.os.ParcelUuid
import android.util.Log
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
        private val TAG: String = BleScannerImpl::class.java.simpleName

        private const val APPLE_MANUFACTURER_ID = 76
    }

    private var scanCallback: ScanCallback? = null

    override fun start(callback: BleScanner.Callback) {
        Log.d(TAG, "Starting scanner")

        doStop()
        doStart(callback)
    }

    override fun stop() {
        Log.d(TAG, "Stopping scanner")

        doStop()
    }

    private fun doStart(callback: BleScanner.Callback) {
        scanCallback = InnerScanCallback(callback).also {
            bluetoothScanner.startScan(buildScanFilter(), buildScanSettings(), it)
        }
    }

    private fun doStop() {
        scanCallback?.let {
            bluetoothScanner.stopScan(it)
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

    private inner class InnerScanCallback(private val callback: BleScanner.Callback) : ScanCallback() {

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)

            results
                .distinctBy { it.device }
                .toBleScannedDevices(settings.serviceUuid)
                .takeIf { it.isNotEmpty() }
                ?.let { callback.onResult(it) }
        }

        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            super.onScanResult(callbackType, result)

            callback.onResult(listOf(result.toBleScannedDevice(settings.serviceUuid)))
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)

            Log.w(TAG, "onScanFailed errorCode = $errorCode")
            callback.onError(errorCode)
        }
    }

}


