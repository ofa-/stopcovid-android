/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Authors
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Created by Orange / Date - 2020/04/27 - for the STOP-COVID project
 */

package com.orange.proximitynotification

import android.app.Notification
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import com.orange.proximitynotification.ble.BleProximityNotification
import com.orange.proximitynotification.ble.BleProximityNotificationFactory
import com.orange.proximitynotification.ble.BleSettings
import com.orange.proximitynotification.tools.BluetoothStateBroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

/**
 * ProximityNotification foreground service.
 */
abstract class ProximityNotificationService : Service(),
    ProximityNotificationCallback, ProximityPayloadProvider, CoroutineScope {

    companion object {
        private val TAG = ProximityNotificationService::class.java.simpleName
    }

    private var bleProximityNotification: BleProximityNotification? = null
    private var bluetoothStateBroadcastReceiver: BluetoothStateBroadcastReceiver? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + SupervisorJob()

    abstract val foregroundNotificationId: Int
    abstract val bleSettings: BleSettings

    override fun onDestroy() {
        stop()
        super.onDestroy()

        cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * Starts ProximityNotification and foreground service.
     *
     * It registers a BluetoothStateBroadcastReceiver in order to be notified of state changes
     * @see BluetoothStateBroadcastReceiver
     */
    fun start() {
        registerBluetoothBroadcastReceiver()
        doStart()
    }

    /**
     * Stops ProximityNotification and foreground service.
     *
     * It unregisters a BluetoothStateBroadcastReceiver in order to be notified of state changes
     * @see BluetoothStateBroadcastReceiver
     */
    fun stop() {
        doStop()
        unregisterBluetoothBroadcastReceiver()
    }

    /**
     * Notify ProximityPayload provided by ProximityPayloadProvider has changed.
     *
     * @see ProximityPayloadProvider
     */
    fun notifyProximityPayloadUpdated() {
        bleProximityNotification?.notifyPayloadUpdated()
    }

    /**
     * @return true if ProximityNotification is running, false otherwise
     */
    fun isRunning() = bleProximityNotification?.isRunning == true

    protected open fun doStart() {
        startForeground(foregroundNotificationId, buildForegroundServiceNotification())
        startBleProximityNotification()
    }

    protected open fun doStop() {
        stopBleProximityNotification()
        stopForeground(true)
    }

    private fun registerBluetoothBroadcastReceiver() {
        bluetoothStateBroadcastReceiver = BluetoothStateBroadcastReceiver(
            onBluetoothDisabled = { onBluetoothDisabled() },
            onBluetoothEnabled = { onBluetoothEnabled() })
            .also {
                applicationContext.registerReceiver(
                    it,
                    IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
                )
            }
    }

    private fun unregisterBluetoothBroadcastReceiver() {
        bluetoothStateBroadcastReceiver?.let {
            applicationContext.unregisterReceiver(it)
        }
        bluetoothStateBroadcastReceiver = null
    }

    private fun startBleProximityNotification() {
        if (bleProximityNotification != null) {
            stopBleProximityNotification()
        }

        BleProximityNotificationFactory.build(this, bleSettings, this)?.run {
            bleProximityNotification = this
            setUp(
                this@ProximityNotificationService,
                this@ProximityNotificationService
            )
        } ?: run {
            onError(
                ProximityNotificationError(
                    ProximityNotificationError.Type.BLE_PROXIMITY_NOTIFICATION,
                    cause = "Failed to initialize bluetooth"
                )
            )
        }

        bleProximityNotification?.start()
    }

    private fun stopBleProximityNotification() {
        bleProximityNotification?.stop()
        bleProximityNotification = null
    }

    /**
     * Called once Bluetooth is disabled.
     * By default it stops the ProximityNotification and the foreground service
     *
     * @see ProximityNotification#stop
     */
    protected open fun onBluetoothDisabled() {
        Log.d(TAG, "Bluetooth disabled")

        doStop()
    }

    /**
     * Called once Bluetooth is enabled.
     * By default it starts the ProximityNotification and the foreground service
     *
     * @see ProximityNotification#start
     */
    protected open fun onBluetoothEnabled() {
        Log.d(TAG, "Bluetooth enabled")

        doStart()
    }

    /**
     * Create the Notification to display once this foreground service is running
     *
     * @return Notification used to display foreground service
     */
    abstract fun buildForegroundServiceNotification(): Notification

}


