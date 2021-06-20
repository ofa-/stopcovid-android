/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the TOUS-ANTI-COVID project
 */

package com.orange.proximitynotification.ble.advertiser

import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import com.orange.proximitynotification.ProximityNotificationEventId
import com.orange.proximitynotification.ProximityNotificationLogger
import com.orange.proximitynotification.ble.BleSettings

class BleAdvertiserImpl(
    override val settings: BleSettings,
    private val bluetoothAdvertiser: BluetoothLeAdvertiser
) : BleAdvertiser {

    private var advertiseCallback: InnerAdvertiseCallback? = null

    override fun start(data: ByteArray, callback: BleAdvertiser.Callback): Boolean {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.BLE_ADVERTISER_START,
            "Starting advertising"
        )

        doStop()
        return doStart(data, callback)
    }

    override fun stop() {
        ProximityNotificationLogger.info(
            ProximityNotificationEventId.BLE_ADVERTISER_STOP,
            "Stopping advertising"
        )

        doStop()
    }

    private fun doStart(data: ByteArray, callback: BleAdvertiser.Callback): Boolean {
        advertiseCallback = InnerAdvertiseCallback(callback)

        return runCatching {
            bluetoothAdvertiser.startAdvertising(
                buildAdvertiseSettings(),
                buildAdvertiseData(data),
                advertiseCallback
            )
        }.onFailure {
            ProximityNotificationLogger.error(
                eventId = ProximityNotificationEventId.BLE_ADVERTISER_START_ERROR,
                message = "Failed to start advertising",
                cause = it
            )

            doStop()
        }.isSuccess
    }

    private fun doStop() {
        advertiseCallback?.runCatching {
            bluetoothAdvertiser.stopAdvertising(advertiseCallback)
        }?.onFailure {
            ProximityNotificationLogger.error(
                eventId = ProximityNotificationEventId.BLE_ADVERTISER_STOP_ERROR,
                message = "Failed to stop advertising",
                cause = it
            )
        }?.onSuccess {
            ProximityNotificationLogger.info(
                eventId = ProximityNotificationEventId.BLE_ADVERTISER_STOP_SUCCESS,
                message = "Succeed to stop advertising"
            )
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

            ProximityNotificationLogger.error(
                ProximityNotificationEventId.BLE_ADVERTISER_START_ERROR,
                "Failed to start advertising (errorCode=$errorCode)"
            )

            callback.onError(errorCode)
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)

            ProximityNotificationLogger.info(
                ProximityNotificationEventId.BLE_ADVERTISER_START_SUCCESS,
                "Succeed to start advertising"
            )
        }
    }
}