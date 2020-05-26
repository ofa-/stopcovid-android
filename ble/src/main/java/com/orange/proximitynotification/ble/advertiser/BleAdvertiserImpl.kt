/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification.ble.advertiser

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import android.util.Log
import com.orange.proximitynotification.ble.BleSettings

class BleAdvertiserImpl(
    override val settings: BleSettings,
    private val bluetoothAdvertiser: BluetoothLeAdvertiser
) : BleAdvertiser {

    companion object {
        private val TAG: String = BleAdvertiserImpl::class.java.simpleName
    }

    private var advertiseCallback: InnerAdvertiseCallback? = null

    override fun start(data: ByteArray, callback: BleAdvertiser.Callback) {
        Log.d(TAG, "Starting Advertising")

        doStop()
        doStart(data, callback)
    }

    override fun stop() {
        Log.d(TAG, "Stopping Advertising")
        doStop()
    }

    private fun doStart(data: ByteArray, callback: BleAdvertiser.Callback) {
        advertiseCallback = InnerAdvertiseCallback(callback).also {
            bluetoothAdvertiser.startAdvertising(
                buildAdvertiseSettings(),
                buildAdvertiseData(data),
                it
            )
        }
    }

    private fun doStop() {
        advertiseCallback?.let {
            bluetoothAdvertiser.stopAdvertising(advertiseCallback)
        }
        advertiseCallback = null
    }

    private fun buildAdvertiseData(data: ByteArray): AdvertiseData {
        return AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(settings.serviceUuid))
            .addServiceData(ParcelUuid(settings.serviceUuid), data)
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()
    }

    private fun buildAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setTimeout(0)
            .build()
    }

    private inner class InnerAdvertiseCallback(val callback: BleAdvertiser.Callback) :
        AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.w(TAG, "Advertising failed errorCode=$errorCode")
            callback.onError(errorCode)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "Advertising successfully started")
        }
    }

}